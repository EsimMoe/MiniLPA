package moe.sekiu.minilpa.ui.component

import java.awt.Component
import moe.sekiu.minilpa.ui.MiniPanel
import net.miginfocom.swing.MigLayout

class MiniGroup(vararg components : Component, constraints : String = "") : MiniPanel()
{
    init
    {
        isOpaque = false
        layout = MigLayout("insets 0, $constraints")
        components.forEach {
            if (it is ConstraintsBox) add(it.component, it.constraints)
            else add(it)
        }
    }

    data class ConstraintsBox(val component : Component, val constraints : String) : Component()

    companion object
    {
        fun Component.with(constraints : String) = ConstraintsBox(this, constraints)
    }
}