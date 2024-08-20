package moe.sekiu.minilpa.lpa

import javax.swing.DefaultComboBoxModel
import javax.swing.JToggleButton
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import moe.sekiu.minilpa.backend
import moe.sekiu.minilpa.cast
import moe.sekiu.minilpa.language
import moe.sekiu.minilpa.mainFrame
import moe.sekiu.minilpa.model.Device
import moe.sekiu.minilpa.model.DownloadInfo
import moe.sekiu.minilpa.model.Notification
import moe.sekiu.minilpa.setting
import moe.sekiu.minilpa.ui.ChipPanel
import moe.sekiu.minilpa.ui.component.ChipInfo
import moe.sekiu.minilpa.ui.component.NotificationCard
import moe.sekiu.minilpa.ui.component.NotificationList
import moe.sekiu.minilpa.ui.component.ProfileList

object LocalProfileAssistant
{
    val devices = DefaultComboBoxModel<Device>()

    val showDetails = object : JToggleButton.ToggleButtonModel()
    {
        init { super.setSelected(setting.`show-details`) }

        override fun setSelected(select : Boolean)
        {
            super.setSelected(select)
            setting.update { `show-details` = select }
            ProfileList.instance.switchProfileIccidMask(select)
            NotificationList.instance.switchNotificationIccidMask(select)
            ChipInfo.instance?.switchChipInfoEidMask(select)
        }
    }

    fun refreshDeviceData()
    {
        GlobalScope.launch {
            devices.removeAllElements()
            mainFrame.freezeWithTimeout(requireDevice = false) {
                devices.addAll(backend.cast<LPACExecutor>().getDeviceList())
                val device = devices.getElementAt(0)
                devices.selectedItem = device
            }
        }
    }

    fun downloadProfile(downloadInfo : DownloadInfo, processNotification : Boolean, removeNotification : Boolean)
    {
        GlobalScope.launch { mainFrame.freezeWithTimeout(language.`operation-success-profile-download`) {
            var stage = 4 + 7
            if (processNotification)
            {
                stage += 2
            }
            mainFrame.progressBar.stage = stage
            mainFrame.progressInfo.text = language.`profile-download`
            backend.downloadProfile(downloadInfo)
            mainFrame.progressBar.swipePlusAuto()
            mainFrame.progressInfo.text = language.`profile-get-list`
            ProfileList.instance.refreshProfileData()
            mainFrame.progressBar.swipePlusAuto()
            mainFrame.progressInfo.text = language.`notification-get-list`
            NotificationList.instance.refreshNotificationData()
            mainFrame.progressBar.swipePlusAuto()
            if (processNotification) mainFrame.tryExecute {
                mainFrame.progressInfo.text = language.`notification-process`
                mainFrame.progressBar.freeze = true
                val seqs = mutableSetOf<Int>()
                var iccid : String? = null
                for (component in NotificationList.instance.components)
                {
                    val notification = (component as? NotificationCard ?: continue).notification
                    if (notification.operation != Notification.Operation.INSTALL) continue
                    if (iccid != null && notification.iccid != null && iccid != notification.iccid) continue
                    val lastSeq = seqs.lastOrNull()
                    if (lastSeq != null && lastSeq - 1 != notification.seq) break
                    seqs.add(notification.seq)
                    if (iccid == null) iccid = notification.iccid
                }
                backend.processNotification(*seqs.toIntArray(), remove = removeNotification)
                mainFrame.progressInfo.text = language.`notification-get-list`
                NotificationList.instance.refreshNotificationData()
                mainFrame.progressBar.swipePlusAuto()
            }
            mainFrame.progressInfo.text = language.`get-chip-info`
            ChipPanel.instance.refreshChipInfo()
            mainFrame.progressBar.swipePlusAuto()
        } }
    }

