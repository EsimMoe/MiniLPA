package moe.sekiu.minilpa.ui.component

import com.formdev.flatlaf.FlatClientProperties
import java.awt.Component
import java.awt.Dialog
import java.awt.Dimension
import java.awt.Frame
import java.awt.Image
import java.awt.KeyboardFocusManager
import java.awt.MouseInfo
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.geom.RoundRectangle2D
import java.awt.im.InputContext
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.RootPaneContainer
import javax.swing.SwingUtilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import moe.sekiu.minilpa.CatMagic
import moe.sekiu.minilpa.buffered
import moe.sekiu.minilpa.cast
import moe.sekiu.minilpa.drop
import moe.sekiu.minilpa.language
import moe.sekiu.minilpa.logger
import moe.sekiu.minilpa.mainFrame
import moe.sekiu.minilpa.model.ActivationCode
import moe.sekiu.minilpa.parseActivationCode
import net.miginfocom.swing.MigLayout
import org.apache.commons.lang3.SystemUtils

class MiniDropArea : JDialog
{
    private val log = logger()
    val triggerArea : Component
    val reference : Component
    val block : (ActivationCode) -> Unit
    private val focusPlaceHolder = object : JPanel()
    {
        init { isOpaque = false }

        override fun getInputContext() : InputContext? = null
    }
    private var originFocus : Component? = null
    val dropHereLabel = JLabel()
    val toAutoParseLabel = JLabel()

    companion object
    {
        private val blockedDataFlavor = listOf<DataFlavor>(
            DataFlavor.selectionHtmlFlavor,
            DataFlavor.fragmentHtmlFlavor,
            DataFlavor.allHtmlFlavor
        )
    }

    constructor(parent : Dialog?, triggerArea : Component, reference : Component = triggerArea, block : (ActivationCode) -> Unit) : super(parent, false)
    {
        this.triggerArea = triggerArea
        this.reference = reference
        this.block = block
        setup()
    }

    constructor(parent : Frame?, triggerArea : Component, reference : Component = triggerArea, block : (ActivationCode) -> Unit) : super(parent, false)
    {
        this.triggerArea = triggerArea
        this.reference = reference
        this.block = block
        setup()
    }

