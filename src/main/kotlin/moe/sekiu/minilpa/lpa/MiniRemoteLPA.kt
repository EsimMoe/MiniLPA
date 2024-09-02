package moe.sekiu.minilpa.lpa

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlin.uuid.Uuid
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import moe.sekiu.minilpa.decode
import moe.sekiu.minilpa.drop
import moe.sekiu.minilpa.json
import moe.sekiu.minilpa.model.ChipInfo
import moe.sekiu.minilpa.model.DownloadInfo
import moe.sekiu.minilpa.model.LPACIO
import moe.sekiu.minilpa.model.Notification
import moe.sekiu.minilpa.model.Profile
import moe.sekiu.minilpa.model.RemoteCard
import moe.sekiu.minilpa.receive
import moe.sekiu.minilpa.send
import moe.sekiu.minilpa.model.WSIO
import moe.sekiu.minilpa.model.WSIO.Type.ERROR
import moe.sekiu.minilpa.model.WSIO.Type.EXECUTE
import moe.sekiu.minilpa.model.WSIO.Type.PAIR

suspend fun main()
{
    val remoteLPA = MiniRemoteLPA()
    remoteLPA.setup()
}

class MiniRemoteLPA : LPABackend<RemoteCard>
{
    lateinit var wsSession : WebSocketSession
    val waiting = mutableMapOf<Uuid, CompletableDeferred<LPACIO>>()

    suspend fun setup()
    {
        wsSession = HttpClient { install(WebSockets) }.webSocketSession(host = "127.0.0.1", port = 8080, path = "/MiniLPA/")
        wsSession.outgoing.send(WSIO(PAIR, JsonPrimitive(10)))
        CoroutineScope(Dispatchers.IO).launch {
            while (true)
            {
                val wsio = wsSession.incoming.receive<WSIO>() ?: continue
                when(wsio.type)
                {
                    PAIR -> {
                        println(wsio) }
                    EXECUTE -> {
                        val lpacio = json.decodeFromJsonElement<LPACIO>(wsio.data)
                        waiting[wsio.id]?.complete(lpacio)
                    }
                    ERROR -> TODO()
                }

            }
        }
        println(getChipInfo())

    }

    suspend fun execute(vararg commands : String) = with(WSIO(EXECUTE, JsonArray(commands.map { JsonPrimitive(it) })))
    {
        wsSession.outgoing.send(this)
        this.id
    }

    suspend fun waitingResult(id : Uuid) = with(CompletableDeferred<LPACIO>())
    {
        waiting[id] = this
        this.await().payload.lpa.assertSuccess()
    }

    override suspend fun getChipInfo() : ChipInfo = decode(waitingResult(execute("chip", "info")).data)

    override suspend fun getProfileList() : List<Profile> = decode(waitingResult(execute("profile", "list")).data)

    override suspend fun downloadProfile(downloadInfo : DownloadInfo) = waitingResult(execute(*downloadInfo.toCommand())).drop()

    override suspend fun enableProfile(iccid : String) = waitingResult(execute("profile", "enable", iccid)).drop()

    override suspend fun disableProfile(iccid : String) = waitingResult(execute("profile", "disable", iccid)).drop()

    override suspend fun deleteProfile(iccid : String) = waitingResult(execute("profile", "delete", iccid)).drop()

    override suspend fun setProfileNickname(iccid : String, nickname : String) = waitingResult(execute("profile", "nickname", iccid, nickname)).drop()

    override suspend fun getNotificationList() : List<Notification> = decode(waitingResult(execute("notification", "list")).data)

    override suspend fun processNotification(vararg seq : Int, remove : Boolean)
    {
        val commands = mutableListOf("notification", "process")
        if (remove) commands.add("-r")
        commands.addAll(seq.map { "$it" })
        waitingResult(execute(*commands.toTypedArray())).drop()
    }

    override suspend fun removeNotification(vararg seq : Int) = waitingResult(execute("notification", "remove", *seq.map { "$it" }.toTypedArray())).drop()

    override suspend fun setDefaultSMDPAddress(address : String) = waitingResult(execute("chip", "defaultsmdp", address)).drop()

    override suspend fun getVersion() : String = decode(waitingResult(execute("version")).data)
}