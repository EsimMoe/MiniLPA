package moe.sekiu.minilpa.ui

import com.formdev.flatlaf.FlatClientProperties
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.plaf.basic.BasicOptionPaneUI
import kotlinx.serialization.json.JsonObject
import moe.sekiu.minilpa.cast
import moe.sekiu.minilpa.enableWhenChecked
import moe.sekiu.minilpa.language
import moe.sekiu.minilpa.lpa.LocalProfileAssistant
import moe.sekiu.minilpa.mainFrame
import moe.sekiu.minilpa.model.ActivationCode
import moe.sekiu.minilpa.model.CIManifest
import moe.sekiu.minilpa.model.DownloadInfo
import moe.sekiu.minilpa.model.Notification.Operation.DELETE
import moe.sekiu.minilpa.model.Notification.Operation.DISABLE
import moe.sekiu.minilpa.model.Notification.Operation.ENABLE
import moe.sekiu.minilpa.model.Notification.Operation.INSTALL
import moe.sekiu.minilpa.outlineClear
import moe.sekiu.minilpa.outlineError
import moe.sekiu.minilpa.setting
import moe.sekiu.minilpa.setup
import moe.sekiu.minilpa.showDangerConfirmDialog
import moe.sekiu.minilpa.ui.component.CertificateIssuerCard
import moe.sekiu.minilpa.ui.component.MiniDropArea
import moe.sekiu.minilpa.ui.component.MiniPasteArea
import moe.sekiu.minilpa.ui.component.NotificationList
import moe.sekiu.minilpa.ui.component.NotificationToolBar
import moe.sekiu.minilpa.ui.component.ProfileToolBar
import net.miginfocom.swing.MigLayout