    private suspend fun processNotificationSingleStage(iccid : String, operation : Notification.Operation, remove : Boolean)
    {
        mainFrame.progressInfo.text = language.`notification-process`
        mainFrame.progressBar.freeze = true
        val seqs = mutableSetOf<Int>()
        for (component in NotificationList.instance.components)
        {
            val notification = (component as? NotificationCard ?: continue).notification
            if (notification.operation != operation) continue
            if (notification.iccid != null && notification.iccid != iccid) continue
            val lastSeq = seqs.lastOrNull()
            if (lastSeq != null && lastSeq - 1 != notification.seq) break
            seqs.add(notification.seq)
        }
        backend.processNotification(*seqs.toIntArray(), remove = remove)
        if (remove)
        {
            mainFrame.progressInfo.text = language.`notification-get-list`
            NotificationList.instance.refreshNotificationData()
            mainFrame.progressBar.swipePlusAuto()
        }
    }

    fun enableProfile(iccid : String)
    {
        GlobalScope.launch { mainFrame.freezeWithTimeout {
            var stage = 4
            if (setting.`notification-behavior`.enable.process)
            {
                stage += 1
                if (setting.`notification-behavior`.enable.remove) stage += 1
            }
            mainFrame.progressBar.stage = stage
            mainFrame.progressInfo.text = language.`profile-enable`
            backend.enableProfile(iccid)
            mainFrame.progressBar.swipePlusAuto()
            mainFrame.progressInfo.text = language.`profile-get-list`
            ProfileList.instance.refreshProfileData()
            mainFrame.progressBar.swipePlusAuto()
            mainFrame.progressInfo.text = language.`notification-get-list`
            NotificationList.instance.refreshNotificationData()
            mainFrame.progressBar.swipePlusAuto()
            if (setting.`notification-behavior`.enable.process) mainFrame.tryExecute {
                processNotificationSingleStage(iccid, Notification.Operation.ENABLE, setting.`notification-behavior`.enable.remove)
            }
            mainFrame.progressInfo.text = language.`get-chip-info`
            ChipPanel.instance.refreshChipInfo()
            mainFrame.progressBar.swipePlusAuto()
        } }
    }

    fun disableProfile(iccid : String)
    {
        GlobalScope.launch { mainFrame.freezeWithTimeout {
            var stage = 4
            if (setting.`notification-behavior`.disable.process)
            {
                stage += 1
                if (setting.`notification-behavior`.disable.remove) stage += 1
            }
            mainFrame.progressBar.stage = stage
            mainFrame.progressInfo.text = language.`profile-disable`
            backend.disableProfile(iccid)
            mainFrame.progressBar.swipePlusAuto()
            mainFrame.progressInfo.text = language.`profile-get-list`
            ProfileList.instance.refreshProfileData()
            mainFrame.progressBar.swipePlusAuto()
            mainFrame.progressInfo.text = language.`notification-get-list`
            NotificationList.instance.refreshNotificationData()
            mainFrame.progressBar.swipePlusAuto()
            if (setting.`notification-behavior`.enable.process) mainFrame.tryExecute {
                processNotificationSingleStage(iccid, Notification.Operation.DISABLE, setting.`notification-behavior`.disable.remove)
            }
            mainFrame.progressInfo.text = language.`get-chip-info`
            ChipPanel.instance.refreshChipInfo()
            mainFrame.progressBar.swipePlusAuto()
        } }
    }

