package moe.sekiu.minilpa.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class Profile(
    val iccid : String,
    val isdpAid : String,
    @SerialName("profileState")
    val state : State,
    @SerialName("profileNickname")
    val nickname : String?,
    val serviceProviderName : String?,
    @SerialName("profileName")
    val name : String?,
    val iconType : IconType?,
    val icon : String?,
    @SerialName("profileClass")
    val `class` : Class
)
{
    @Serializable
    enum class State
    {
        @SerialName("enabled")
        ENABLED,
        @SerialName("disabled")
        DISABLED
    }

    @Serializable
    enum class IconType
    {
        @SerialName("jpg")
        @JsonNames("jpeg")
        JPG,
        @SerialName("png")
        PNG
    }

    @Serializable
    enum class Class
    {
        @SerialName("test")
        TEST,
        @SerialName("provisioning")
        PROVISIONING,
        @SerialName("operational")
        OPERATIONAL
    }
}