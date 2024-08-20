package moe.sekiu.minilpa

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.decodeFromStream
import com.charleskorn.kaml.encodeToStream
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.extras.FlatAnimatedLafChange
import com.formdev.flatlaf.extras.FlatSVGIcon
import com.formdev.flatlaf.themes.FlatMacDarkLaf
import com.formdev.flatlaf.themes.FlatMacLightLaf
import com.jthemedetecor.OsThemeDetector
import java.awt.Color
import java.io.File
import java.util.*
import java.util.function.Consumer
import java.util.function.Function
import java.util.zip.ZipInputStream
import javax.swing.SwingUtilities
import javax.swing.UIManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import moe.sekiu.minilpa.lpa.LPABackend
import moe.sekiu.minilpa.lpa.LPACExecutor
import moe.sekiu.minilpa.model.Language
import moe.sekiu.minilpa.model.Manifest
import moe.sekiu.minilpa.model.Setting
import moe.sekiu.minilpa.ui.MainFrame
import moe.sekiu.minilpa.ui.component.MiniThemePanel
import org.apache.commons.lang3.SystemUtils
import org.slf4j.LoggerFactory

internal val constructedCallback = mutableListOf<() -> Unit>()

val platform = getPlatformInfo()

val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))

val json = Json { ignoreUnknownKeys = true }

val appDataFolder = if (BuildConfig.IS_PACKAGED) File(System.getProperty("user.home"), ".minilpa") else File(".")

val languageFolder = File(appDataFolder, "languages")

val logFolder = File(appDataFolder, "logs")

val lpacFolder = File(appDataFolder, platform)

private val log = setupLogBack()

var setting : Setting = setupSetting()

var language : Language = setupLanguage()

lateinit var backend : LPABackend<*>

inline val mainFrame : MainFrame
    get() = MainFrame.instance

fun main()
{
    try
    {
        extractResources()
        setupTheme()
        setupIconColorFilter()
        backend = LPACExecutor()
        MainFrame()
        mainFrame.isVisible = true
    } catch (th : Throwable) { log.error("We have encountered an error", th) }
}

fun addConstructedHook(callback : () -> Unit) = constructedCallback.add(callback)

internal val updateTheme = Consumer<Boolean> { isDark ->
    val (name, default) = if (!isDark || setting.`auto-night-mode` == Setting.AutoNightMode.DISABLED) setting.`daytime-theme` to FlatMacLightLaf()
    else setting.`nighttime-theme` to FlatMacDarkLaf()
    val theme = MiniThemePanel.findTheme(name, default)
    val block = Runnable {
        FlatAnimatedLafChange.showSnapshot()
        UIManager.setLookAndFeel(theme::class.qualifiedName)
        FlatLaf.updateUI()
        FlatAnimatedLafChange.hideSnapshotWithAnimation()
    }
    if (Thread.currentThread().threadId() == 1L || SwingUtilities.isEventDispatchThread()) block.run()
    else SwingUtilities.invokeLater(block)
}

fun setupTheme()
{
    OsThemeDetector.getDetector().registerListener(updateTheme)
    updateTheme.accept(OsThemeDetector.getDetector().isDark)
}

fun setupIconColorFilter()
{
    FlatSVGIcon.ColorFilter.getInstance().mapper = Function { color ->
        when(color)
        {
            Color(110, 110, 110) -> UIManager.getColor("Actions.Grey") ?: color
            else -> color
        }
    }
}

fun setupSetting() : Setting = setupYamlFile("setting.yaml")

fun setupLanguage() : Language
{
    if (BuildConfig.LANGUAGE_PACK_UPDATE_TIME > setting.`language-pack-update-time`)
    {
        ZipInputStream(bufferedResourceStream("languages.zip")).unzip(languageFolder)
        File(languageFolder, "${Locale.US}.yaml").outputStream().buffered().use { out -> yaml.encodeToStream(Language(), out) }
        setting.update { `language-pack-update-time` = BuildConfig.LANGUAGE_PACK_UPDATE_TIME }
    }
    if (!File(languageFolder, "${setting.language}.yaml").exists()) setting.update { setting.language = Locale.US }
    return setupYamlFile("languages/${setting.language}.yaml")
}

fun setupFontSize()
{
    object {}::class.java.getResourceAsStream("/font-size.json")!!
        .use { `in` -> json.decodeFromStream<Map<String, Float>>(`in`) }
        .forEach { (key, size) ->
            val font = UIManager.getFont(key) ?: return@forEach
            UIManager.put(key, font.deriveFont(size))
        }
}

inline fun <reified T> setupYamlFile(path : String) : T
{
    val file = File(appDataFolder, path)
    file.parentFile?.mkdirs()
    val createNew = { T::class.java.getConstructor().newInstance() }
    val t : T = if (file.exists()) runCatching<T> {
        file.inputStream().buffered().use { `in` -> yaml.decodeFromStream(`in`) }
    }.getOrElse { createNew() } else createNew()
    file.outputStream().buffered().use { out -> yaml.encodeToStream(t, out) }
    return t
}

fun extractResources()
{
    Manifest.loadManifests()
    if (BuildConfig.LPAC_BUILD_TIME > setting.`lpac-build-time`)
    {
        ZipInputStream(bufferedResourceStream("$platform.zip")).unzip(lpacFolder)
        setting.update { `lpac-build-time` = BuildConfig.LPAC_BUILD_TIME }
    }
}

fun setupLogBack() : Logger
{
    System.setProperty("LOG_FOLDER", logFolder.canonicalPath)
    val context = LoggerFactory.getILoggerFactory().cast<LoggerContext>()
    val log = context.getLogger("Main")
    log.info("MiniLPA ${BuildConfig.VERSION}")
    log.info("Runtime Version: ${SystemUtils.JAVA_VM_VERSION} ${SystemUtils.OS_ARCH}")
    log.info("VM: ${SystemUtils.JAVA_VM_NAME}, ${SystemUtils.JAVA_VM_VENDOR}")
    log.info("IS_PACKAGE -> ${BuildConfig.IS_PACKAGED}")
    log.info("SHORT_COMMIT_ID -> ${BuildConfig.SHORT_COMMIT_ID}")
    log.info("AppDataFolder -> ${appDataFolder.canonicalPath}")
    return log
}