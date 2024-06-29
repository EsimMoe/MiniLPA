package moe.sekiu.minilpa.ui.component

import com.formdev.flatlaf.extras.FlatSVGIcon
import java.awt.Dimension
import javax.swing.AbstractButton
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JToolBar
import moe.sekiu.minilpa.action
import moe.sekiu.minilpa.language
import moe.sekiu.minilpa.model.Device
import moe.sekiu.minilpa.setting
import moe.sekiu.minilpa.lpa.LocalProfileAssistant

open class MiniToolBar : JToolBar()
{
    companion object
    {
        val referenceSize = 20

        val devicesOperations = mutableListOf<JButton>()
    }

    fun <T : AbstractButton> T.setup() : T
    {
        preferredSize = Dimension(28, 28)
        return this
    }

    fun createDeviceSelector() = object : JComboBox<Device>(LocalProfileAssistant.devices) { override fun getMaximumSize() = preferredSize
    }.apply {
        toolTipText = language.`device-selector`
        isFocusable = false
        preferredSize = Dimension(180,  27)
    }

    fun createDevicesOperation() = JButton().apply {
        setup()
        action { setting.backend.onOperationButtonAction(it) }
        devicesOperations.add(this)
    }

    fun createRefreshCard() = JButton(FlatSVGIcon("icons/refresh-profile.svg", referenceSize, referenceSize)).apply {
        setup()
        toolTipText = language.`refresh-card`
        action { LocalProfileAssistant.refreshCardData() }
    }
}