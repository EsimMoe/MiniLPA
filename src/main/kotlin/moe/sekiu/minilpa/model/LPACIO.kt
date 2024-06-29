package moe.sekiu.minilpa.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import moe.sekiu.minilpa.exception.LPAOperationFailureException
import moe.sekiu.minilpa.json

@Serializable
private data class LPACOutDelegate(val type : LPACIO.Type, val payload : JsonElement)
{
    constructor(LPACIO : LPACIO) : this(LPACIO.type, JsonObject(json.encodeToJsonElement(LPACIO.payload).jsonObject.toMutableMap().apply { remove("type") }))
}

@Serializable(LPACOutSerializer::class)
data class LPACIO(val type : Type, val payload : Payload)
{
    enum class Type
    {
        @SerialName("lpa")
        LPA,
        @SerialName("progress")
        PROGRESS,
        @SerialName("driver")
        DRIVER,
        @SerialName("apdu")
        APDU
    }

    @Serializable
    sealed interface Payload
    {
        val lpa get() = this as LPA

        val driver get() = this as Driver

        val apdu get() = this as ApduOut

       sealed interface DataPayload : Payload
       {
           val data : JsonElement
       }

        @Serializable
        data class LPA(val code : Int, val message : String, override val data : JsonElement) : DataPayload
        {
            fun isSuccess() = code == 0

            fun assertSuccess() : LPA
            {
                if (!isSuccess()) throw LPAOperationFailureException(this)
                return this
            }
        }

        @Serializable
        data class Driver(val env : String, override val data : JsonElement) : DataPayload

        @Serializable
        data class ApduOut(val func : String, val param : String?) : Payload

        @Serializable
        data class ApduIn(val ecode : Int, val data : String = "") : Payload
    }
}

object LPACOutSerializer : KSerializer<LPACIO>
{
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("LPACIO")

    override fun deserialize(decoder : Decoder) : LPACIO
    {
        val delegate = LPACOutDelegate.serializer().deserialize(decoder)
        val deserializer = when (delegate.type)
        {
            LPACIO.Type.LPA, LPACIO.Type.PROGRESS -> LPACIO.Payload.LPA.serializer()
            LPACIO.Type.DRIVER -> LPACIO.Payload.Driver.serializer()
            LPACIO.Type.APDU -> LPACIO.Payload.ApduOut.serializer()
        }
        return LPACIO(delegate.type, json.decodeFromJsonElement(deserializer, delegate.payload))
    }

    override fun serialize(encoder : Encoder, value : LPACIO)
    {
        LPACOutDelegate.serializer().serialize(encoder, LPACOutDelegate(value))
    }
}