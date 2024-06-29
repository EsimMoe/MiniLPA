package moe.sekiu.minilpa.ui.component

import com.formdev.flatlaf.FlatClientProperties
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.UIManager
import javax.swing.border.MatteBorder
import moe.sekiu.minilpa.language
import moe.sekiu.minilpa.mainFrame
import net.miginfocom.swing.MigLayout

class BottomInfo : JPanel()
{
    companion object
    {
        private val list = mutableListOf<BottomInfo>()

        fun updateFreeSpace(bytes : Int) = list.forEach { it.freeSpace.text = language.`free-space`.format(formatSize(bytes)) }

        private fun formatSize(bytes : Int): String {
            return when {
                bytes >= 1024 * 1024 -> {
                    val sizeInMB = bytes.toDouble() / (1024 * 1024)
                    "%.2f MiB".format(sizeInMB)
                }
                bytes >= 1024 -> {
                    val sizeInKB = bytes.toDouble() / 1024
                    "%.2f KiB".format(sizeInKB)
                }
                else -> "$bytes B"
            }
        }
    }

    val selectedInfo = JLabel(" ").setup()
    val freeSpace = JLabel(" ").setup()

    init
    {
        list.add(this)
        layout = MigLayout("insets 0 12 5 12", "[]push[]")
        add(selectedInfo)
        add(freeSpace)
    }

    override fun updateUI()
    {
        mainFrame.tab.border
        border = MatteBorder(1, 0, 0, 0, UIManager.getColor("TabbedPane.contentAreaColor"))
        super.updateUI()
    }

    fun JLabel.setup() : JLabel
    {
        putClientProperty(FlatClientProperties.STYLE, "font: 16 \$light.font")
        return this
    }
}