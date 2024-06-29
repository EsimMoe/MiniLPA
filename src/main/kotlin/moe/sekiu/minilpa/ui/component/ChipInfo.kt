package moe.sekiu.minilpa.ui.component

import com.formdev.flatlaf.FlatClientProperties
import com.formdev.flatlaf.extras.FlatSVGIcon
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.SwingUtilities
import moe.sekiu.minilpa.action
import moe.sekiu.minilpa.language
import moe.sekiu.minilpa.mask
import moe.sekiu.minilpa.model.ChipInfo
import moe.sekiu.minilpa.model.EumManifest
import moe.sekiu.minilpa.resize
import moe.sekiu.minilpa.setClipboard
import moe.sekiu.minilpa.toCountryFlagImage
import moe.sekiu.minilpa.ui.Actions
import moe.sekiu.minilpa.lpa.LocalProfileAssistant
import net.miginfocom.swing.MigLayout


class ChipInfo(var chipInfo : ChipInfo) : JPanel()
{
    companion object
    {
        var instance : moe.sekiu.minilpa.ui.component.ChipInfo? = null
            private set
    }

    val eid = JLabel().setup()

    init
    {
        instance = this
        layout = MigLayout("wrap 1, insets 5 15 15 15", "[fill]", "")
        switchChipInfoEidMask(LocalProfileAssistant.showDetails.isSelected)
        val copyEid = JButton(FlatSVGIcon("icons/copy.svg", 15, 15)).setup()
        copyEid.toolTipText = language.copy
        copyEid.action { setClipboard(chipInfo.eid) }
        val defaultSMDPAddr = JLabel("${language.`default-SMDP+-address`}: ${chipInfo.euiccConfiguredAddresses.defaultDpAddress ?: language.`not-set-tag`}").setup()
        val editDefaultSMDPAddr = JButton(FlatSVGIcon("icons/edit.svg", 15, 15)).setup()
        editDefaultSMDPAddr.toolTipText = language.edit
        editDefaultSMDPAddr.action { editDefaultSMDPAddr() }
        val rootSMDSAddr = JLabel("${language.`root-smds-address`}: ${chipInfo.euiccConfiguredAddresses.rootDsAddress}").setup()
        val copyRootSMDSAddr = JButton(FlatSVGIcon("icons/copy.svg", 15, 15)).setup()
        copyRootSMDSAddr.toolTipText = language.copy
        copyRootSMDSAddr.action { setClipboard(chipInfo.euiccConfiguredAddresses.rootDsAddress) }
        val countryFlag = object : JLabel()
        {
            override fun updateUI()
            {
                if (toolTipText != null) icon = ImageIcon(toolTipText.toCountryFlagImage().resize(20, 20), toolTipText)
                super.updateUI()
            }
        }
        var manufacturer = language.unknown
        var secureElement = ""
        val eum = EumManifest.findEum(chipInfo.eid)
        if (eum != null)
        {
            manufacturer = eum.manufacturer
            countryFlag.icon = ImageIcon(eum.country.toCountryFlagImage().resize(20, 20), eum.country.uppercase())
            countryFlag.toolTipText = eum.country.uppercase()
            val product = EumManifest.findProduct(eum, chipInfo.eid)
            if (product != null)
            {
                manufacturer += " (${product.name})"
                if (product.chip != null)
                {
                    secureElement = language.`secure-element`.format(product.chip)
                }
            }
        }

        val certificateIssuers = JButton(language.`certificate-issuers`, FlatSVGIcon("icons/chip-certificate.svg", 20, 20))
        certificateIssuers.action { Actions.Chip.viewCertificateIssuers(chipInfo.eUICCInfo2) }
        certificateIssuers.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS)
        certificateIssuers.isFocusable = false
        certificateIssuers.putClientProperty(FlatClientProperties.STYLE, "font: 130% \$light.font")

        add(MiniBidirectionalPanel(MiniGroup(eid, copyEid), MiniGroup(JLabel(language.manufacturer).setup(), countryFlag, JLabel(manufacturer).setup())))
        add(MiniBidirectionalPanel(MiniGroup(defaultSMDPAddr, editDefaultSMDPAddr), JLabel(secureElement).setup()))
        add(MiniBidirectionalPanel(MiniGroup(rootSMDSAddr, copyRootSMDSAddr), certificateIssuers))
        add("cell 0 3, span, push, grow", JScrollPane(MiniJsonTree("eUICCInfo2", chipInfo.eUICCInfo2)))
    }

    fun switchChipInfoEidMask(show : Boolean) { eid.text = "${language.eid}: ${if (show) chipInfo.eid else chipInfo.eid.mask(12)}" }

    fun JLabel.setup() : JLabel
    {
        putClientProperty(FlatClientProperties.STYLE, "font: 130% \$light.font")
        return this
    }

    fun JButton.setup() : JButton
    {
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS)
        isFocusable = false
        return this
    }

    fun editDefaultSMDPAddr()
    {
        val window = SwingUtilities.windowForComponent(this)
        val result = JOptionPane.showInputDialog(
            window,
            language.`edit-default-SMDP+-address-message`,
            language.`edit-default-SMDP+-address`,
            JOptionPane.QUESTION_MESSAGE,
            null,
            null,
            chipInfo.euiccConfiguredAddresses.defaultDpAddress
        ) as String?
        if (result != null && result != chipInfo.euiccConfiguredAddresses.defaultDpAddress) LocalProfileAssistant.editDefaultSMDPAddress(result)
    }
}

