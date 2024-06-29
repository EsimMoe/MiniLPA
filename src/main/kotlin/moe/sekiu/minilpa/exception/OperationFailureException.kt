package moe.sekiu.minilpa.exception

import moe.sekiu.minilpa.model.LPACIO

open class OperationFailureException(override val message: String) : RuntimeException(message)

class LPAOperationFailureException(val lpa : LPACIO.Payload.LPA) : OperationFailureException(lpa.message)
{
    val data = lpa.data
}