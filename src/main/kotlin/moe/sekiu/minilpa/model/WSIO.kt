package moe.sekiu.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.uuid.SecureRandom
import kotlinx.uuid.UUID
import kotlinx.uuid.generateUUID

@Serializable
data class WSIO(val type : Type, val data : JsonElement, val id : UUID = UUID.generateUUID(SecureRandom))
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