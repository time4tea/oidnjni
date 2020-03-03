package net.time4tea.oidn

import java.awt.Graphics
import java.awt.image.BufferedImage
import java.nio.FloatBuffer

inline fun <T : Graphics, R> T.disposing(block: (T) -> R): R {
    try {
        return block(this)
    } finally {
        this.dispose()
    }
}

fun BufferedImage.copyTo(dest: FloatBuffer) {
    val dataBuffer = this.raster.dataBuffer
    for (i in 0 until dataBuffer.size) {
        dest.put(i, dataBuffer.getElemFloat(i))
    }
}

fun BufferedImage.copyTo(dest: BufferedImage) {
    dest.graphics.disposing {
        it.drawImage(this, 0, 0, null)
    }
}

fun FloatBuffer.copyTo(dest: BufferedImage) {
    for (i in 0 until capacity()) {
        dest.raster.dataBuffer.setElemFloat(i, get(i))
    }
}