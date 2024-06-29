package moe.sekiu.minilpa.ui.component

import java.awt.Component
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.WindowConstants
import moe.sekiu.minilpa.language
import moe.sekiu.minilpa.ui.MainFrame
import net.miginfocom.swing.MigLayout

class MiniProgressDialog(owner : MainFrame, private val relativeComponentCallback : () -> Component) : JDialog(owner, language.processing, true)
{
    val progressBar = MiniProgressBar()
    val progressInfo = JLabel(" ")
    init
    {
        defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        focusableWindowState = false
        val processPanel = JPanel()
        processPanel.layout = MigLayout("wrap 1", "", "")
        processPanel.add(progressInfo)
        processPanel.add(progressBar, "pushX, growX")
        val optionPane = JOptionPane(
            processPanel,
            JOptionPane.PLAIN_MESSAGE,
            JOptionPane.DEFAULT_OPTION,
            null,
            emptyArray()
        )
        contentPane.add(optionPane)
        isResizable = false
        addComponentListener(object : ComponentAdapter()
        {
            override fun componentMoved(event : ComponentEvent)
            {
                setLocationRelativeTo(relativeComponentCallback())
            }
        })
    }

    override fun setVisible(visible : Boolean)
    {
        if (visible)
        {
            pack()
            setLocationRelativeTo(relativeComponentCallback())
        }
        super.setVisible(visible)
    }
}