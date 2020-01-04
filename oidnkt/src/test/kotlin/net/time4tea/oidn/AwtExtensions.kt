package net.time4tea.oidn

import java.awt.Graphics

inline fun <T : Graphics, R> T.disposing(block: (T) -> R): R {
    try {
        return block(this)
    } finally {
        this.dispose()
    }
}

