package moe.sekiu.minilpa.ui.component

import java.awt.Image
import java.awt.MouseInfo
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.io.File
import javax.imageio.ImageIO
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.JTextField
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import moe.sekiu.minilpa.buffered
import moe.sekiu.minilpa.cast
import moe.sekiu.minilpa.logger
import moe.sekiu.minilpa.model.ActivationCode
import moe.sekiu.minilpa.parseActivationCode

class MiniPasteArea(val triggerArea : JComponent, val block : (ActivationCode) -> Unit) : AbstractAction()
{
    private val log = logger()
    init
    {
        with(triggerArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)) {
            put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK), "paste")
            put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, KeyEvent.SHIFT_DOWN_MASK), "paste")
        }
        triggerArea.actionMap.put("paste", this)
    }

    override fun actionPerformed(event : ActionEvent)
    {
        val location = MouseInfo.getPointerInfo().location
        val relativeLocation = location.location.also { SwingUtilities.convertPointFromScreen(it, triggerArea) }
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        if (triggerArea.contains(relativeLocation) || triggerArea is JTextField || !SwingUtilities.windowForComponent(triggerArea).bounds.contains(location))
        {
            var activationCode : ActivationCode? = null
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor))
            {
                val text = clipboard.getData(DataFlavor.stringFlavor).cast<String>().trim()
                activationCode = ActivationCode.of(text)
                if (activationCode == null && triggerArea is JTextField)
                {
                    triggerArea.text = text
                    return
                }
                log.info("Text paste -> $text")
            }
            if (activationCode == null && clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor))
            {
                val image = clipboard.getData(DataFlavor.imageFlavor).cast<Image>()
                log.info("Image paste -> $image")
                activationCode = image.buffered().parseActivationCode()
            }
            if (activationCode == null && clipboard.isDataFlavorAvailable(DataFlavor.javaFileListFlavor))
            {
                val files = clipboard.getData(DataFlavor.javaFileListFlavor).cast<List<File>>()
                log.info("Files paste -> $files")
                for (file in files)
                {
                    if (!file.isFile) continue
                    val image = ImageIO.read(file) ?: continue
                    activationCode = image.parseActivationCode() ?: continue
                    break
                }
            }
            log.info("ActivationCode -> $activationCode")
            if (activationCode != null) block(activationCode)
        }
    }
}