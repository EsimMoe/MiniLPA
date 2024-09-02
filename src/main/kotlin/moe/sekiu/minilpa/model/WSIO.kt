package moe.sekiu.minilpa.model

import kotlin.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class WSIO(val type : Type, val data : JsonElement, @Serializable(UuidSerializer::class) val id : Uuid = Uuid.random())
{
    enum class Type
    {
        @SerialName("pair")
        PAIR,
        @SerialName("execute")
        EXECUTE,
        @SerialName("error")
        ERROR
    }
}