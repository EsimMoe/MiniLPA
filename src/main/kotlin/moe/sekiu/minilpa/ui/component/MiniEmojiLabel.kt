package moe.sekiu.minilpa.ui.component

import com.formdev.flatlaf.FlatClientProperties
import java.awt.FlowLayout
import java.awt.image.BufferedImage
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JLabel
import kotlinx.serialization.json.decodeFromStream
import moe.sekiu.minilpa.json
import moe.sekiu.minilpa.resize
import moe.sekiu.minilpa.setting
import moe.sekiu.minilpa.ui.MiniPanel

class MiniEmojiLabel() : MiniPanel()
{
    sealed interface Token
    {
        val value : String
        fun createLabel() : JLabel
    }

    data class EmojiToken(override val value : String) : Token
    {
        override fun createLabel() : JLabel
        {
            val unicodeScalars = value.codePoints().toArray()
            val unicodeHex = unicodeScalars.joinToString("-") { Integer.toHexString(it) }
            val design = setting.`emoji-design`
            val key = "${design}_${unicodeHex}"
            var ref = emojiCache[key]
            if (ref == null)
            {
                val resource = object {}::class.java.getResource("/emoji/$design/$unicodeHex.png")
                ref = if (resource == null) WeakReference(null)
                else WeakReference(ImageIO.read(resource).resize(emojiSize, emojiSize))
            }

            return if (ref.get() == null) JLabel(value)
            else JLabel(ImageIcon(ref.get()))
        }
    }

    data class TextToken(override val value : String) : Token
    {
        override fun createLabel() : JLabel = JLabel(value)
    }

    var text : String = ""
        set(value)
        {
            removeAll()
            value.toEmojiAndText().forEach { add(it.createLabel().setup()) }
            field = value
        }

    companion object
    {
        private val emojiSize : Int = 20
        private val emojis : List<String> = object {}::class.java.getResource("/emoji.json")!!.openStream().buffered().use { `in` -> json.decodeFromStream(`in`) }
        private val unicodePattern = emojis.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }.toRegex()
        private val emojiCache = ConcurrentHashMap<String, WeakReference<BufferedImage>>()

        private fun String.toEmojiAndText() = sequence {
            var end = 0
            unicodePattern.findAll(this@toEmojiAndText).forEach {
                val previousText = substring(end, it.range.first)
                if (previousText.isNotBlank()) yield(TextToken(previousText))
                yield(EmojiToken(it.value))
                end = it.range.last + 1
            }
            val previousText = substring(end)
            if (previousText.isNotBlank()) yield(TextToken(previousText))
        }
    }

    constructor(text : String) : this() { this.text = text }

    init
    {
        layout = FlowLayout(FlowLayout.LEFT, 0, 0)
        this.text = text
        isOpaque = false
    }

    override fun updateUI()
    {
        @Suppress("UNNECESSARY_SAFE_CALL")
        text?.apply {
            removeAll()
            text.toEmojiAndText().forEach { add(it.createLabel().setup()) }
        }
        super.updateUI()
    }

    fun JLabel.setup() : JLabel
    {
        putClientProperty(FlatClientProperties.STYLE_CLASS, this@MiniEmojiLabel.getClientProperty(FlatClientProperties.STYLE_CLASS))
        putClientProperty(FlatClientProperties.STYLE, this@MiniEmojiLabel.getClientProperty(FlatClientProperties.STYLE))
        return this
    }
}