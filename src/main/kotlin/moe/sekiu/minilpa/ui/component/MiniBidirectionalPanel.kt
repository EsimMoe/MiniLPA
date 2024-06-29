package moe.sekiu.minilpa.ui.component

import javax.swing.JComponent
import moe.sekiu.minilpa.ui.MiniPanel
import net.miginfocom.swing.MigLayout

data class MiniBidirectionalPanel(val left : JComponent, val right : JComponent) : MiniPanel()
{
    init
    {
        isOpaque = false
        layout = MigLayout("insets 0")
        add(left, "pushX")
        add(right)
    }
}