package moe.sekiu.minilpa.ui

import com.formdev.flatlaf.FlatClientProperties
import java.awt.BorderLayout
import javax.swing.JScrollPane
import moe.sekiu.minilpa.addConstructedHook
import moe.sekiu.minilpa.language
import moe.sekiu.minilpa.mainFrame
import moe.sekiu.minilpa.setup
import moe.sekiu.minilpa.ui.component.BottomInfo
import moe.sekiu.minilpa.ui.component.MiniDropArea
import moe.sekiu.minilpa.ui.component.MiniPasteArea
import moe.sekiu.minilpa.ui.component.ProfileList
import moe.sekiu.minilpa.ui.component.ProfileToolBar


class ProfilePanel : MiniPanel()
{
    init
    {
        layout = BorderLayout()
        add(ProfileToolBar(), BorderLayout.NORTH)
        val scrollPane = JScrollPane(ProfileList()).setup()
        addConstructedHook {
            MiniDropArea(mainFrame, scrollPane) { Actions.Profile.download(it) }.apply {
                dropHereLabel.text = "<html><p align=\"center\">${language.`profile-drop-here`.replace("\n", "<br />")}</p></html>"
                dropHereLabel.putClientProperty(FlatClientProperties.STYLE, "font: 220% \$light.font")
                toAutoParseLabel.text = language.`to-auto-parse`
                toAutoParseLabel.putClientProperty(FlatClientProperties.STYLE, "font: 180% \$light.font")
            }
            MiniPasteArea(scrollPane) { Actions.Profile.download(it) }
        }
        add(scrollPane, BorderLayout.CENTER)
        add(BottomInfo(), BorderLayout.SOUTH)
    }
}