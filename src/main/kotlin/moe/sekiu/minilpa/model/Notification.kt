package moe.sekiu.minilpa.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    @SerialName("seqNumber")
    val seq : Int,
    @SerialName("profileManagementOperation")
    val operation : Operation,
    @SerialName("notificationAddress")
    val address : String,
    val iccid : String?
)
{
    @Serializable
    enum class Operation
    {
        @SerialName("install")
        INSTALL,
        @SerialName("enable")
        ENABLE,
        @SerialName("disable")
        DISABLE,
        @SerialName("delete")
        DELETE
    }
}