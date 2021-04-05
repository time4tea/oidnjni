package net.time4tea.oidn

data class Library(val name: String, val version: String? = null) {
    fun filename(): String {
        return if (version == null) {
            "lib$name.so"
        } else {
            "lib$name.so.$version"
        }
    }
}

class OidnLibrary {
    companion object {
        val libraries = listOf(
            Library("tbb", "2"),
            Library("tbbmalloc", "2"),
            Library("OpenImageDenoise", "1.2.4"),
            Library("oidnjni")
        )
    }
}