package moe.sekiu.minilpa.lpa

import moe.sekiu.minilpa.model.ChipInfo
import moe.sekiu.minilpa.model.Device
import moe.sekiu.minilpa.model.DownloadInfo
import moe.sekiu.minilpa.model.Notification
import moe.sekiu.minilpa.model.Profile

interface LPABackend<D : Device>
{
    suspend fun getChipInfo() : ChipInfo

    suspend fun getProfileList() : List<Profile>

    suspend fun downloadProfile(downloadInfo : DownloadInfo)

    suspend fun enableProfile(iccid : String)

    suspend fun disableProfile(iccid : String)

    suspend fun deleteProfile(iccid : String)

    suspend fun setProfileNickname(iccid : String, nickname : String)

    suspend fun getNotificationList() : List<Notification>

    suspend fun processNotification(vararg seq : Int, remove : Boolean = false)

    suspend fun removeNotification(vararg seq : Int)

    suspend fun setDefaultSMDPAddress(address : String)

    suspend fun getVersion() : String
}