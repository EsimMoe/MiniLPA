package moe.sekiu.minilpa.ui.component

import com.formdev.flatlaf.extras.FlatSVGIcon
import java.awt.Component
import java.awt.Dimension
import java.awt.Insets
import java.awt.KeyboardFocusManager
import javax.swing.Box
import javax.swing.JButton
import javax.swing.JToggleButton
import moe.sekiu.minilpa.action
import moe.sekiu.minilpa.language
import moe.sekiu.minilpa.ui.Actions
import moe.sekiu.minilpa.lpa.LocalProfileAssistant
import moe.sekiu.minilpa.ui.NotificationPanel

class NotificationToolBar : MiniToolBar()
{
    companion object
    {
        lateinit var instance : NotificationToolBar
            private set
    }

    val searchBox : MiniSearchBox
    val selectionTools = mutableSetOf<Component>()

    init
    {
        instance = this
        margin = Insets(0, 6, 0, 10)
        val process = JButton(FlatSVGIcon("icons/notification-process.svg", referenceSize, referenceSize)).setup()
        process.toolTipText = language.process
        process.action {

            if (NotificationList.instance.selectionMode)
            {
                LocalProfileAssistant.processNotification(*NotificationList.instance.selectedNotifications.map { it.notification.seq }.toIntArray())
            }
            else
            {
                val focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
                if (focusOwner is NotificationCard)
                {
                    LocalProfileAssistant.processNotification(focusOwner.notification.seq)
                }
            }
        }
        val remove = JButton(FlatSVGIcon("icons/notification-remove.svg", referenceSize, referenceSize)).setup()
        remove.toolTipText = language.remove
        remove.action {
            if (NotificationList.instance.selectionMode)
            {
                Actions.Notification.remove(*NotificationList.instance.selectedNotifications.map { it.notification.seq }.toIntArray())
            }
            else
            {
                val focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
                if (focusOwner is NotificationCard) Actions.Notification.remove(focusOwner.notification.seq)
            }
        }
        val deviceSelector = createDeviceSelector()
        val refreshDevices = createDevicesOperation()
        val refreshCard = createRefreshCard()
        val toProfile = JButton(FlatSVGIcon("icons/profile.svg", referenceSize, referenceSize)).setup()
        toProfile.toolTipText = language.`to-profile`
        toProfile.action {
            val focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
            if (focusOwner is NotificationCard) Actions.Notification.toProfile(focusOwner.notification.iccid ?: return@action)
        }
        val showDetails = JToggleButton(FlatSVGIcon("icons/show.svg", referenceSize - 4, referenceSize - 4)).setup()
        showDetails.toolTipText = language.`show-details`
        showDetails.model = LocalProfileAssistant.showDetails

        val selectAll = JButton(FlatSVGIcon("icons/select-all.svg", referenceSize, referenceSize)).setup()
        selectAll.toolTipText = language.`select-all`
        selectAll.action {
            val count = NotificationList.instance.cards.onEach { it.selectionCheckbox.isSelected = true }.count()
            NotificationPanel.instance.bottomInfo.selectedInfo.text = language.selected.format(count)
        }
        val cancelSelection = JButton(FlatSVGIcon("icons/cancel-selection.svg", referenceSize, referenceSize)).setup()
        cancelSelection.toolTipText = language.`cancel-selection`
        cancelSelection.action {
            NotificationList.instance.cards.forEach { it.selectionCheckbox.isSelected = false }
            NotificationList.instance.selectionMode = false
            NotificationPanel.instance.bottomInfo.selectedInfo.text = " "
        }
        val batchSelect = JButton(FlatSVGIcon("icons/batch-select.svg", referenceSize, referenceSize)).setup()
        batchSelect.toolTipText = language.`batch-select`
        batchSelect.action { Actions.Notification.batchSelect() }

        searchBox = MiniSearchBox() { NotificationList.instance.filterNotification() }
        add(process)
        add(remove)
        addSeparator(Dimension(2, referenceSize + 5))
        add(Box.createHorizontalStrut(2))
        add(deviceSelector)
        add(refreshDevices)
        addSeparator(Dimension(2, referenceSize + 5))
        add(refreshCard)
        add(toProfile)
        add(showDetails)
        add(Box.createHorizontalStrut(2))
        add(Box.createHorizontalGlue())
        add(Separator(Dimension(2, referenceSize + 5)).selectionTool())
        add(selectAll.selectionTool())
        add(cancelSelection.selectionTool())
        add(batchSelect.selectionTool())
        add(Separator(Dimension(2, referenceSize + 5)).selectionTool())
        add(searchBox)
    }

    private fun <T : Component> T.selectionTool() : T
    {
        this.isVisible = false
        selectionTools.add(this)
        return this
    }
}