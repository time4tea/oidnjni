package net.time4tea.oidn

data class Library(val name: String, val version: String? = null) {
    fun filename(): String {
        return if (version == null) {
            "lib$name.dylib"
        } else {
            "lib$name.$version.dylib"
        }
    }
}

class OidnLibrary {
    companion object {
        val libraries = listOf(
            Library("tbb"),
            Library("tbbmalloc"),
            Library("OpenImageDenoise", "1.2.4"),
            Library("oidnjni")
        )
    }
}