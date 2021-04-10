package net.time4tea.oidn

import java.awt.Transparency
import java.awt.color.ColorSpace
import java.awt.image.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicBoolean

class Oidn private constructor() {

    enum class DeviceType(val primitive: Int) {
        DEVICE_TYPE_DEFAULT(0), DEVICE_TYPE_CPU(1)
    }

    enum class OidnFormat(val primitive: Int) {
        FORMAT_UNDEFINED(0), FORMAT_FLOAT(1), FORMAT_FLOAT2(2), FORMAT_FLOAT3(3), FORMAT_FLOAT4(4)
    }

    enum class OidnAccess(val primitive: Int) {
        ACCESS_READ(0), ACCESS_WRITE(1), ACCESS_READ_WRITE(2), ACCESS_WRITE_DISCARD(3)
    }

    private external fun jniNewDevice(type: Int): Long

    fun newDevice(type: DeviceType): OidnDevice {
        return OidnDevice(jniNewDevice(type.primitive)).also { it.commit() }
    }

    class OidnLibraryNotFoundError(s: String?, cause: Throwable?) : LinkageError(s, cause)

    companion object {

        private val initialized = AtomicBoolean()

        /**
         * initialise the library
         *
         * locator only used for testing the library, call as init()
         */
        fun init(locator: LibraryLocator = PackagedLibraryLocator("oidn")): Oidn {
            if ( initialized.compareAndSet(false, true) ) {
                loadLibraries(locator, OidnLibrary.libraries())
            }
            return Oidn()
        }

        private fun loadLibraries(locator: LibraryLocator, libraries: List<Library>) {
            libraries.forEach {
                try {
                    System.load(locator.locate(it).absolutePath)
                } catch (e: UnsatisfiedLinkError) {
                    throw OidnLibraryNotFoundError("Unable to load $it", e)
                }
            }
        }

        fun allocateBuffer(width: Int, height: Int): FloatBuffer {
            val capacity = width * height * 3 * 4
            val buffer = ByteBuffer.allocateDirect(capacity)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            return buffer.asFloatBuffer()
        }

        fun allocateBufferFor(image: BufferedImage): FloatBuffer {
            return allocateBuffer(image.width, image.height)
        }
    }
}

class OidnImages {
    companion object {

        fun newBufferedImage(width: Int, height: Int): BufferedImage {
            val bands = 3
            val bandOffsets = intArrayOf(0, 1, 2) // length == bands, 0 == R, 1 == G, 2 == B

            val sampleModel: SampleModel =
                PixelInterleavedSampleModel(DataBuffer.TYPE_FLOAT, width, height, bands, width * bands, bandOffsets)
            val buffer = DataBufferFloat(width * height * bands)

            val raster = Raster.createWritableRaster(sampleModel, buffer, null)

            val colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB)
            val colorModel: ColorModel =
                ComponentColorModel(colorSpace, false, false, Transparency.OPAQUE, DataBuffer.TYPE_FLOAT)

            return BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied, null)
        }

        fun newBufferedImageFrom(src: BufferedImage): BufferedImage {
            return newBufferedImage(src.width, src.height).also { dest ->
                src.copyTo(dest)
            }
        }
    }
}

class OidnFilter(private val ptr: Long) : AutoCloseable {

    private external fun jniRelease(ptr: Long)
    private external fun jniCommit(ptr: Long)
    private external fun jniExecute(ptr: Long)
    private external fun jniSetSharedFilterImage(
        ptr: Long,
        name: String,
        buffer: FloatBuffer,
        width: Int,
        height: Int
    )

    override fun close() {
        jniRelease(ptr)
    }

    fun commit() {
        jniCommit(ptr)
    }

    fun execute() {
        jniExecute(ptr)
    }

    fun setFilterImage(colour: FloatBuffer, output: FloatBuffer, width: Int, height: Int) {
        jniSetSharedFilterImage(ptr, "color", ensureDirect(colour), width, height)
        jniSetSharedFilterImage(ptr, "output", ensureDirect(output), width, height)
    }

    fun setAdditionalImages(albedo: FloatBuffer, normal: FloatBuffer?, width: Int, height: Int) {
        jniSetSharedFilterImage(ptr, "albedo", ensureDirect(albedo), width, height)
        if (normal != null) {
            jniSetSharedFilterImage(ptr, "normal", ensureDirect(normal), width, height)
        }
    }

    private fun ensureDirect(buffer: FloatBuffer): FloatBuffer {
        if (!buffer.isDirect) {
            throw IllegalArgumentException("Must be direct")
        }
        return buffer
    }
}

enum class OidnError(val code: Int, val isError: Boolean, val explanation: String) {
    OIDN_ERROR_NONE(0, false, "no error occurred"),
    OIDN_ERROR_UNKNOWN(1, true, "an unknown error occurred"),
    OIDN_ERROR_INVALID_ARGUMENT(2, true, "an invalid argument was specified"),
    OIDN_ERROR_INVALID_OPERATION(3, true, "the operation is not allowed"),
    OIDN_ERROR_OUT_OF_MEMORY(4, true, "not enough memory to execute the operation"),
    OIDN_ERROR_UNSUPPORTED_HARDWARE(5, true, "the hardware (e.g. CPU) is not supported"),
    OIDN_ERROR_CANCELLED(6, true, "the operation was cancelled by the user")
}

class OidnDeviceError(code: Int, cmsg: String?) {
    val message = cmsg ?: "no error message"
    val error: OidnError = OidnError.values().find { it.code == code } ?: OidnError.OIDN_ERROR_UNKNOWN

    fun ok(): Boolean = !error.isError
}

class OidnDevice(private val ptr: Long) : AutoCloseable {
    private external fun jniCommit(ptr: Long)
    private external fun jniRelease(ptr: Long)

    private external fun jniVersion(ptr: Long): Int

    private external fun jniNewFilter(ptr: Long, type: String): Long
    private external fun jniGetError(ptr: Long): OidnDeviceError

    data class Version(val major: Int, val minor: Int, val patch: Int) {

        constructor(encoded: Int) : this(encoded / 10_000, (encoded % 10000) / 100, encoded % 100)

        fun string(): String {
            return "%d.%02d.%03d".format(major, minor, patch)
        }
    }

    fun commit() {
        jniCommit(ptr)
    }

    fun raytraceFilter(): OidnFilter {
        return OidnFilter(jniNewFilter(ptr, "RT"))
    }

    fun error(): OidnDeviceError {
        return jniGetError(ptr)
    }

    override fun close() {
        jniRelease(ptr)
    }

    fun version(): Version {
        return Version(jniVersion(ptr))
    }
}