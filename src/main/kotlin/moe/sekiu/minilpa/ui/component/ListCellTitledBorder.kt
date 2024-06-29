package moe.sekiu.minilpa.ui.component

import com.formdev.flatlaf.ui.FlatUIUtils
import com.formdev.flatlaf.util.UIScale
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Insets
import java.awt.geom.Rectangle2D
import javax.swing.JList
import javax.swing.UIManager
import javax.swing.border.Border

/**
 * @author Karl Tauber
 */
internal class ListCellTitledBorder(private val list : JList<*>, private val title : String) : Border
{
    override fun isBorderOpaque() = true

    override fun getBorderInsets(c : Component) : Insets
    {
        val height = c.getFontMetrics(list.font).height
        return Insets(height, 0, 0, 0)
    }

    override fun paintBorder(c : Component, g : Graphics, x : Int, y : Int, width : Int, height : Int)
    {
        val fm = c.getFontMetrics(list.font)
        val titleWidth = fm.stringWidth(title)
        val titleHeight = fm.height

        // fill background
        g.color = list.background
        g.fillRect(x, y, width, titleHeight)

        val gap = UIScale.scale(4)

        val g2 = g.create() as Graphics2D
        try
        {
            FlatUIUtils.setRenderingHints(g2)

            g2.color = UIManager.getColor("Label.disabledForeground")

            if (g.color == g2.color) g2.color = UIManager.getColor("Label.background")

            // paint separator lines
            val sepWidth = (width - titleWidth) / 2 - gap - gap
            if (sepWidth > 0)
            {
                val sy = y + Math.round(titleHeight / 2f)
                val sepHeight = UIScale.scale(1f)

                g2.fill(Rectangle2D.Float((x + gap).toFloat(), sy.toFloat(), sepWidth.toFloat(), sepHeight))
                g2.fill(
                    Rectangle2D.Float(
                        (x + width - gap - sepWidth).toFloat(),
                        sy.toFloat(),
                        sepWidth.toFloat(),
                        sepHeight
                    )
                )
            }

            // draw title
            val xt = x + ((width - titleWidth) / 2)
            val yt = y + fm.ascent
            FlatUIUtils.drawString(list, g2, title, xt, yt)
        } finally { g2.dispose() }
    }
}