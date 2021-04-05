package net.time4tea.oidn

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.lessThan
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun BufferedImage.variance(): Double {
    return OidnTest.ImageVariance(this).imageVariance()
}

class OidnTest {

    class ImageVariance(val image: BufferedImage) {

        fun imageVariance(): Double {
            return (reds().variance() + blues().variance() + greens().variance()) / 3
        }

        private fun reds(): Sequence<Float> {
            return pixels().map { it.red / 255f }
        }

        private fun greens(): Sequence<Float> {
            return pixels().map { it.green / 255f }
        }

        private fun blues(): Sequence<Float> {
            return pixels().map { it.blue / 255f }
        }

        private fun pixels(): Sequence<Color> {
            return Iterable {
                object : AbstractIterator<Color>() {
                    var count = 0
                    override fun computeNext() {
                        if (count < image.width * image.height) {
                            setNext(Color(image.getRGB(count % image.width, count / image.width)))
                        } else {
                            done()
                        }
                        count++
                    }
                }
            }.asSequence()
        }
    }

    @Volatile //just a hack so we don't get "always false" warnings
    private var displaying = ! System.getProperty("java.awt.headless", "false").toBoolean()

    @Test
    fun `version from encoded`() {
        assertThat(OidnDevice.Version(10_23_45), equalTo(OidnDevice.Version(10, 23, 45)))
        assertThat(OidnDevice.Version(1_02_09), equalTo(OidnDevice.Version(1, 2, 9)))
    }

    @Test
    fun `version formatting`() {
        assertThat(OidnDevice.Version(1, 2, 3).string(), equalTo("1.02.003"))
    }

    @Test
    fun `library version`() {
        val oidn = Oidn()
        val version = oidn.newDevice(Oidn.DeviceType.DEVICE_TYPE_DEFAULT).use { device -> device.version() }
        assertThat(version, equalTo(OidnDevice.Version(1, 2, 4)))
    }

    @Test
    fun `denoising an image`() {
        val oidn = Oidn()

        // Load an image this will have "normal" pixel layout
        val imageName = "example-cornellbox-rendered.png"
        val imageInIntPixelLayout = javaClass.getResourceAsStream("""/$imageName""").use {
            ImageIO.read(it)
        }

        // Allocate a BufferedImage with the same pixel layout that OIDN needs
        val imageInCorrectPixelLayout = OidnImages.newBufferedImageFrom(imageInIntPixelLayout)

        if (displaying) SwingFrame(imageInCorrectPixelLayout)

        // Allocate input and output buffers, with input buffer having input image
        val color = Oidn.allocateBufferFor(imageInIntPixelLayout).also { imageInCorrectPixelLayout.copyTo(it) }
        val output = Oidn.allocateBufferFor(imageInIntPixelLayout)

        val beforeVariance = imageInCorrectPixelLayout.variance()

        // set up OIDN and run denoise filter
        oidn.newDevice(Oidn.DeviceType.DEVICE_TYPE_DEFAULT).use { device ->
            device.raytraceFilter().use { filter ->
                filter.setFilterImage(
                    color, output, imageInIntPixelLayout.width, imageInIntPixelLayout.height
                )
                filter.commit()
                filter.execute()
                assertThat("should have no errors", device.error().ok(), equalTo(true))
            }
        }

        // Copy output pixel data to displayable image
        output.copyTo(imageInCorrectPixelLayout)

        val afterVariance = imageInCorrectPixelLayout.variance()

        assertThat("before image should have content", beforeVariance, !equalTo(0.0))
        assertThat("after image should have content", afterVariance, !equalTo(0.0))

        assertThat("after image should be less noisy", afterVariance, lessThan(beforeVariance))

        // Copy image to original pixel layout, so it can be written
        imageInCorrectPixelLayout.copyTo(imageInIntPixelLayout)

        // Write the output image to a file
        if (!ImageIO.write(
                imageInIntPixelLayout,
                "png",
                File("example-output/${imageName}").also { it.parentFile.mkdirs() })
        ) {
            throw IllegalArgumentException("unable to write file")
        }

        if (displaying) Thread.sleep(500)
    }
}

fun Sequence<Number>.variance(): Double {
    val rs = RunningStat()
    this.forEach { rs.push(it.toDouble()) }
    return rs.variance()
}