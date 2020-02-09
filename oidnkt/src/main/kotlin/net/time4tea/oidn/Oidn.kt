package net.time4tea.oidn

import java.io.File
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.time.Duration

class Oidn {

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

    data class Library(val name: String, val version:Int? = null) {
        fun filename(): String {
            return if ( version == null ) {
                "lib$name.so"
            } else {
                "lib$name.so.$version"
            }
        }
    }

    companion object {
        private val libraries = listOf(
            Library("tbb", 2),
            Library("tbbmalloc", 2),
            Library("OpenImageDenoise"),
            Library("oidnjni")
        )

        init {
            loadLibrary()
        }

        fun loadLibrary() {
            val useFilesystem = System.getProperty("oidnjni.filesystem")?.toBoolean() ?: false
            if (!useFilesystem) {
                val directory = Files.createTempDirectory("oidn").toFile().also { it.deleteOnExit() }
                libraries.forEach {
                    val lib = copyLibrary(it, directory)
                    try {
                        System.load(lib.absolutePath)
                    } catch (e: UnsatisfiedLinkError) {
                        throw OidnLibraryNotFoundError("Unable to load $it", e)
                    }
                }
            }
        }

        private fun copyLibrary(resource: Library, directory: File): File {
            val filename = resource.filename()
            val destination = File(directory, filename)
            destination.outputStream().use { dest ->
                val stream = Oidn::class.java.getResourceAsStream("/$filename")?:throw FileNotFoundException(filename)
                stream.use { source ->
                    source.copyTo(dest)
                }
            }
            return destination
       }

        fun allocateBuffer(width: Int, height: Int): ByteBuffer {
            val capacity = width * height * 3 * 4
            val buffer = ByteBuffer.allocateDirect(capacity)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            return buffer
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
        buffer: ByteBuffer,
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
        timed("${javaClass.name}:execute") { jniExecute(ptr) }
    }

    fun setFilterImage(colour: ByteBuffer, output: ByteBuffer, width: Int, height: Int) {
        jniSetSharedFilterImage(ptr, "color", ensureDirect(colour), width, height)
        jniSetSharedFilterImage(ptr, "output", ensureDirect(output), width, height)
    }

    private fun ensureDirect(b: ByteBuffer): ByteBuffer {
        if (!b.isDirect) {
            throw IllegalArgumentException("Must be direct")
        }
        return b
    }
}

inline fun <T> timed(name: String, op: () -> T): T {
    val start = System.nanoTime()
    try {
        return op()
    } finally {
        val elapsed = Duration.ofNanos(System.nanoTime() - start)
        println("$name took $elapsed")
    }
}

class OidnDevice(private val ptr: Long) : AutoCloseable {
    private external fun jniCommit(ptr: Long)
    private external fun jniRelease(ptr: Long)

    private external fun jniNewFilter(ptr: Long, type: String): Long
    private external fun jniGetError(ptr: Long)

    fun commit() {
        jniCommit(ptr)
    }

    fun raytraceFilter(): OidnFilter {
        return OidnFilter(jniNewFilter(ptr, "RT"))
    }

    fun error() {
        jniGetError(ptr)
    }

    override fun close() {
        jniRelease(ptr)
    }
}