package net.time4tea.oidn

import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files

interface LibraryLocator {
    fun locate(library: Library): File
}

class PackagedLibraryLocator(name: String) : LibraryLocator {

    private val directory = Files.createTempDirectory(name).toFile().also { it.deleteOnExit() }

    override fun locate(library: Library): File {
        val filename = library.filename()
        val destination = File(directory, filename).also { it.deleteOnExit() }
        destination.outputStream().use { dest ->
            val resourceFilename = "/lib/$filename"
            val stream =
                Oidn::class.java.getResourceAsStream(resourceFilename) ?: throw FileNotFoundException(resourceFilename)
            stream.use { source ->
                source.copyTo(dest)
            }
        }
        return destination
    }
}
