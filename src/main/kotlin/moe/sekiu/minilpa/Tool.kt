package moe.sekiu.minilpa

import com.formdev.flatlaf.FlatClientProperties
import com.formdev.flatlaf.extras.FlatSVGIcon
import com.formdev.flatlaf.ui.FlatOptionPaneUI
import com.google.zxing.BinaryBitmap
import com.google.zxing.NotFoundException
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.qrcode.QRCodeMultiReader
import io.ktor.websocket.*
import java.awt.Component
import java.awt.Container
import java.awt.Desktop
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.io.File
import java.net.URI
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.imageio.ImageIO
import javax.swing.AbstractButton
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTabbedPane
import javax.swing.JTextField
import javax.swing.JToolBar
import javax.swing.UIManager
import javax.swing.border.EmptyBorder
import javax.swing.plaf.basic.BasicOptionPaneUI
import kotlin.reflect.KProperty1
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import moe.sekiu.minilpa.model.ActivationCode
import moe.sekiu.minilpa.ui.component.MiniSearchBox
import org.apache.commons.lang3.ArchUtils
import org.apache.commons.lang3.SystemUtils
import org.slf4j.LoggerFactory


inline fun <reified T> Any?.cast() = this as T

inline fun <reified T> Any?.castOrNull() = this as? T?

inline fun <reified T> decode(element : JsonElement) : T = json.decodeFromJsonElement(element)

suspend inline fun <reified T> SendChannel<Frame>.send(value : T) = send(Frame.Text(json.encodeToString(value)))

suspend inline fun <reified T> ReceiveChannel<Frame>.receive() = receive().castOrNull<Frame.Text>()?.readText()?.let { json.decodeFromString<T>(it) }

inline fun Any?.drop() = Unit

fun setClipboard(content : String)
{
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(content), null)
}

fun freeze(container : Container, enabled : Boolean)
{
    freezeInternal(container, enabled)
    if (enabled) disabledComponents.clear()
}

private val disabledComponents = mutableSetOf<Component>()
private fun freezeInternal(container : Container, enabled : Boolean)
{
    fun Component.setEnable(enable : Boolean)
    {
        if (enable)
        {
            if (this !in disabledComponents) isEnabled = true
        } else
        {
            if (!isEnabled) disabledComponents.add(this)
            isEnabled = false
        }
    }

    for (c in container.components)
    {
        if (c is JPanel)
        {
            freezeInternal(c as Container, enabled)
            continue
        }


        c.setEnable(enabled)

        if (c is JScrollPane)
        {
            val view = c.viewport.view
            view?.setEnable(enabled)
            (view as? Container)?.also { freezeInternal(view, enabled) }
        } else if (c is JTabbedPane)
        {
            val tabCount = c.tabCount
            for (i in 0 until tabCount)
            {
                c.getComponentAt(i)?.setEnable(enabled)
            }
        }

        if (c is JToolBar || c is JButton || c is JTextField) freezeInternal(c as Container, enabled)
    }
}

fun JComponent.outlineError() = putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR)

fun JComponent.outlineClear() = putClientProperty(FlatClientProperties.OUTLINE, null)

fun <C : JComponent, D> MiniSearchBox.filter(components : List<C>, dataProp : KProperty1<C, D>, filterProps : List<KProperty1<D, Any?>>)
{
    runCatching {
        var input = text
        if (!regularExpression.isSelected) input = Regex.escape(input)
        if (wholeWords.isSelected) input = """\b$input\b"""
        val regex = if (matchCase.isSelected) Regex(input) else Regex(input, RegexOption.IGNORE_CASE)
        val func = if (regularExpression.isSelected) regex::matches else regex::containsMatchIn
        for (component in components)
        {
            val data = dataProp.get(component)
            for (prop in filterProps)
            {
                var value = "${prop.get(data) ?: continue}"
                if (prop.name == "seq") value = "#$value"
                if (func(value))
                {
                    component.isVisible = true
                    break
                }
                component.isVisible = false
            }
        }
    }.onFailure { outlineError() }.onSuccess { outlineClear() }
}

fun JOptionPane.setInitialValueIndex(index : Int)
{
    if (ui is FlatOptionPaneUI) setUI(object : FlatOptionPaneUI() { override fun getInitialValueIndex() = index })
    else setUI(object : BasicOptionPaneUI() { override fun getInitialValueIndex() = index })
}

fun showDangerConfirmDialog(parent : Component?, message : Any, title : String) : Boolean
{
    with(JOptionPane(message, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null)) {
        setInitialValueIndex(1)
        createDialog(parent, title).apply {
            isVisible = true
            dispose()
        }
        val selectedValue = value ?: return false

        if (options == null)
        {
            if (selectedValue is Int) return selectedValue == JOptionPane.OK_OPTION
            return false
        }
        options.forEach { value -> if (value == selectedValue) return value == JOptionPane.OK_OPTION }
        return false
    }
}

