package moe.sekiu.minilpa.ui.component

import com.formdev.flatlaf.extras.FlatSVGIcon
import java.awt.Dimension
import java.awt.Insets
import java.awt.KeyboardFocusManager
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.Box
import javax.swing.JButton
import javax.swing.JToggleButton
import moe.sekiu.minilpa.action
import moe.sekiu.minilpa.addConstructedHook
import moe.sekiu.minilpa.language
import moe.sekiu.minilpa.setting
import moe.sekiu.minilpa.ui.Actions
import moe.sekiu.minilpa.lpa.LocalProfileAssistant

class ProfileToolBar : MiniToolBar()
{
    companion object
    {
        lateinit var instance : ProfileToolBar
            private set
    }

    val searchBox : MiniSearchBox
    val delete : JButton

    init
    {
        instance = this
        margin = Insets(0, 6, 0, 10)
        val download = JButton(FlatSVGIcon("icons/profile-download.svg", referenceSize, referenceSize)).setup()
        download.toolTipText = language.download
        download.action { Actions.Profile.download() }
        delete = JButton(FlatSVGIcon("icons/profile-delete.svg", referenceSize, referenceSize)).setup()
        delete.toolTipText = language.delete
        delete.action {
            val focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
            if (focusOwner is ProfileCard) Actions.Profile.delete(focusOwner.profile.iccid)
        }
        val enable = JButton(FlatSVGIcon("icons/profile-enable.svg", referenceSize, referenceSize)).setup()
        enable.toolTipText = language.enable
        enable.action {
            val focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
            if (focusOwner is ProfileCard && !focusOwner.isProfileEnabled) LocalProfileAssistant.enableProfile(focusOwner.profile.iccid)
        }
        val disable = JButton(FlatSVGIcon("icons/profile-disable.svg", referenceSize, referenceSize)).setup()
        disable.toolTipText = language.disable
        disable.action {
            val focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
            if (focusOwner is ProfileCard && focusOwner.isProfileEnabled) LocalProfileAssistant.disableProfile(focusOwner.profile.iccid)
        }
        val nickname = JButton(FlatSVGIcon("icons/profile-edit.svg", referenceSize, referenceSize)).setup()
        nickname.toolTipText = language.`edit-nickname`
        nickname.action {
            val focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
            if (focusOwner is ProfileCard) Actions.Profile.editNickname(focusOwner.profile.nickname, focusOwner.profile.iccid)
        }
        val deviceSelector = createDeviceSelector()
        val refreshDevices = createDevicesOperation()
        addConstructedHook {
            refreshDevices.doClick()
            setting.backend.updateOperationButtonAppearances()
        }
        val refreshCard = createRefreshCard()
        val toNotification = JButton(FlatSVGIcon("icons/notification.svg", referenceSize - 4, referenceSize - 4)).setup()
        toNotification.toolTipText = language.`to-notification`
        toNotification.action {
            val focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
            if (focusOwner is ProfileCard) Actions.Profile.toNotification(focusOwner.profile.iccid)
        }
        val showDetails = JToggleButton(FlatSVGIcon("icons/show.svg", referenceSize - 4, referenceSize - 4)).setup()
        showDetails.toolTipText = language.`show-details`
        showDetails.model = LocalProfileAssistant.showDetails
        searchBox = MiniSearchBox(false) { ProfileList.instance.filterProfile() }
        searchBox.addComponentListener(object : ComponentAdapter()
        {
            override fun componentResized(event : ComponentEvent)
            {
                searchBox.preferredSize = searchBox.size
                NotificationToolBar.instance.searchBox.preferredSize = searchBox.size
                NotificationToolBar.instance.searchBox.updateUI()
                searchBox.usePreferredWidth = true
            }
        })
        MiniPasteArea(searchBox) { Actions.Profile.download(it) }

        add(download)
        add(delete)
        addSeparator(Dimension(2, referenceSize + 5))
        add(enable)
        add(disable)
        add(nickname)
        addSeparator(Dimension(2, referenceSize + 5))
        add(Box.createHorizontalStrut(2))
        add(deviceSelector)
        add(refreshDevices)
        addSeparator(Dimension(2, referenceSize + 5))
        add(refreshCard)
        add(toNotification)
        add(showDetails)
        addSeparator(Dimension(2, referenceSize + 5))
        add(Box.createHorizontalStrut(2))
        add(Box.createHorizontalGlue())
        add(searchBox)
    }
}