package moe.sekiu.minilpa.ui

import com.charleskorn.kaml.decodeFromStream
import com.charleskorn.kaml.encodeToStream
import com.formdev.flatlaf.FlatClientProperties
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.extras.FlatSVGIcon
import com.jthemedetecor.OsThemeDetector
import java.awt.Color
import java.io.File
import java.util.*
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JScrollPane
import javax.swing.JSeparator
import kotlinx.coroutines.runBlocking
import moe.sekiu.minilpa.BuildConfig
import moe.sekiu.minilpa.action
import moe.sekiu.minilpa.backend
import moe.sekiu.minilpa.cast
import moe.sekiu.minilpa.enableWhenChecked
import moe.sekiu.minilpa.language
import moe.sekiu.minilpa.languageFolder
import moe.sekiu.minilpa.logFolder
import moe.sekiu.minilpa.logger
import moe.sekiu.minilpa.lpacFolder
import moe.sekiu.minilpa.mainFrame
import moe.sekiu.minilpa.model.Language
import moe.sekiu.minilpa.model.Setting
import moe.sekiu.minilpa.noFocus
import moe.sekiu.minilpa.openExplorer
import moe.sekiu.minilpa.setting
import moe.sekiu.minilpa.ui.component.MiniGroup
import moe.sekiu.minilpa.ui.component.MiniThemePanel
import moe.sekiu.minilpa.updateTheme
import moe.sekiu.minilpa.yaml
import net.miginfocom.swing.MigLayout
import org.apache.commons.lang3.SystemUtils


class SettingPanel : MiniPanel()
{
    init
    {
        layout = MigLayout("wrap 1, fillX")
        separator(language.about)
        add(AboutPanel())
        separator(language.language)
        add(LanguagePanel())
        separator(language.log)
        add(LogPanel())
        separator(language.backend)
        add(BackendPanel())
        separator(language.behavior)
        add(BehaviorPanel())
        separator(language.appearance)
        add(AppearancePanel())
    }

    class AboutPanel : MiniPanel()
    {
        init
        {
            layout = MigLayout("", "[]20[]")
            val infoPanel = MiniPanel()
            infoPanel.layout = MigLayout("wrap 1, insets 0")
            infoPanel.add(MiniGroup(
                JLabel("MiniLPA ${BuildConfig.VERSION}").apply { putClientProperty(FlatClientProperties.STYLE_CLASS, "h3") },
                MiniPanel().apply {
                    layout = MigLayout("insets 2")
                    isOpaque = true
                    background = Color(128, 128, 128, 64)
                    putClientProperty(FlatClientProperties.STYLE, "arc: 8")
                    add(JLabel(BuildConfig.SHORT_COMMIT_ID).apply {
                        foreground = Color(128, 128, 128, 255)
                        putClientProperty(FlatClientProperties.STYLE, "font: \$mini.font")
                    })
                }
            ))
            infoPanel.add(JLabel(language.`runtime-version`.format("${SystemUtils.JAVA_VM_VERSION} ${SystemUtils.OS_ARCH}")))
            infoPanel.add(JLabel(language.VM.format("${SystemUtils.JAVA_VM_NAME}, ${SystemUtils.JAVA_VM_VENDOR}")))
            add(JLabel(FlatSVGIcon("icons/window.svg", 64, 64)))
            add(infoPanel, "top")
        }
    }

    class LanguagePanel : MiniPanel()
    {
        data class LanguageItem(val id : String, val language : Language)
        {
            override fun toString() : String = language.`language-name`
        }

        init
        {
            layout = MigLayout()
            val languageItems = mutableListOf<LanguageItem>()
            var currentLanguageItem : LanguageItem? = null
            languageFolder.listFiles { it -> it.name.endsWith(".yaml") }?.also {
                for (file in it)
                {
                    val language = file.inputStream().buffered().use { `in` -> yaml.decodeFromStream<Language>(`in`) }
                    file.outputStream().buffered().use { out -> yaml.encodeToStream(language, out) }
                    val languageItem = LanguageItem(file.nameWithoutExtension, language)
                    languageItems.add(languageItem)
                    if ("${setting.language}" == languageItem.id) currentLanguageItem = languageItem
                }
            }
            val languageSelector = JComboBox(languageItems.toTypedArray()).noFocus()
            languageSelector.selectedItem = currentLanguageItem
            languageSelector.addActionListener {
                val selectedItem = languageSelector.selectedItem.cast<LanguageItem>()
                if ("${setting.language}" == selectedItem.id) return@addActionListener
                setting.update { language = Locale.forLanguageTag(selectedItem.id.replace('_', '-')) }
                language = selectedItem.language
                mainFrame.boot()
            }
            val openLanguageFolder = JButton(language.`open-language-folder`).noFocus()
            openLanguageFolder.action { languageFolder.openExplorer() }
            add(MiniGroup(JLabel("${language.language}:"), languageSelector, openLanguageFolder))
        }
    }

