package moe.sekiu.minilpa.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ChipInfo(
    @SerialName("eidValue")
    val eid : String,
    @SerialName("EuiccConfiguredAddresses")
    val euiccConfiguredAddresses : EuiccConfiguredAddresses,
    @SerialName("EUICCInfo2")
    val eUICCInfo2 : JsonObject
)
{
    @Serializable
    data class EuiccConfiguredAddresses(
        val defaultDpAddress : String?,
        val rootDsAddress : String
    )

    @Serializable
    data class EuiccInfo2Lite(
        val extCardResource : ExtCardResource,
        val euiccCiPKIdListForVerification : Set<String>,
        val euiccCiPKIdListForSigning : Set<String>
    )
    {
        @Serializable
        data class ExtCardResource(val freeNonVolatileMemory : Int)
    }
}