object Actions
{
    object Profile
    {
        fun download(activationCode : ActivationCode? = null)
        {
            val inputPanel = JPanel()
            inputPanel.layout = MigLayout("wrap 2")
            val smdpField = JTextField()
            val matchingIdField = JTextField()
            val confirmCodeLabel = JLabel(language.`confirmation-code`)
            val confirmCodeField = JTextField()
            class ClearOutlineListener(val component : JComponent) : DocumentListener
            {
                override fun insertUpdate(event : DocumentEvent) { component.outlineClear() }

                override fun removeUpdate(event : DocumentEvent) { }

                override fun changedUpdate(event : DocumentEvent) { }
            }
            confirmCodeField.document.addDocumentListener(ClearOutlineListener(confirmCodeField))
            val confirmCodeCheckBox = JCheckBox(language.`confirmation-code-required`)
            confirmCodeCheckBox.enableWhenChecked(confirmCodeLabel, confirmCodeField) { if (!it) confirmCodeField.outlineClear() }
            val IMEILabel = JLabel(language.IMEI)
            val IMEIField = JTextField()
            IMEIField.document.addDocumentListener(ClearOutlineListener(IMEIField))
            val IMEICheckBox = JCheckBox(language.`specified-IMEI`)
            IMEICheckBox.enableWhenChecked(IMEILabel, IMEIField) { if (!it) IMEIField.outlineClear() }
            val processInstallNotificationCheckbox = JCheckBox(language.`process-install-notification`, setting.`notification-behavior`.install.process)
            val andRemoveCheckbox = JCheckBox(language.`and-remove`, setting.`notification-behavior`.install.remove)
            processInstallNotificationCheckbox.enableWhenChecked(andRemoveCheckbox)
            inputPanel.add(JLabel(language.`SMDP+`))
            inputPanel.add(smdpField, "grow, push")
            inputPanel.add(JLabel(language.`matching-id`))
            inputPanel.add(matchingIdField, "grow, push")
            inputPanel.add(confirmCodeCheckBox, "span, wrap")
            inputPanel.add(confirmCodeLabel)
            inputPanel.add(confirmCodeField, "grow, push")
            inputPanel.add(IMEICheckBox, "span, wrap")
            inputPanel.add(IMEILabel)
            inputPanel.add(IMEIField, "grow, push")
            inputPanel.add(processInstallNotificationCheckbox, "split 2, span")
            inputPanel.add(andRemoveCheckbox)

            fun deactivateDropTarget(component : Component)
            {
                component.dropTarget?.isActive = false
                if (component is Container) component.components.forEach { deactivateDropTarget(it) }
            }
            deactivateDropTarget(inputPanel)

            val downloadDialog = JDialog(mainFrame, language.`download-profile-title`, true)
            val optionPane = JOptionPane(
                inputPanel,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION)

            val buttonArea = optionPane.components.last().cast<JPanel>()
            val components = buttonArea.components
            val padding = buttonArea.layout.cast<BasicOptionPaneUI.ButtonAreaLayout>().padding
            buttonArea.layout = BoxLayout(buttonArea, BoxLayout.X_AXIS)
            // TODO: Add Scan module
//            buttonArea.add(JButton("Scan"))
            buttonArea.add(Box.createHorizontalGlue())
            buttonArea.add(components[0])
            buttonArea.add(Box.createHorizontalStrut(padding))
            buttonArea.add(components[1])

            optionPane.addPropertyChangeListener { event ->
                if (event.propertyName != JOptionPane.VALUE_PROPERTY || event.newValue == JOptionPane.UNINITIALIZED_VALUE) return@addPropertyChangeListener
                optionPane.value = JOptionPane.UNINITIALIZED_VALUE
                when(event.newValue)
                {
                    JOptionPane.OK_OPTION -> {
                        var valid = true
                        if (confirmCodeCheckBox.isSelected && confirmCodeField.text.isNullOrBlank())
                        {
                            confirmCodeField.outlineError()
                            valid = false
                        }
                        if (IMEICheckBox.isSelected && IMEIField.text.isNullOrBlank())
                        {
                            IMEIField.outlineError()
                            valid = false
                        }
                        if (!valid) return@addPropertyChangeListener
                        val downloadInfo = DownloadInfo(
                            smdpField.text,
                            matchingIdField.text,
                            if (confirmCodeCheckBox.isSelected) confirmCodeField.text else null,
                            if (IMEICheckBox.isSelected) IMEIField.text else null
                        )
                        downloadDialog.dispose()
                        LocalProfileAssistant.downloadProfile(downloadInfo, processInstallNotificationCheckbox.isSelected, andRemoveCheckbox.isSelected)
                    }

                    JOptionPane.CANCEL_OPTION -> downloadDialog.dispose()
                }
            }

            downloadDialog.contentPane.add(optionPane)
            downloadDialog.pack()
            downloadDialog.setLocationRelativeTo(mainFrame)
            downloadDialog.isResizable = false

            val fillInfo : (ActivationCode) -> Unit = { (SMDP, MatchingID, _, ReqConCode) ->
                smdpField.text = SMDP
                matchingIdField.text = MatchingID
                if (confirmCodeCheckBox.isSelected != ReqConCode) confirmCodeCheckBox.doClick()
            }
            activationCode?.also { fillInfo(it) }
            MiniDropArea(downloadDialog, downloadDialog.contentPane, inputPanel, fillInfo).apply {
                dropHereLabel.text = "<html><p align=\"center\">${language.`download-drop-here`.replace("\n", "<br />")}</p></html>"
                dropHereLabel.putClientProperty(FlatClientProperties.STYLE, "font: 145% \$light.font")
                toAutoParseLabel.text = language.`to-auto-parse`
                toAutoParseLabel.putClientProperty(FlatClientProperties.STYLE, "font: 120% \$light.font")
            }
            MiniPasteArea(downloadDialog.rootPane, fillInfo)
            MiniPasteArea(smdpField, fillInfo)

            downloadDialog.isVisible = true
        }

        fun editNickname(oldNickname : String?, iccid : String)
        {
            val result = JOptionPane.showInputDialog(
                mainFrame,
                language.`edit-nickname-message`,
                language.`edit-nickname`,
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                oldNickname
            ) as String?
            if (result != null && result != (oldNickname ?: "")) LocalProfileAssistant.setProfileNickname(iccid, result)
        }

        fun toNotification(iccid : String)
        {
            mainFrame.tab.selectedIndex = 1
            val searchBox = NotificationToolBar.instance.searchBox
            searchBox.matchCase.isSelected = false
            searchBox.wholeWords.isSelected = true
            searchBox.regularExpression.isSelected = false
            searchBox.text = iccid
        }

