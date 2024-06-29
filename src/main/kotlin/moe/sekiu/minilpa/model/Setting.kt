package moe.sekiu.minilpa.model

import com.charleskorn.kaml.encodeToStream
import com.formdev.flatlaf.extras.FlatSVGIcon
import com.formdev.flatlaf.themes.FlatMacDarkLaf
import com.formdev.flatlaf.themes.FlatMacLightLaf
import java.awt.event.ActionEvent
import java.io.File
import java.util.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import moe.sekiu.minilpa.appDataFolder
import moe.sekiu.minilpa.drop
import moe.sekiu.minilpa.language
import moe.sekiu.minilpa.lpa.LocalProfileAssistant
import moe.sekiu.minilpa.ui.component.MiniToolBar
import moe.sekiu.minilpa.yaml

@Serializable
data class Setting(
    @Serializable(with = LocaleSerializer::class)
    var language : Locale = Locale.getDefault(),
    var `daytime-theme` : String = FlatMacLightLaf.NAME,
    var `nighttime-theme` : String = FlatMacDarkLaf.NAME,
    var `emoji-design` : EmojiDesign = EmojiDesign.TWEMOJI,
    var `auto-night-mode` : AutoNightMode = AutoNightMode.SYSTEM,
    val debug : Debug = Debug(),
    val `notification-behavior` : NotificationBehavior = NotificationBehavior(),
    var `show-details` : Boolean = true,

    var `lpac-build-time` : Long = 0,
    var `euicc-info-update-time` : Long = 0,
    var `language-pack-update-time` : Long = 0,
)
{
    enum class EmojiDesign
    {
        @SerialName("twemoji") TWEMOJI,
        @SerialName("emojitwo") EMOJITWO,
        @SerialName("openmoji") OPENMOJI;

        override fun toString() = name.lowercase()
    }

    enum class AutoNightMode
    {
        SYSTEM { override fun toString() = language.system },
        DISABLED { override fun toString() = language.disabled }
    }

    var backend : Backend = Backend.LPACExecutor
        set(value)
        {
            value.updateOperationButtonAppearances()
            field = value
        }

    @Serializable
    enum class Backend()
    {
        @SerialName("lpac_executor")
        LPACExecutor
        {
            override fun onOperationButtonAction(event : ActionEvent) = LocalProfileAssistant.refreshDeviceData()
            override fun updateOperationButtonAppearances()
            {
                for (button in MiniToolBar.devicesOperations) with(button)
                {
                    icon =
                        FlatSVGIcon("icons/refresh.svg", MiniToolBar.referenceSize - 4, MiniToolBar.referenceSize - 4)
                    toolTipText = language.`refresh-devices`
                }
            }
        },

        @SerialName("remote_lpa")
        RemoteLPA
        {
            override fun onOperationButtonAction(event : ActionEvent)
            {

            }

            override fun updateOperationButtonAppearances()
            {

            }
        };

        abstract fun onOperationButtonAction(event : ActionEvent)
        abstract fun updateOperationButtonAppearances()
    }

    @Serializable
    data class Debug(val libeuicc : LibEuicc = LibEuicc())
    {
        @Serializable
        data class LibEuicc(
            var apdu : Boolean = false,
            var http : Boolean = false
        )
    }

    @Serializable
    data class NotificationBehavior(
        val install : Install = Install(),
        val enable : Enable = Enable(),
        val disable : Disable = Disable(),
        val delete : Delete = Delete()
    )
    {
        @Serializable sealed class NotificationType(var process : Boolean = true, var remove : Boolean = false)
        @Serializable class Install() : NotificationType()
        @Serializable class Enable() : NotificationType(process = false)
        @Serializable class Disable() : NotificationType(process = false)
        @Serializable class Delete() : NotificationType()
    }

    fun update(block : Setting.() -> Unit)
    {
        @Suppress("UNUSED_EXPRESSION")
        block()
        save()
    }

    fun save() = File(appDataFolder, "setting.yaml").outputStream().buffered().use { out -> yaml.encodeToStream(this, out) }.drop()
}

object LocaleSerializer : KSerializer<Locale>
{
    override val descriptor = PrimitiveSerialDescriptor("Locale", PrimitiveKind.STRING)

    override fun deserialize(decoder : Decoder) = Locale.forLanguageTag(decoder.decodeString())

    override fun serialize(encoder : Encoder, value : Locale) = encoder.encodeString(value.toLanguageTag())
}