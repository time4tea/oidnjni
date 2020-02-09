package net.time4tea.oidn

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.lessThan
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.Transparency
import java.awt.color.ColorSpace
import java.awt.image.*
import java.io.File
import java.nio.FloatBuffer
import javax.imageio.ImageIO


class OidnTest {

    class FloatBufferedImage(width: Int, height: Int) {
        private val bands = 3
        private val buffer: DataBuffer
        val image: BufferedImage

        companion object {
            fun from(image: BufferedImage): FloatBufferedImage {
                return FloatBufferedImage(image.width, image.height).also {
                    it.copyFrom(image)
                }
            }
        }

        init {
            val bandOffsets = intArrayOf(0, 1, 2) // length == bands, 0 == R, 1 == G, 2 == B

            val sampleModel: SampleModel =
                PixelInterleavedSampleModel(DataBuffer.TYPE_FLOAT, width, height, bands, width * bands, bandOffsets)
            buffer = DataBufferFloat(width * height * bands)

            val raster = Raster.createWritableRaster(sampleModel, buffer, null)

            val colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB)
            val colorModel: ColorModel =
                ComponentColorModel(colorSpace, false, false, Transparency.OPAQUE, DataBuffer.TYPE_FLOAT)

            image = BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied, null)
        }

        fun reds(): Sequence<Float> {
            return pixels().map { it.red / 255f }
        }

        fun greens(): Sequence<Float> {
            return pixels().map { it.green / 255f }
        }

        fun blues(): Sequence<Float> {
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

        fun copyFrom(src: BufferedImage) {
            this.image.graphics.disposing {
                it.drawImage(src, 0, 0, null)
            }
        }

        fun copyTo(dst: BufferedImage) {
            dst.graphics.disposing {
                it.drawImage(this.image, 0, 0, null)
            }
        }

        fun toBuffer(dst: FloatBuffer) {
            for (i in 0 until buffer.size) {
                dst.put(i, buffer.getElemFloat(i))
            }
        }

        fun fromBuffer(src: FloatBuffer) {
            for (i in 0 until src.capacity()) {
                buffer.setElemFloat(i, src.get(i))
            }
        }
    }

    //    @Volatile //just a hack so we don't get "always false" warnings
    private var displaying = true

    @Test
    fun something() {
        println(System.getProperty("java.library.path"))
        println(System.getenv("LD_LIBRARY_PATH"))

        val oidn = Oidn()

        val imageName = "weekfinal.png"
        val image = javaClass.getResourceAsStream("""/$imageName""").use {
            ImageIO.read(it)
        }

        val color = Oidn.allocateBuffer(image.width, image.height)

        val intermediate = FloatBufferedImage.from(image)

        intermediate.toBuffer(color.asFloatBuffer())

        val output = Oidn.allocateBuffer(image.width, image.height)

        if (displaying) SwingFrame(intermediate.image)

        val beforeVariance = imageVariance(intermediate)

        oidn.newDevice(Oidn.DeviceType.DEVICE_TYPE_DEFAULT).use { device ->
            device.raytraceFilter().use { filter ->
                filter.setFilterImage(
                    color, output, image.width, image.height
                )
                filter.commit()
                filter.execute()
                device.error()
            }
        }

        intermediate.fromBuffer(output.asFloatBuffer())

        val afterVariance = imageVariance(intermediate)

        assertThat("before image should have content", beforeVariance, !equalTo(0.0))
        assertThat("after image should have content", afterVariance, !equalTo(0.0))

        assertThat("after image should be less noisy", afterVariance, lessThan(beforeVariance))

        intermediate.copyTo(image)

        if (!ImageIO.write(image, "png", File("example-output/${imageName}").also { it.parentFile.mkdirs() })) {
            throw IllegalArgumentException("unable to write file")
        }

        if (displaying) Thread.sleep(500)
    }

    private fun imageVariance(image: FloatBufferedImage): Double {
        return (image.reds().variance() + image.blues().variance() + image.greens().variance()) / 3
    }
}

fun Sequence<Number>.variance(): Double {
    val rs = RunningStat()
    this.forEach { rs.push(it.toDouble()) }
    return rs.variance()
}