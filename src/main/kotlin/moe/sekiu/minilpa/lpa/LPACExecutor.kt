package moe.sekiu.minilpa.lpa

import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import java.io.File
import kotlinx.serialization.json.jsonPrimitive
import moe.sekiu.minilpa.appDataFolder
import moe.sekiu.minilpa.cast
import moe.sekiu.minilpa.decode
import moe.sekiu.minilpa.drop
import moe.sekiu.minilpa.exception.OperationFailureException
import moe.sekiu.minilpa.json
import moe.sekiu.minilpa.language
import moe.sekiu.minilpa.logger
import moe.sekiu.minilpa.mainFrame
import moe.sekiu.minilpa.model.ChipInfo
import moe.sekiu.minilpa.model.DownloadInfo
import moe.sekiu.minilpa.model.Driver
import moe.sekiu.minilpa.model.LPACIO
import moe.sekiu.minilpa.model.Notification
import moe.sekiu.minilpa.model.Profile
import moe.sekiu.minilpa.platform
import moe.sekiu.minilpa.setting
import org.apache.commons.lang3.SystemUtils

class LPACExecutor() : LPABackend<Driver>
{
    private val log = logger()

    override var selectedDevice : Driver? = null

    suspend fun getDeviceList() : List<Driver> = decode(execute("driver", "apdu", "list").data)

    override suspend fun getChipInfo() : ChipInfo = decode(execute("chip", "info").data)

    override suspend fun getProfileList() : List<Profile> = decode(execute("profile", "list").data)

    override suspend fun downloadProfile(downloadInfo : DownloadInfo) = execute(*downloadInfo.toCommand()).drop()

    override suspend fun enableProfile(iccid : String) = execute("profile", "enable", iccid).drop()

    override suspend fun disableProfile(iccid : String) = execute("profile", "disable", iccid).drop()

    override suspend fun deleteProfile(iccid : String) = execute("profile", "delete", iccid).drop()

    override suspend fun setProfileNickname(iccid : String, nickname : String) = execute(
        "profile",
        "nickname",
        iccid,
        nickname
    ).drop()

    override suspend fun getNotificationList() : List<Notification> = decode(execute("notification", "list").data)

    override suspend fun processNotification(vararg seq : Int, remove : Boolean)
    {
        val commands = mutableListOf("notification", "process")
        if (remove) commands.add("-r")
        commands.addAll(seq.map { "$it" })
        execute(*commands.toTypedArray())
    }

    override suspend fun removeNotification(vararg seq : Int) = execute(
        "notification",
        "remove",
        *seq.map { "$it" }.toTypedArray()
    ).drop()

    override suspend fun setDefaultSMDPAddress(address : String) = execute("chip", "defaultsmdp", address).drop()

    override suspend fun getVersion() : String = decode(execute("version").data)

    suspend fun execute(vararg commands : String) : LPACIO.Payload.DataPayload
    {
        log.info("lpac command input -> ${commands.joinToString(" ")}")
        val lpacFile = File(File(appDataFolder, platform), if (SystemUtils.IS_OS_WINDOWS) "lpac.exe" else "lpac")
        if (!lpacFile.exists() || lpacFile.isDirectory) throw OperationFailureException(language.`lpac-not-found-or-invalid`.format(lpacFile.canonicalPath))
        var initialze = true
        val env = mutableMapOf<String, String>()
        if (setting.debug.libeuicc.apdu) env["LIBEUICC_DEBUG_APDU"] = "true"
        if (setting.debug.libeuicc.http) env["LIBEUICC_DEBUG_HTTP"] = "true"
        selectedDevice?.run { env["DRIVER_IFID"] = cast<Driver>().env }
        var lpacout : LPACIO? = null
        process(
            *(arrayOf(lpacFile.canonicalPath) + commands),
            env = env,
            stdout = Redirect.Capture {
                if (it.isBlank()) return@Capture
                log.info("lpac stdout output -> $it")
                val lpacio = json.decodeFromString<LPACIO>(it)
                when (lpacio.type)
                {
                    LPACIO.Type.PROGRESS ->
                    {
                        val message = lpacio.payload.lpa.message
                        if (message in listOf("es10b_retrieve_notifications_list", "es9p_handle_notification"))
                        {
                            mainFrame.progressInfo.text = "#${lpacio.payload.lpa.data.jsonPrimitive.content} $message"
                        } else mainFrame.progressInfo.text = message
                        if (!initialze && !mainFrame.progressBar.freeze) mainFrame.progressBar.swipePlusAuto()
                        initialze = false
                    }
                    LPACIO.Type.LPA ->
                    {
                        if (!initialze) mainFrame.progressBar.swipePlusAuto()
                        lpacout = lpacio
                    }
                    LPACIO.Type.DRIVER ->
                    {
                        mainFrame.progressBar.value = mainFrame.progressBar.maximum
                        lpacout = lpacio
                    }
                    else -> throw UnsupportedOperationException()
                }
            },
            stderr = Redirect.Capture {
                if (it.isBlank()) return@Capture
                log.info("lpac stderr output -> $it")
            }
        )
        val payload = lpacout?.payload ?: throw OperationFailureException(language.`lpac-output-not-captured`)
        if (payload is LPACIO.Payload.LPA) payload.assertSuccess()
        return payload.cast()
    }

}