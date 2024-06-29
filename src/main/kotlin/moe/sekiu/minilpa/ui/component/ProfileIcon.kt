package moe.sekiu.minilpa.ui.component

import com.formdev.flatlaf.extras.FlatSVGIcon
import java.awt.AlphaComposite
import java.awt.Dimension
import java.awt.Graphics
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.swing.JPanel
import kotlin.io.encoding.Base64


class ProfileIcon(base64 : String?) : JPanel()
{
    val image : BufferedImage? = base64?.let { makeRoundedCorner(ImageIO.read(ByteArrayInputStream(Base64.decode(base64))), 20) }
    val defaultSVGIcon = FlatSVGIcon("icons/profile.svg", 64, 64)

    init
    {
        preferredSize = Dimension(64, 64)
        minimumSize = preferredSize
        isOpaque = false
        defaultSVGIcon.image
    }

    private fun makeRoundedCorner(image : BufferedImage, cornerRadius : Int) : BufferedImage
    {
        val w = image.width
        val h = image.height
        val output = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)

        val g2 = output.createGraphics()


        // This is what we want, but it only does hard-clipping, i.e. aliasing
        // g2.setClip(new RoundRectangle2D ...)

        // so instead fake soft-clipping by first drawing the desired clip shape
        // in fully opaque white with antialiasing enabled...
        g2.composite = AlphaComposite.Src
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.color = background
        g2.fill(
            RoundRectangle2D.Float(
                0f,
                0f,
                w.toFloat(),
                h.toFloat(),
                cornerRadius.toFloat(),
                cornerRadius.toFloat()
            )
        )


        // ... then compositing the image on top,
        // using the white shape from above as alpha source
        g2.composite = AlphaComposite.SrcAtop
        g2.drawImage(image, 0, 0, null)

        g2.dispose()

        return output
    }

    override fun paintComponent(g : Graphics)
    {
        super.paintComponent(g)
        g.drawImage(image ?: defaultSVGIcon.image, 0, 0, this)
    }
}