    class LogPanel() : MiniPanel()
    {
        init
        {
            val openLatestLog = JButton(language.`open-latest-log`).noFocus().action { File(logFolder, "latest.log").openExplorer() }
            val openLogFolder = JButton(language.`open-log-floder`).noFocus().action { logFolder.openExplorer() }
            add(MiniGroup(openLatestLog, openLogFolder))
        }
    }

    class BackendPanel : MiniPanel()
    {
        val log = logger()
        init
        {
            layout = MigLayout("wrap 1")
            val lpacPanel = object : MiniPanel()
            {
                val version = JLabel(" ")
                init
                {
                    layout = MigLayout("wrap 1")
                    val openlpacFolder = JButton(language.`open-lpac-folder`).noFocus().action { lpacFolder.openExplorer() }
                    add(MiniGroup(version, openlpacFolder))
                    add(JCheckBox(language.`libeuicc-apdu-debug`, setting.debug.libeuicc.apdu)
                        .noFocus().apply { action { setting.update { setting.debug.libeuicc.apdu = isSelected } } })
                    add(JCheckBox(language.`libeuicc-http-debug`, setting.debug.libeuicc.http)
                        .noFocus().apply { action { setting.update { setting.debug.libeuicc.http = isSelected } } })
                }

                override fun setVisible(visable : Boolean)
                {
                    if (visable) try { runBlocking { version.text = language.`lpac-version`.format(backend.getVersion()) } }
                    catch (_ : Throwable) { version.text = language.`lpac-version`.format(language.unknown) }
                    super.setVisible(visable)
                }
            }
            val remoteLPAPanel = object : MiniPanel()
            {
                init
                {
                    layout = MigLayout("wrap 1")
                    add(JLabel("Not yet realized"))
                }
            }
            val dataModel = object : DefaultComboBoxModel<Setting.Backend>(Setting.Backend.entries.toTypedArray())
            {
                override fun setSelectedItem(selected : Any?)
                {
                    super.setSelectedItem(selected)
                    if (selected is Setting.Backend)
                    {
                        setting.update { backend = selected }
                        when(selected)
                        {
                            Setting.Backend.LPACExecutor -> {
                                lpacPanel.isVisible = true
                                remoteLPAPanel.isVisible = false
                            }
                            Setting.Backend.RemoteLPA -> {
                                lpacPanel.isVisible = false
                                remoteLPAPanel.isVisible = true
                            }
                        }
                    }
                }
            }
            dataModel.selectedItem = setting.backend
            val backendSelector = JComboBox(dataModel).noFocus()
            add(MiniGroup(JLabel("${language.backend}:"), backendSelector))
            add(lpacPanel, "hidemode 3")
            add(remoteLPAPanel, "hidemode 3")
        }
    }

