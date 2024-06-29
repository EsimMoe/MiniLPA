package moe.sekiu.minilpa.ui

import java.awt.BorderLayout
import javax.swing.JScrollPane
import moe.sekiu.minilpa.setup
import moe.sekiu.minilpa.ui.component.BottomInfo
import moe.sekiu.minilpa.ui.component.NotificationList
import moe.sekiu.minilpa.ui.component.NotificationToolBar

class NotificationPanel : MiniPanel()
{
    companion object
    {
        lateinit var instance : NotificationPanel
    }

    val bottomInfo = BottomInfo()

    init
    {
        instance = this
        layout = BorderLayout()
        add(NotificationToolBar(), BorderLayout.NORTH)
        add(JScrollPane(NotificationList()).setup(), BorderLayout.CENTER)
        add(bottomInfo, BorderLayout.SOUTH)
    }
}