package moe.sekiu.minilpa.ui.component

import com.formdev.flatlaf.FlatClientProperties
import com.formdev.flatlaf.extras.FlatSVGIcon
import com.formdev.flatlaf.icons.FlatSearchIcon
import java.awt.Dimension
import javax.swing.JTextField
import javax.swing.JToggleButton
import javax.swing.JToolBar
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import moe.sekiu.minilpa.action
import moe.sekiu.minilpa.language

class MiniSearchBox(var usePreferredWidth : Boolean = true, updateSearch : MiniSearchBox.() -> Unit) : JTextField()
{
    val matchCase = JToggleButton(FlatSVGIcon("icons/match-case.svg")).apply {
        rolloverIcon = FlatSVGIcon("icons/match-case-hovered.svg")
        selectedIcon = FlatSVGIcon("icons/match-case-selected.svg")
        toolTipText = language.`match-case`
        action { updateSearch() }
    }

    val wholeWords = JToggleButton(FlatSVGIcon("icons/words.svg")).apply {
        rolloverIcon = FlatSVGIcon("icons/words-hovered.svg")
        selectedIcon = FlatSVGIcon("icons/words-selected.svg")
        toolTipText = language.`whole-words`
        action { updateSearch() }
    }

    val regularExpression = JToggleButton(FlatSVGIcon("icons/regex.svg")).apply {
        rolloverIcon = FlatSVGIcon("icons/regex-hovered.svg")
        selectedIcon = FlatSVGIcon("icons/regex-selected.svg")
        toolTipText = language.`regular-expression`
        action { updateSearch() }
    }

    init
    {
        toolTipText = language.search
        putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true)
        putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, FlatSearchIcon())
        putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, JToolBar().apply {
            addSeparator()
            add(matchCase)
            add(wholeWords)
            add(regularExpression)
        })
        document.addDocumentListener(object : DocumentListener
        {
            override fun insertUpdate(event : DocumentEvent) = updateSearch()

            override fun removeUpdate(event : DocumentEvent) = updateSearch()

            override fun changedUpdate(event : DocumentEvent) { }
        })
    }


    override fun getMaximumSize() : Dimension = super.getMaximumSize().apply {
        height = preferredSize.height
        if (usePreferredWidth) width = preferredSize.width
    }
}