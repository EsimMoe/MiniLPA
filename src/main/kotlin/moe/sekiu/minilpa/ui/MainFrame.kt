package moe.sekiu.minilpa.ui

import com.formdev.flatlaf.FlatClientProperties
import com.formdev.flatlaf.extras.FlatSVGIcon
import com.formdev.flatlaf.extras.FlatSVGUtils
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.JScrollPane
import javax.swing.JTabbedPane
import javax.swing.SwingConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonPrimitive
import moe.sekiu.minilpa.backend
import moe.sekiu.minilpa.constructedCallback
import moe.sekiu.minilpa.exception.LPAOperationFailureException
import moe.sekiu.minilpa.exception.OperationFailureException
import moe.sekiu.minilpa.freeze
import moe.sekiu.minilpa.language
import moe.sekiu.minilpa.logger
import moe.sekiu.minilpa.setup
import moe.sekiu.minilpa.setupFontSize
import moe.sekiu.minilpa.ui.component.MiniProgressDialog


class MainFrame : JFrame()
{
    private val log = logger()

    companion object
    {
        lateinit var instance : MainFrame
    }

    var tab = JTabbedPane()
        private set

    private val processingDialog = MiniProgressDialog(this) { tab.selectedComponent }
    val progressBar
        get() = processingDialog.progressBar
    val progressInfo
        get() = processingDialog.progressInfo

    init
    {
        instance = this
        setupFontSize()
        title = "MiniLPA"
        iconImages = FlatSVGUtils.createWindowIconImages("/icons/window.svg")
        defaultCloseOperation = EXIT_ON_CLOSE
        minimumSize = Dimension(780, 600)
        size = Dimension(780, 600)
        setLocationRelativeTo(null)
        boot()
        addComponentListener(object : ComponentAdapter()
        {
            override fun componentResized(event : ComponentEvent)
            {
                val size = event.component.size
                tab.preferredSize = size.also { it.height = size.height }
            }
        })
    }

    fun boot()
    {
        contentPane.removeAll()
        tab = with(JTabbedPane())
        {
            tabPlacement = JTabbedPane.LEFT
            putClientProperty(
                FlatClientProperties.TABBED_PANE_TAB_AREA_ALIGNMENT,
                FlatClientProperties.TABBED_PANE_ALIGN_FILL
            )
            putClientProperty(FlatClientProperties.TABBED_PANE_TAB_ICON_PLACEMENT, SwingConstants.TOP)
            putClientProperty(FlatClientProperties.TABBED_PANE_MINIMUM_TAB_WIDTH, 120)
            addTab(language.profile, FlatSVGIcon("icons/profile.svg", 30, 30), ProfilePanel())
            addTab(language.notification, FlatSVGIcon("icons/notification.svg", 25, 25), NotificationPanel())
            addTab(language.chip, FlatSVGIcon("icons/chip.svg", 28, 28), ChipPanel())
            addTab(language.setting, FlatSVGIcon("icons/setting.svg", 30, 30), JScrollPane(SettingPanel()).setup())
            if (tab.selectedIndex != -1) selectedIndex = tab.selectedIndex
            this
        }
        add(tab)
        tab.updateUI()
        processingDialog.title = language.processing
        constructedCallback.forEach { it() }
        constructedCallback.clear()
    }

    suspend fun <T> tryExecute(block : suspend () -> T) : T?
    {
        return try
        {
            block()
        } catch (ex : OperationFailureException)
        {
            withContext(Dispatchers.Swing)
            {
                processingDialog.dispose()
                var errorMessage : String
                if (ex is LPAOperationFailureException)
                {
                    errorMessage = language.`operation-failed-stage`.format(ex.message)
                    errorMessage += "\n${language.`operation-failed-info`.format(if (ex.data !is JsonNull) ex.data.jsonPrimitive.content else language.`empty-tag`)}"
                } else errorMessage = ex.message
                errorMessage += "\n\n${language.`operation-failed-check-the-log`}"
                JOptionPane.showMessageDialog(
                    this@MainFrame,
                    errorMessage,
                    language.`operation-failed-title`,
                    JOptionPane.ERROR_MESSAGE
                )
            }
            return null
        } catch (th : Throwable)
        {
            withContext(Dispatchers.Swing) {
                processingDialog.dispose()
                log.error("We have encountered an error", th)
                JOptionPane.showMessageDialog(
                    this@MainFrame,
                    "${language.`operation-failed-error`}\n" +
                            "${language.`operation-failed-type`.format(th::class.simpleName)}\n" +
                            "${language.`operation-failed-info`.format(th.message)}\n\n" +
                            language.`operation-failed-check-the-log`,
                    language.`operation-failed-title`,
                    JOptionPane.ERROR_MESSAGE
                )
            }
            return null
        }
    }

    suspend fun <T> freezeWithTimeout(
        successMessage : String? = null,
        requireDevice : Boolean = true,
        timeMillis : Long =  300000,
        block : suspend () -> T
    ) : T?
    {
        if (requireDevice && backend.selectedDevice == null)
        {
            JOptionPane.showMessageDialog(
                this@MainFrame,
                language.`operation-failed-select-device`,
                language.`operation-failed-title`,
                JOptionPane.ERROR_MESSAGE
            )
            return null
        }
        try
        {
            freeze()
            progressBar.reset()
            progressInfo.text = language.initializing
            GlobalScope.launch(Dispatchers.Swing) { processingDialog.isVisible = true }
            val result = withTimeout(timeMillis) { tryExecute(block) } ?: return null
            progressBar.swipeTo(progressBar.maximum).join()
            progressInfo.text = language.complete
            delay(100)
            processingDialog.dispose()
            if (successMessage != null) JOptionPane.showMessageDialog(
                this,
                successMessage,
                language.`operation-success-title`,
                JOptionPane.INFORMATION_MESSAGE
            )
            return result
        } finally
        {
            unFreeze()
        }
    }

    fun freeze() = freeze(tab, false)

    fun unFreeze()
    {
        freeze(tab, true)
    }
}