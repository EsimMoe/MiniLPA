package moe.sekiu.minilpa.ui.component

import com.formdev.flatlaf.FlatClientProperties
import com.formdev.flatlaf.extras.FlatSVGIcon
import com.formdev.flatlaf.ui.FlatMenuItemUI
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.SwingUtilities
import javax.swing.plaf.MenuItemUI
import javax.swing.plaf.basic.BasicMenuItemUI
import moe.sekiu.minilpa.action
import moe.sekiu.minilpa.autoHighlight
import moe.sekiu.minilpa.language
import moe.sekiu.minilpa.mask
import moe.sekiu.minilpa.model.Notification
import moe.sekiu.minilpa.noFocus
import moe.sekiu.minilpa.ui.Actions
import moe.sekiu.minilpa.lpa.LocalProfileAssistant
import moe.sekiu.minilpa.ui.NotificationPanel
import net.miginfocom.swing.MigLayout


class NotificationCard(val notification : Notification) : JButton()
{
    val nicknameLabel : MiniEmojiLabel
    val iccidLabel = JLabel()
    val selectionCheckbox = TransparentCheckBox().noFocus()

    class TransparentCheckBox : JCheckBox()
    {
        init { isVisible = false }

        override fun paintComponent(graphics : Graphics)
        {
            val graphics2D = graphics.create() as Graphics2D
            val alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, if (NotificationList.instance.selectionMode) 1F else 0.5F)
            graphics2D.composite = alpha
            super.paintComponent(graphics2D)
            graphics2D.dispose()
        }
    }

    init
    {
        preferredSize = Dimension(620, 110)
        layout = MigLayout(
            "fillY",
            "[]20[]"
        )
        putClientProperty(
            FlatClientProperties.STYLE,
            """
                [light]background: tint(@background,50%);
                [dark]background: shade(@background,15%);
            """)
        val profile = ProfileList.instance.cards.find { it.profile.iccid == notification.iccid }?.profile
        nicknameLabel = MiniEmojiLabel(if (profile != null) profile.nickname ?: language.`empty-tag` else language.`deleted-tag`)
        nicknameLabel.putClientProperty(FlatClientProperties.STYLE_CLASS, "h3")
        switchIccidMask(LocalProfileAssistant.showDetails.isSelected)
        addMouseListener(object : MouseAdapter()
        {
            override fun mouseEntered(event : MouseEvent)
            {
                selectionCheckbox.isVisible = true
            }

            override fun mouseExited(evnet : MouseEvent)
            {
                val component = SwingUtilities.getDeepestComponentAt(evnet.component, evnet.x, evnet.y)
                if (!NotificationList.instance.selectionMode)
                {
                    if (component == null || !SwingUtilities.isDescendingFrom(component, this@NotificationCard)) selectionCheckbox.isVisible = false
                }
            }
        })
        selectionCheckbox.addActionListener {
            NotificationList.instance.lastSelected = this
            val selectedNotifications = NotificationList.instance.selectedNotifications
            NotificationList.instance.selectionMode = selectedNotifications.isNotEmpty()
            if (selectedNotifications.isEmpty()) NotificationPanel.instance.bottomInfo.selectedInfo.text = " "
            else NotificationPanel.instance.bottomInfo.selectedInfo.text = language.selected.format(selectedNotifications.count())
        }
        addActionListener { event ->
            if (NotificationList.instance.selectionMode)
            {
                if ((event.modifiers and ActionEvent.SHIFT_MASK) != 0)
                {
                    NotificationList.instance.lastSelected?.also { last ->
                        val components = NotificationList.instance.cards
                        val lastIndex = components.indexOf(last)
                        val thisIndex = components.indexOf(this)
                        if (lastIndex != -1 && thisIndex != -1)
                        {
                            for (component in components) component.selectionCheckbox.isSelected = false
                            for (i in if (lastIndex < thisIndex) lastIndex..thisIndex else lastIndex downTo thisIndex)
                            {
                                components[i].selectionCheckbox.isSelected = true
                            }
                        }
                    }
                } else
                {
                    selectionCheckbox.doClick()
                    selectionCheckbox.isVisible = true
                }
                val selectedNotifications = NotificationList.instance.selectedNotifications
                if (selectedNotifications.isEmpty()) NotificationPanel.instance.bottomInfo.selectedInfo.text = " "
                else NotificationPanel.instance.bottomInfo.selectedInfo.text = language.selected.format(selectedNotifications.count())
            }
        }
        add("cell 0 0 1 3", JLabel().icon())
        add("cell 1 0, pushX, wmax 450", nicknameLabel)
        add("cell 2 0", JLabel("#${notification.seq}"))
        add("cell 1 1", JLabel(notification.address))
        add("cell 2 1", selectionCheckbox)
        add("cell 1 2", iccidLabel)

        val referenceSize = 16
        val menu = JPopupMenu()
        menu.add(
            JMenuItem(language.process, FlatSVGIcon("icons/notification-process.svg", referenceSize, referenceSize))
            .autoHighlight()
            .action {
                if (NotificationList.instance.selectionMode)
                {
                    val seqs = NotificationList.instance.selectedNotifications.map { it.notification.seq }.toIntArray()
                    if (notification.seq in seqs) LocalProfileAssistant.processNotification(*seqs)
                    else LocalProfileAssistant.processNotification(notification.seq)
                }
                else LocalProfileAssistant.processNotification(notification.seq)
            })
        menu.add(
            JMenuItem(language.`to-profile`, FlatSVGIcon("icons/profile.svg", referenceSize, referenceSize))
            .autoHighlight()
            .action { Actions.Notification.toProfile(notification.iccid ?: return@action) })
        menu.add(
            object : JMenuItem(language.remove, FlatSVGIcon("icons/notification-remove.svg", referenceSize, referenceSize))
            {
                override fun setUI(ui : MenuItemUI?)
                {
                    super.setUI(when (ui)
                    {
                        is FlatMenuItemUI -> object : FlatMenuItemUI() { init { selectionBackground = Color.RED } }
                        is BasicMenuItemUI -> object : BasicMenuItemUI() { init { selectionBackground = Color.RED } }
                        else -> ui
                    })
                }
            }.autoHighlight().action {
                if (NotificationList.instance.selectionMode)
                {
                    val seqs = NotificationList.instance.selectedNotifications.map { it.notification.seq }.toIntArray()
                    if (notification.seq in seqs) Actions.Notification.remove(*seqs)
                    else Actions.Notification.remove(notification.seq)
                }
                else Actions.Notification.remove(notification.seq)
            })
        componentPopupMenu = menu
    }

    private fun JLabel.icon() : JLabel
    {
        val (iconPath, toolTip) = when (notification.operation)
        {
            moe.sekiu.minilpa.model.Notification.Operation.INSTALL -> "icons/profile-install.svg" to language.install
            moe.sekiu.minilpa.model.Notification.Operation.ENABLE -> "icons/profile-enable.svg" to language.enable
            moe.sekiu.minilpa.model.Notification.Operation.DISABLE -> "icons/profile-disable.svg" to language.disable
            moe.sekiu.minilpa.model.Notification.Operation.DELETE -> "icons/profile-delete.svg" to language.delete
        }
        icon = FlatSVGIcon(iconPath, 64, 64)
        toolTipText = toolTip
        return this
    }

    fun switchIccidMask(show : Boolean) { iccidLabel.text = if (show) notification.iccid else notification.iccid?.mask(8) }
}