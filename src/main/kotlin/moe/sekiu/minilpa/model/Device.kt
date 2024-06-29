package moe.sekiu.minilpa.model

import kotlinx.serialization.Serializable

sealed interface Device

@Serializable
data class Driver(val env : String, val name : String) : Device
{
    override fun toString() : String = name
}

data class RemoteCard(val host : String, val port : Int) : Device
{
    override fun toString() : String = "$host:$port"
}