inline fun <T : AbstractButton> T.action(listener : ActionListener) : T
{
    addActionListener(listener)
    return this
}

fun JMenuItem.autoHighlight() : JMenuItem
{
    addMouseListener(object : MouseAdapter()
    {
        override fun mouseEntered(event : MouseEvent)
        {
            icon.cast<FlatSVGIcon>().colorFilter = FlatSVGIcon.ColorFilter { UIManager.getColor("textHighlightText") }
        }

        override fun mouseExited(evnet : MouseEvent)
        {
            icon.cast<FlatSVGIcon>().colorFilter = null
        }

        override fun mouseReleased(event : MouseEvent)
        {
            icon.cast<FlatSVGIcon>().colorFilter = null
        }
    })
    return this
}

fun JScrollPane.setup() : JScrollPane
{
    horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
    verticalScrollBar.unitIncrement = 15
    border = EmptyBorder(0, 0, 0, 0)
    return this
}

fun <T : Component> T.noFocus() : T
{
    isFocusable = false
    return this
}

fun JCheckBox.enableWhenChecked(vararg components : Component, block : (Boolean) -> Unit = {})
{
    components.forEach { it.isEnabled = isSelected }
    action {
        components.forEach { it.isEnabled = isSelected }
        block(isSelected)
    }
}

fun countryCodeToUnicodeCodePoint(countryCode : String) : String
{
    require(countryCode.length == 2)
    val base = 0x1F1E6
    return countryCode.map { it.uppercaseChar() - 'A' + base }
        .toTypedArray()
        .joinToString("-") { it.toString(16) }
}

fun String?.toCountryFlagImage() : BufferedImage
{
    val name = "/emoji/${setting.`emoji-design`}/${if (this == null) "1f5fa" else countryCodeToUnicodeCodePoint(this)}.png"
    return ImageIO.read(object {}::class.java.getResource(name))
}

fun Image.buffered() : BufferedImage
{
    if (this is BufferedImage) return this
    val bufferedImage = BufferedImage(getWidth(null), getHeight(null), BufferedImage.TYPE_INT_ARGB)
    val graphics = bufferedImage.createGraphics()
    graphics.drawImage(this, 0, 0, null)
    graphics.dispose()
    return bufferedImage
}

fun BufferedImage.resize(width : Int, height : Int) : BufferedImage = getScaledInstance(width, height, Image.SCALE_DEFAULT).buffered()

fun BufferedImage.parseActivationCode() : ActivationCode?
{
    val bitmap = BinaryBitmap(HybridBinarizer(BufferedImageLuminanceSource(this)))
    val results = try { QRCodeMultiReader().decodeMultiple(bitmap) } catch (_ : NotFoundException) { return null }
    for (result in results) return ActivationCode.of(result.text) ?: continue
    return null
}

fun String.openLink()
{
    if (Desktop.isDesktopSupported())
    {
        val desktop = Desktop.getDesktop()
        if (desktop.isSupported(Desktop.Action.BROWSE))
        {
            desktop.browse(URI(this))
        }
    }
}

fun File.openExplorer()
{
    if (Desktop.isDesktopSupported())
    {
        val desktop = Desktop.getDesktop()
        if (desktop.isSupported(Desktop.Action.OPEN))
        {
            desktop.open(this)
        }
    }
}

fun String.mask(start : Int) = replaceRange(start, length, "*".repeat(length - start))

fun getPlatformInfo() : String
{
    val os = if (SystemUtils.IS_OS_MAC_OSX) "macos"
    else if (SystemUtils.IS_OS_WINDOWS) "windows"
    else if (SystemUtils.IS_OS_LINUX) "linux"
    else throw UnsupportedOperationException("Unsupported os ${SystemUtils.OS_NAME}")

    val processor = ArchUtils.getProcessor()
    val arch = if (os == "macos") "universal"
    else if (processor.isX86) "x86"
    else if (processor.isAarch64) "aarch64"
    else throw UnsupportedOperationException("Unsupported arch ${processor.type.label}")
    return "${os}_${arch}"
}

inline fun Any.logger() = LoggerFactory.getLogger(this::class.simpleName)

fun bufferedResourceStream(name : String) = object {}::class.java.getResourceAsStream("/$name")!!.buffered()


fun ZipInputStream.unzip(folder : File)
{
    use { `in` ->
        folder.mkdirs()
        var entry : ZipEntry? = `in`.nextEntry
        while (entry != null) {
            val file = File(folder, entry.name)
            file.outputStream().use { out -> `in`.copyTo(out) }
            file.setExecutable(true)
            entry = `in`.nextEntry
        }
    }
}