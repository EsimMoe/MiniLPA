package moe.sekiu.minilpa.ui.component

import com.formdev.flatlaf.FlatClientProperties
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.extras.FlatSVGIcon
import com.formdev.flatlaf.ui.FlatMenuItemUI
import java.awt.Color
import java.awt.Dimension
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.plaf.MenuItemUI
import javax.swing.plaf.basic.BasicMenuItemUI
import moe.sekiu.minilpa.action
import moe.sekiu.minilpa.autoHighlight
import moe.sekiu.minilpa.cast
import moe.sekiu.minilpa.language
import moe.sekiu.minilpa.mask
import moe.sekiu.minilpa.model.Profile
import moe.sekiu.minilpa.setClipboard
import moe.sekiu.minilpa.ui.Actions
import moe.sekiu.minilpa.lpa.LocalProfileAssistant
import net.miginfocom.swing.MigLayout


class ProfileCard(val profile : Profile) : JButton()
{
    var isProfileEnabled : Boolean = false
        set(value)
        {
            if (value)
            {
                putClientProperty(
                    FlatClientProperties.STYLE,
                    """
                        background: @selectionBackground;
                        foreground: @selectionForeground;
                    """
                )
                isEnabled = false
            } else
            {
                putClientProperty(
                    FlatClientProperties.STYLE,
                    """
                        [light]background: tint(@background,50%);
                        [dark]background: shade(@background,15%);
                    """)
            }

            field = value
        }
    
    val parent by lazy { super.getParent() as ProfileList }

    val iccidLabel = JLabel()

    override fun addNotify()
    {
        super.addNotify()
        if (isProfileEnabled) parent.enabledProfileCard = this
        if (profile.iccid == parent.focusProfileIccid) requestFocusInWindow()
    }

    init
    {
        layout = MigLayout(
            "fillY",
            "[]20[]",
        )
        this.isProfileEnabled = profile.state == Profile.State.ENABLED
        preferredSize = Dimension(305, 110)
        val profileIcon = ProfileIcon(profile.icon)
        val nickname = MiniEmojiLabel(profile.nickname ?: language.`empty-tag`)
        nickname.putClientProperty(FlatClientProperties.STYLE_CLASS, "h3")
        val editIcon = FlatSVGIcon("icons/edit.svg", 15, 15)
        val nicknameEdit = JButton(editIcon).setup()
        nicknameEdit.toolTipText = language.edit
        nicknameEdit.action { Actions.Profile.editNickname(profile.nickname, profile.iccid) }
        val copyIcon = FlatSVGIcon("icons/copy.svg", 15, 15)
        val iccidCopy = JButton(copyIcon).setup()
        iccidCopy.toolTipText = language.copy
        iccidCopy.action { setClipboard(profile.iccid) }
        switchIccidMask(LocalProfileAssistant.showDetails.isSelected)

        add("cell 0 0 1 3", profileIcon)
        add("cell 1 0, wmax 160", nickname)
        add("cell 1 0", nicknameEdit)
        add("cell 1 1", JLabel(profile.serviceProviderName))
        add("cell 1 2", iccidLabel)
        add("cell 1 2", iccidCopy)


        if (isProfileEnabled)
        {
            val colorFilter = FlatSVGIcon.ColorFilter {
                try
                {
                    return@ColorFilter FlatLaf.parseDefaultsValue("background", "@selectionForeground", null).cast<Color>()
                } catch (_ : IllegalArgumentException)
                {
                    return@ColorFilter UIManager.getColor("textHighlight")
                }
            }
            components.filterIsInstance<JLabel>().forEach { it.putClientProperty(FlatClientProperties.STYLE, "foreground: @selectionForeground") }
            nickname.putClientProperty(FlatClientProperties.STYLE, "foreground: @selectionForeground")
            nickname.updateUI()
            profileIcon.defaultSVGIcon.colorFilter = colorFilter
            editIcon.colorFilter = colorFilter
            copyIcon.colorFilter = colorFilter
            val adapter = object : MouseAdapter()
            {
                lateinit var background : Color
                override fun mouseEntered(event : MouseEvent)
                {
                    background = event.component.background
                    event.component.background = UIManager.getColor("Component.focusedBorderColor")
                }

                override fun mouseExited(evnet : MouseEvent)
                {
                    evnet.component.background = background
                }
            }
            nicknameEdit.addMouseListener(adapter)
            iccidCopy.addMouseListener(adapter)
            addFocusListener(object : FocusListener
            {
                override fun focusGained(event : FocusEvent)
                {
                    ProfileToolBar.instance.delete.isEnabled = false
                }

                override fun focusLost(event : FocusEvent)
                {
                    ProfileToolBar.instance.delete.isEnabled = true
                }
            })
        }

        addMouseListener(object : MouseAdapter()
        {
            override fun mouseClicked(event : MouseEvent)
            {
                if (this@ProfileCard.isEnabled &&
                    SwingUtilities.isLeftMouseButton(event) &&
                    parent.enabledProfileCard != this@ProfileCard &&
                    event.clickCount == 2) LocalProfileAssistant.enableProfile(profile.iccid)
            }
        })

        val referenceSize = 16
        val menu = JPopupMenu()
        if (isProfileEnabled) menu.add(JMenuItem(language.disable, FlatSVGIcon("icons/profile-disable.svg", referenceSize, referenceSize))
            .autoHighlight()
            .action { LocalProfileAssistant.disableProfile(profile.iccid) })
        else menu.add(JMenuItem(language.enable, FlatSVGIcon("icons/profile-enable.svg", referenceSize, referenceSize))
            .autoHighlight()
            .action { LocalProfileAssistant.enableProfile(profile.iccid) })
        menu.add(JMenuItem(language.`edit-nickname`, FlatSVGIcon("icons/profile-edit.svg", referenceSize, referenceSize))
            .autoHighlight()
            .action { Actions.Profile.editNickname(profile.nickname, profile.iccid) })
        menu.add(JMenuItem(language.`to-notification`, FlatSVGIcon("icons/notification.svg", referenceSize - 4, referenceSize - 4))
            .autoHighlight()
            .action { Actions.Profile.toNotification(profile.iccid) })
        menu.add(object : JMenuItem(language.delete, FlatSVGIcon("icons/profile-delete.svg", referenceSize, referenceSize))
        {
            init { isEnabled = !isProfileEnabled }

            override fun setUI(ui : MenuItemUI?)
            {
                super.setUI(when (ui)
                {
                    is FlatMenuItemUI -> object : FlatMenuItemUI() { init { selectionBackground = Color.RED } }
                    is BasicMenuItemUI -> object : BasicMenuItemUI() { init { selectionBackground = Color.RED } }
                    else -> ui
                })
            }
        }.autoHighlight().action { Actions.Profile.delete(profile.iccid) })
        componentPopupMenu = menu
    }

    private var isEnabled = true

    override fun setEnabled(enable : Boolean)
    {
        super.setEnabled(enable)
        isEnabled = enable
    }

    override fun isEnabled() : Boolean = true

    fun switchIccidMask(show : Boolean) { iccidLabel.text = if (show) profile.iccid else profile.iccid.mask(8) }

    fun JButton.setup() : JButton
    {
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS)
        isFocusable = false
        return this
    }
}