    class BehaviorPanel : MiniPanel()
    {
        init
        {
            layout = MigLayout("wrap 1")
            val installProcess = JCheckBox(language.`process-install-notification`, setting.`notification-behavior`.install.process).noFocus()
            val installRemove = JCheckBox(language.`and-remove`, setting.`notification-behavior`.install.remove).noFocus()
            installProcess.enableWhenChecked(installRemove)
            installProcess.action { setting.update { `notification-behavior`.install.process = installProcess.isSelected } }
            installRemove.action { setting.update { `notification-behavior`.install.remove = installRemove.isSelected } }
            add(MiniGroup(installProcess, installRemove))

            val deleteProcess = JCheckBox(language.`process-delete-notification`, setting.`notification-behavior`.delete.process).noFocus()
            val deleteRemove = JCheckBox(language.`and-remove`, setting.`notification-behavior`.delete.remove).noFocus()
            deleteProcess.enableWhenChecked(deleteRemove)
            deleteProcess.action { setting.update { `notification-behavior`.delete.process = deleteProcess.isSelected } }
            deleteRemove.action { setting.update { `notification-behavior`.delete.remove = deleteRemove.isSelected } }
            add(MiniGroup(deleteProcess, deleteRemove))

            val enableProcess = JCheckBox(language.`process-enable-notification`, setting.`notification-behavior`.enable.process).noFocus()
            val enableRemove = JCheckBox(language.`and-remove`, setting.`notification-behavior`.enable.remove).noFocus()
            enableProcess.enableWhenChecked(enableRemove)
            enableProcess.action { setting.update { `notification-behavior`.enable.process = enableProcess.isSelected } }
            enableRemove.action { setting.update { `notification-behavior`.enable.remove = enableRemove.isSelected } }
            add(MiniGroup(enableProcess, enableRemove))

            val disableProcess = JCheckBox(language.`process-disable-notification`, setting.`notification-behavior`.disable.process).noFocus()
            val disableRemove = JCheckBox(language.`and-remove`, setting.`notification-behavior`.disable.remove).noFocus()
            disableProcess.enableWhenChecked(disableRemove)
            disableProcess.action { setting.update { `notification-behavior`.disable.process = disableProcess.isSelected } }
            disableRemove.action { setting.update { `notification-behavior`.disable.remove = disableRemove.isSelected } }
            add(MiniGroup(disableProcess, disableRemove))
        }
    }

    class AppearancePanel : MiniPanel()
    {
        init
        {
            layout = MigLayout("wrap 1")
            val emojiDesignSelector = JComboBox(Setting.EmojiDesign.entries.toTypedArray()).noFocus()
            emojiDesignSelector.selectedItem = setting.`emoji-design`
            emojiDesignSelector.addActionListener {
                val selectedItem = emojiDesignSelector.selectedItem.cast<Setting.EmojiDesign>()
                if (setting.`emoji-design` == selectedItem) return@addActionListener
                setting.update { `emoji-design` = selectedItem }
                FlatLaf.updateUI()
            }
            val themePanel = ThemePanel()
            val autoNightModeSelector = JComboBox(Setting.AutoNightMode.entries.toTypedArray()).noFocus()
            autoNightModeSelector.selectedItem = setting.`auto-night-mode`
            autoNightModeSelector.addActionListener {
                val selectedItem = autoNightModeSelector.selectedItem.cast<Setting.AutoNightMode>()
                if (setting.`auto-night-mode` == selectedItem) return@addActionListener
                setting.update { `auto-night-mode` = selectedItem }
                updateTheme.accept(OsThemeDetector.getDetector().isDark)
                themePanel.setNightTimeThemePanelEnable(selectedItem == Setting.AutoNightMode.SYSTEM)
            }
            add(MiniGroup(JLabel("${language.`emoji-design`}:"), emojiDesignSelector))
            add(MiniGroup(JLabel("${language.`auto-night-mode`}:"), autoNightModeSelector))
            add(themePanel)
        }

        class ThemePanel() : MiniPanel()
        {
            val setNightTimeThemePanelEnable : (Boolean) -> Unit

            init
            {
                layout = MigLayout("insets 2, wrap 2", "[]20[]")
                val dayTimeLabel = JLabel(language.`daytime-theme`)
                dayTimeLabel.putClientProperty(FlatClientProperties.STYLE_CLASS, "h4")
                val dayTimeThemePanel = MiniThemePanel(Setting::`daytime-theme`)
                val nightTimeLabel = JLabel(language.`nighttime-theme`)
                nightTimeLabel.putClientProperty(FlatClientProperties.STYLE_CLASS, "h4")
                val nightTimeThemePanel = MiniThemePanel(Setting::`nighttime-theme`)
                setNightTimeThemePanelEnable = {
                    nightTimeLabel.isEnabled = it
                    nightTimeThemePanel.isEnabled = it
                }
                setNightTimeThemePanelEnable(setting.`auto-night-mode` == Setting.AutoNightMode.SYSTEM)
                add(dayTimeLabel, "al center")
                add(nightTimeLabel, "al center")
                add(JScrollPane(dayTimeThemePanel))
                add(JScrollPane(nightTimeThemePanel))
            }
        }
    }

    private fun separator(text : String)
    {
        add(JLabel(text), "split 2")
        add(JSeparator(), "growX")
    }
}