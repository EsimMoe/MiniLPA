package moe.sekiu.minilpa.ui.component

import com.formdev.flatlaf.extras.FlatSVGIcon
import java.awt.Dimension
import java.awt.Insets
import javax.swing.JToggleButton
import moe.sekiu.minilpa.language
import moe.sekiu.minilpa.lpa.LocalProfileAssistant

class ChipToolBar : MiniToolBar()
{
    init
    {
        margin = Insets(0, 15, 0, 15)
        val deviceSelector = createDeviceSelector()
        val refreshDevices = createDevicesOperation()
        val refreshCard = createRefreshCard()
        val showDetails = JToggleButton(FlatSVGIcon("icons/show.svg", referenceSize - 4, referenceSize - 4)).setup()
        showDetails.toolTipText = language.`show-details`
        showDetails.model = LocalProfileAssistant.showDetails
        add(deviceSelector)
        add(refreshDevices)
        addSeparator(Dimension(2, referenceSize + 5))
        add(refreshCard)
        add(showDetails)
    }
}