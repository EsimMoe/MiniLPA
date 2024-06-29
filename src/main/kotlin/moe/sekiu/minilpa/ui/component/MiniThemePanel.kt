package moe.sekiu.minilpa.ui.component

import com.formdev.flatlaf.FlatDarculaLaf
import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatIntelliJLaf
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.FlatLightLaf
import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes
import com.formdev.flatlaf.themes.FlatMacDarkLaf
import com.formdev.flatlaf.themes.FlatMacLightLaf
import com.formdev.flatlaf.ui.FlatListUI
import com.jthemedetecor.OsThemeDetector
import java.awt.Component
import java.awt.Graphics
import java.lang.invoke.MethodType
import javax.swing.AbstractListModel
import javax.swing.DefaultListCellRenderer
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.border.Border
import javax.swing.border.CompoundBorder
import javax.swing.plaf.basic.BasicLookAndFeel
import kotlin.reflect.KMutableProperty1
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import moe.sekiu.minilpa.CatMagic
import moe.sekiu.minilpa.cast
import moe.sekiu.minilpa.language
import moe.sekiu.minilpa.model.Setting
import moe.sekiu.minilpa.setting
import moe.sekiu.minilpa.updateTheme

class MiniThemePanel(val themeProp : KMutableProperty1<Setting, String>) : JList<BasicLookAndFeel>()
{
    companion object
    {
        val categories = mutableMapOf<Int, String>()
        val themes = mutableListOf<BasicLookAndFeel>()

        fun findTheme(name : String, default : BasicLookAndFeel = FlatMacLightLaf()) = themes.find { it.name == name } ?: default

        init
        {
            categories[themes.size] = language.`core-themes`
            themes.add(FlatLightLaf())
            themes.add(FlatDarkLaf())
            themes.add(FlatIntelliJLaf())
            themes.add(FlatDarculaLaf())
            themes.add(FlatMacLightLaf())
            themes.add(FlatMacDarkLaf())
            categories[themes.size] = language.`material-ui-lite-themes`
            themes.addAll(FlatAllIJThemes.INFOS.filter { "(Material)" in it.name }
                .map { Class.forName(it.className).getConstructor().newInstance().cast<FlatLaf>() })
            categories[themes.size] = language.`collection-themes`
            themes.addAll(FlatAllIJThemes.INFOS.filter { "(Material)" !in it.name }
                .map { Class.forName(it.className).getConstructor().newInstance().cast<FlatLaf>() })
            // TODO developer mode
            categories[themes.size] = language.`native-themes`
            UIManager.getInstalledLookAndFeels().map {
                CatMagic.lookup.findConstructor(Class.forName(it.className),
                    MethodType.methodType(Void.TYPE)).invoke().cast<BasicLookAndFeel>() }.forEach { themes.add(it) }
        }
    }

    private var isAdjustingThemesList = false

    init
    {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        addListSelectionListener { event ->
            val selected = selectedValue ?: return@addListSelectionListener
            if (event.valueIsAdjusting) return@addListSelectionListener
            if (isAdjustingThemesList) return@addListSelectionListener
            setting.update { themeProp.set(this, selected.name) }
            SwingUtilities.invokeLater { updateTheme.accept(OsThemeDetector.getDetector().isDark) }
        }
    }

    override fun addNotify()
    {
        GlobalScope.launch(Dispatchers.IO) {
            val name = themeProp.get(setting)
            val index = themes.indexOfFirst { it.name == name }
            isAdjustingThemesList = true
            selectedIndex = index
            isAdjustingThemesList = false
        }
        super.addNotify()
    }

    override fun getModel() = object : AbstractListModel<BasicLookAndFeel>()
    {
        override fun getSize() : Int = themes.size
        override fun getElementAt(index : Int) = themes[index]
    }

    override fun getCellRenderer() = object : DefaultListCellRenderer()
    {
        private var index = 0
        private var isSelected = false
        private var titleHeight = 0

        override fun getListCellRendererComponent(
            list : JList<*>, value : Any,
            index : Int, isSelected : Boolean, cellHasFocus : Boolean
        ) : Component
        {
            this.index = index
            this.isSelected = isSelected
            this.titleHeight = 0

            val title = categories[index]

            val c = super.getListCellRendererComponent(list, value.cast<BasicLookAndFeel>().name, index, isSelected, cellHasFocus) as JComponent
            if (title != null)
            {
                val titledBorder : Border = ListCellTitledBorder(this@MiniThemePanel, title)
                c.border = CompoundBorder(titledBorder, c.border)
                titleHeight = titledBorder.getBorderInsets(c).top
            }
            return c
        }

        override fun isOpaque() = !isSelectedTitle

        override fun paintComponent(g : Graphics)
        {
            if (isSelectedTitle)
            {
                g.color = background
                FlatListUI.paintCellSelection(this@MiniThemePanel, g, index, 0, titleHeight, width, height - titleHeight)
            }

            super.paintComponent(g)
        }

        private val isSelectedTitle = titleHeight > 0 && isSelected && UIManager.getLookAndFeel() is FlatLaf
    }
}