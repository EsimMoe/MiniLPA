package moe.sekiu.minilpa.ui.component

import com.formdev.flatlaf.FlatClientProperties
import com.formdev.flatlaf.extras.FlatSVGIcon
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.UIManager
import javax.swing.border.LineBorder
import kotlinx.serialization.json.encodeToJsonElement
import moe.sekiu.minilpa.action
import moe.sekiu.minilpa.json
import moe.sekiu.minilpa.language
import moe.sekiu.minilpa.model.CIManifest
import moe.sekiu.minilpa.resize
import moe.sekiu.minilpa.toCountryFlagImage
import net.miginfocom.swing.MigLayout

class CertificateIssuerCard(val certificateIssuer : CIManifest.CertificateIssuer) : JButton()
{
    init
    {
        addComponentListener(object : ComponentAdapter()
        {
            override fun componentResized(event : ComponentEvent)
            {
                this@CertificateIssuerCard.preferredSize = this@CertificateIssuerCard.size.apply { width = 540 }
                this@CertificateIssuerCard.updateUI()
            }
        })
        layout = MigLayout("",
            "[]20[]-5",
            "[]0[]"
        )
        putClientProperty(
            FlatClientProperties.STYLE,
            """
                [light]background: tint(@background,50%);
                [dark]background: shade(@background,15%);
            """)
        val name = JLabel(certificateIssuer.name)
        name.putClientProperty(FlatClientProperties.STYLE_CLASS, "h3")
        val openExternal = JButton(FlatSVGIcon("icons/chip-certificate-open-external.svg", 18, 18))
        openExternal.toolTipText = language.`open-external-link`
        openExternal.action { certificateIssuer.openExternalLink() }
        openExternal.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS)
        openExternal.isFocusable = false

        val infoPanel = JPanel()
        infoPanel.layout = MigLayout()
        infoPanel.isOpaque = false
        infoPanel.add("cell 0 0, pushX", name)
        if (certificateIssuer.name != language.unknown) infoPanel.add("cell 1 0", openExternal)
        infoPanel.add("cell 0 1", JLabel(certificateIssuer.keyId))
        certificateIssuer.crls?.also { crls -> infoPanel.add("cell 0 2, grow, push, span", MiniJsonTree("crls", json.encodeToJsonElement(crls)).apply { border = LineBorder(UIManager.getColor("TabbedPane.contentAreaColor")) }) }
        add("cell 0 0", JLabel(ImageIcon(certificateIssuer.country.toCountryFlagImage().resize(64, 64))).apply { certificateIssuer.country?.also { toolTipText = it } })
        add("cell 1 0, top, grow, push", infoPanel)
    }
}