    private fun setup()
    {
        isUndecorated = true
        isAlwaysOnTop = true
        if (SystemUtils.IS_OS_WINDOWS) SwingUtilities.windowForComponent(triggerArea).cast<RootPaneContainer>().rootPane.add(focusPlaceHolder)
        contentPane = JPanel().apply {
            putClientProperty(FlatClientProperties.STYLE,
                """
                    [light]border: 16,16,16,16,shade(@background,10%),,15;
                    [dark]border: 16,16,16,16,tint(@background,10%),,15;
                """)
            layout = MigLayout()
            add("cell 0 0, push, al center, flowY", dropHereLabel)
            add("cell 0 0, al center", toAutoParseLabel)
            if (SystemUtils.IS_OS_WINDOWS)
            {
                addFocusListener(object : FocusListener
                {
                    val setter = CatMagic.lookup.findSetter(FocusEvent::class.java, "consumed", Boolean::class.javaPrimitiveType)
                    override fun focusGained(event : FocusEvent) = setter.invoke(event, true).drop()
                    override fun focusLost(event : FocusEvent) = setter.invoke(event, false).drop()
                })
            }
        }

        DropTarget(triggerArea, DnDConstants.ACTION_NONE, object : DropTargetAdapter()
        {
            override fun dragEnter(event : DropTargetDragEvent)
            {
                event.rejectDrag()
                if (event.transferable.transferDataFlavors.isEmpty() || blockedDataFlavor.any { event.isDataFlavorSupported(it) }) return
                size = Dimension((reference.size.width * 0.8).toInt(), (reference.size.height * 0.8).toInt())
                runCatching { shape = RoundRectangle2D.Double(0.0, 0.0, width.toDouble(), height.toDouble(), 13.0, 13.0) }
                setLocationRelativeTo(reference)
                isVisible = true
            }

            override fun dragExit(event : DropTargetEvent)
            {
                val loc = MouseInfo.getPointerInfo().location
                SwingUtilities.convertPointFromScreen(loc, triggerArea)
                if (!inTriggerArea()) dispose()
                else if (!inDropArea()) dispose()
            }

            override fun drop(event : DropTargetDropEvent) { dispose() }
        })

        DropTarget(contentPane, DnDConstants.ACTION_COPY, object : DropTargetAdapter()
        {

            override fun dragEnter(event : DropTargetDragEvent)
            {
                // TODO: Change text color
            }

            override fun dragExit(event : DropTargetEvent?)
            {
                if (!inTriggerArea()) dispose()
                else if (inDropArea()) dispose()
            }

            override fun drop(event : DropTargetDropEvent)
            {
                var work = {}
                val transferable = event.transferable
                if (transferable != null)
                {
                    try
                    {
                        event.acceptDrop(event.dropAction)
                        var activationCode : ActivationCode? = null
                        if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor))
                        {
                            val text = transferable.getTransferData(DataFlavor.stringFlavor).cast<String>().trim()
                            log.info("Text drop -> $text")
                            activationCode = ActivationCode.of(text)
                        }
                        if (activationCode == null && transferable.isDataFlavorSupported(DataFlavor.imageFlavor))
                        {
                            val image = transferable.getTransferData(DataFlavor.imageFlavor).cast<Image>()
                            log.info("Image drop -> $image")
                            activationCode = image.buffered().parseActivationCode()
                        }
                        if (activationCode == null && transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
                        {
                            val files = transferable.getTransferData(DataFlavor.javaFileListFlavor).cast<List<File>>()
                            log.info("Files drop -> $files")
                            for (file in files)
                            {
                                if (!file.isFile) continue
                                val image = ImageIO.read(file) ?: continue
                                activationCode = image.parseActivationCode() ?: continue
                                break
                            }
                        }
                        log.info("ActivationCode -> $activationCode")
                        work = if (activationCode != null)
                        {
                            { block(activationCode) }
                        } else
                        {
                            { JOptionPane.showMessageDialog(mainFrame, language.`ac-parseing-failed`, language.`operation-failed-title`, JOptionPane.ERROR_MESSAGE) }
                        }
                    } catch (th : Throwable) {
                        work = {
                            log.error("We have encountered an error", th)
                            JOptionPane.showMessageDialog(
                                mainFrame,
                                "${language.`operation-failed-error`}\n" +
                                        "${language.`operation-failed-type`.format(th::class.simpleName)}\n" +
                                        "${language.`operation-failed-info`.format(th.message)}\n\n" +
                                        language.`operation-failed-check-the-log`,
                                language.`operation-failed-title`,
                                JOptionPane.ERROR_MESSAGE
                            )
                        }
                    }
                }
                disposeWithWork(work)
            }
        })
    }

    fun inTriggerArea() : Boolean
    {
        val location = MouseInfo.getPointerInfo().location
        val relativeLocation = location.also { SwingUtilities.convertPointFromScreen(it, triggerArea) }
        return triggerArea.contains(relativeLocation)
    }

    fun inDropArea() : Boolean
    {
        val location = MouseInfo.getPointerInfo().location
        val relativeLocation = location.also { SwingUtilities.convertPointFromScreen(it, this@MiniDropArea) }
        return contains(relativeLocation)
    }

    override fun setVisible(visible : Boolean)
    {
        if (visible && SystemUtils.IS_OS_WINDOWS)
        {
            originFocus = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
            focusPlaceHolder.requestFocusInWindow()
        }
        super.setVisible(visible)
    }

    override fun dispose() = disposeWithWork()

    fun disposeWithWork(work : () -> Unit = {})
    {
        if (SystemUtils.IS_OS_WINDOWS)
        {
            val focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
            CatMagic.lookup.findStaticSetter(focusManager::class.java, "focusOwner", Component::class.java).invoke(null)
        }
        GlobalScope.launch(Dispatchers.Swing) {
            delay(20)
            super.dispose()
            work()
        }
    }
}