    fun deleteProfile(iccid : String, processNotification : Boolean, removeNotification : Boolean)
    {
        GlobalScope.launch { mainFrame.freezeWithTimeout(language.`operation-success-profile-delete`) {
            var stage = 4
            if (processNotification)
            {
                stage += 1
                if (removeNotification) stage += 1
            }
            mainFrame.progressBar.stage = stage
            mainFrame.progressInfo.text = language.`profile-delete`
            backend.deleteProfile(iccid)
            mainFrame.progressBar.swipePlusAuto()
            mainFrame.progressInfo.text = language.`profile-get-list`
            ProfileList.instance.refreshProfileData()
            mainFrame.progressBar.swipePlusAuto()
            mainFrame.progressInfo.text = language.`notification-get-list`
            NotificationList.instance.refreshNotificationData()
            mainFrame.progressBar.swipePlusAuto()
            if (setting.`notification-behavior`.enable.process) mainFrame.tryExecute {
                processNotificationSingleStage(iccid, Notification.Operation.DELETE, removeNotification)
            }
            mainFrame.progressInfo.text = language.`get-chip-info`
            ChipPanel.instance.refreshChipInfo()
            mainFrame.progressBar.swipePlusAuto()
        } }
    }

    fun setProfileNickname(iccid : String, nickname : String)
    {
        GlobalScope.launch { mainFrame.freezeWithTimeout {
            mainFrame.progressBar.stage = 3
            mainFrame.progressInfo.text = language.`edit-nickname`
            backend.setProfileNickname(iccid, nickname)
            mainFrame.progressBar.swipePlusAuto()
            mainFrame.progressInfo.text = language.`profile-get-list`
            ProfileList.instance.refreshProfileData()
            NotificationList.instance.cards.filter { it.notification.iccid == iccid }.forEach { it.nicknameLabel.text = nickname }
            mainFrame.progressBar.swipePlusAuto()
            mainFrame.progressInfo.text = language.`get-chip-info`
            ChipPanel.instance.refreshChipInfo()
            mainFrame.progressBar.swipePlusAuto()
        } }
    }

    fun processNotification(vararg seq : Int)
    {
        GlobalScope.launch { mainFrame.freezeWithTimeout(language.`operation-success-notification-process`) {
            mainFrame.progressBar.stage = 2 * seq.count()
            mainFrame.progressInfo.text = language.`notification-process`
            backend.processNotification(*seq) }
        }
    }

    fun removeNotification(vararg seq : Int)
    {
        GlobalScope.launch { mainFrame.freezeWithTimeout(language.`operation-success-notification-remove`) {
            mainFrame.progressBar.stage = 2 * seq.count() + 1
            mainFrame.progressInfo.text = language.`notification-remove`
            backend.removeNotification(*seq)
            mainFrame.progressBar.swipePlusAuto()
            mainFrame.progressInfo.text = language.`notification-get-list`
            NotificationList.instance.refreshNotificationData()
            mainFrame.progressBar.swipePlusAuto()
            mainFrame.progressInfo.text = language.`get-chip-info`
            ChipPanel.instance.refreshChipInfo()
            mainFrame.progressBar.swipePlusAuto()
        } }
    }

    fun editDefaultSMDPAddress(address : String)
    {
        GlobalScope.launch { mainFrame.freezeWithTimeout {
            mainFrame.progressBar.stage = 2
            mainFrame.progressInfo.text = language.`edit-default-SMDP+-address`
            backend.setDefaultSMDPAddress(address)
            mainFrame.progressBar.swipePlusAuto()
            mainFrame.progressInfo.text = language.`get-chip-info`
            ChipPanel.instance.refreshChipInfo()
            mainFrame.progressBar.swipePlusAuto()
        } }
    }


    fun refreshCardData()
    {
        GlobalScope.launch { mainFrame.freezeWithTimeout {
            mainFrame.progressBar.stage = 3
            mainFrame.progressInfo.text = language.`profile-get-list`
            ProfileList.instance.refreshProfileData()
            mainFrame.progressBar.swipePlusAuto()
            mainFrame.progressInfo.text = language.`notification-get-list`
            NotificationList.instance.refreshNotificationData()
            mainFrame.progressBar.swipePlusAuto()
            mainFrame.progressInfo.text = language.`get-chip-info`
            ChipPanel.instance.refreshChipInfo()
            mainFrame.progressBar.swipePlusAuto()
        } }
    }
}