package net.time4tea.oidn

import java.awt.image.BufferedImage
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.Timer

class SwingFrame(image: BufferedImage) : JFrame() {

    private val icon = JLabel(ImageIcon(image))
    private val timer = Timer(50) { icon.repaint() }

    init {
        title = "Image"
        defaultCloseOperation = EXIT_ON_CLOSE
        contentPane.add(icon)

        timer.start()

        pack()
        isVisible = true
    }
}