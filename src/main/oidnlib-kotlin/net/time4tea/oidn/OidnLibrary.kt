package net.time4tea.oidn

import java.lang.IllegalArgumentException

interface Library {
    fun os(): OidnLibrary.OS
    fun filename(): String
}

class LinuxLibrary(private val name: String, private val version: String? = null) : Library {

    override fun os() = OidnLibrary.OS.Linux

    override fun filename(): String {
        return if (version == null) {
            "lib$name.so"
        } else {
            "lib$name.so.$version"
        }
    }
}

class DarwinLibrary(private val name: String, private val version: String? = null) : Library {

    override fun os() = OidnLibrary.OS.Mac

    override fun filename(): String {
        return if (version == null) {
            "lib$name.dylib"
        } else {
            "lib$name.$version.dylib"
        }
    }
}

class OidnLibrary {

    enum class OS {
        Linux, Mac
    }

    companion object {

        private fun os(): OS {
            val p = System.getProperty("os.name").toLowerCase();
            return when {
                p.contains("linux") -> OS.Linux
                p.contains("mac") -> OS.Mac
                else -> throw IllegalArgumentException("Unsupported OS: $p")
            }
        }

        fun libraries(): List<Library> {
            return when (os()) {
                OS.Linux -> listOf(
                    LinuxLibrary("tbb", "2"),
                    LinuxLibrary("tbbmalloc", "2"),
                    LinuxLibrary("OpenImageDenoise", "1.2.4"),
                    LinuxLibrary("oidnjni")
                )
                OS.Mac -> listOf(
                    DarwinLibrary("tbb"),
                    DarwinLibrary("tbbmalloc"),
                    DarwinLibrary("OpenImageDenoise", "1.2.4"),
                    DarwinLibrary("oidnjni")
                )
            }
        }
    }
}