        fun delete(iccid : String)
        {
            val inputPanel = JPanel()
            inputPanel.layout = MigLayout()
            val processDeleteNotificationCheckbox = JCheckBox(language.`process-delete-notification`, setting.`notification-behavior`.delete.process)
            val andRemoveCheckbox = JCheckBox(language.`and-remove`, setting.`notification-behavior`.delete.remove)
            processDeleteNotificationCheckbox.enableWhenChecked(andRemoveCheckbox)
            inputPanel.add(JLabel(language.`profile-delete-message`), "wrap")
            inputPanel.add(processDeleteNotificationCheckbox, "split 2, span")
            inputPanel.add(andRemoveCheckbox)
            if (showDangerConfirmDialog(mainFrame, inputPanel, language.`profile-delete`)) LocalProfileAssistant.deleteProfile(iccid, processDeleteNotificationCheckbox.isSelected, andRemoveCheckbox.isSelected)
        }
    }

    object Notification
    {
        fun toProfile(iccid : String)
        {
            mainFrame.tab.selectedIndex = 0
            val searchBox = ProfileToolBar.instance.searchBox
            searchBox.matchCase.isSelected = false
            searchBox.wholeWords.isSelected = true
            searchBox.regularExpression.isSelected = false
            searchBox.text = iccid
        }

        fun remove(vararg seq : Int)
        {
            if (showDangerConfirmDialog(mainFrame, language.`notification-remove-message`, language.`notification-remove`))
            {
                LocalProfileAssistant.removeNotification(*seq)
                NotificationList.instance.lastSelected = null
                NotificationList.instance.selectionMode = false
                NotificationPanel.instance.bottomInfo.selectedInfo.text = " "
            }
        }

        fun batchSelect()
        {
            val selectPanel = MiniPanel()
            selectPanel.layout = MigLayout("wrap 1")
            val install = JCheckBox(language.install)
            val enable = JCheckBox(language.enable)
            val disable = JCheckBox(language.disable)
            val delete = JCheckBox(language.delete)
            selectPanel.add(install)
            selectPanel.add(enable)
            selectPanel.add(disable)
            selectPanel.add(delete)
            val result = JOptionPane.showConfirmDialog(
                mainFrame,
                selectPanel,
                language.`batch-select`,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE)
            if (result == JOptionPane.OK_OPTION)
            {
                NotificationList.instance.cards.forEach {
                    when(it.notification.operation)
                    {
                        INSTALL -> it.selectionCheckbox.isSelected = install.isSelected
                        ENABLE -> it.selectionCheckbox.isSelected = enable.isSelected
                        DISABLE -> it.selectionCheckbox.isSelected = disable.isSelected
                        DELETE -> it.selectionCheckbox.isSelected = delete.isSelected
                    }
                }
                val count = NotificationList.instance.selectedNotifications.count()
                if (count == 0)
                {
                    NotificationList.instance.selectionMode = false
                    NotificationPanel.instance.bottomInfo.selectedInfo.text = " "
                } else NotificationPanel.instance.bottomInfo.selectedInfo.text = language.selected.format(count)
            }
        }
    }

    object Chip
    {
        fun viewCertificateIssuers(euiccInfo2 : JsonObject)
        {
            val certificateIssuersPanel = JPanel()
            val wrapLayout = WrapLayout(FlowLayout.CENTER)
            wrapLayout.vgap = 10
            wrapLayout.alignOnBaseline = true
            certificateIssuersPanel.layout = wrapLayout
            CIManifest.findCIs(euiccInfo2).forEach { certificateIssuersPanel.add(CertificateIssuerCard(it)) }
            val certificateIssuersDialog = JOptionPane(JScrollPane(certificateIssuersPanel).setup(), JOptionPane.PLAIN_MESSAGE).createDialog(mainFrame, language.`certificate-issuers`)
            certificateIssuersDialog.size = Dimension(600, 500)
            certificateIssuersDialog.setLocationRelativeTo(mainFrame)
            certificateIssuersDialog.isVisible = true
        }
    }
}