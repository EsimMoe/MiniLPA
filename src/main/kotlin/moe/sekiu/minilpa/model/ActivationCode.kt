package moe.sekiu.minilpa.model

data class ActivationCode(
    val SMDP : String,
    val MatchingID : String,
    val OID : String?,
    val ReqConCode : Boolean
)
{
    companion object
    {
        private val ACTIVATION_CODE_REGEX = Regex("LPA:1\\\$(?<SMDP>[^\$]*)\\\$(?<MatchingID>[^\$]*)(?:\\\$(?<OID>[^\$]*))?(?:\\\$(?<ReqConCode>1))?")

        fun of(ac : String) : ActivationCode?
        {
            val matchResult = ACTIVATION_CODE_REGEX.matchEntire(ac) ?: return null
            return ActivationCode(
                matchResult.groups["SMDP"]!!.value,
                matchResult.groups["MatchingID"]!!.value,
                matchResult.groups["OID"]?.value,
                matchResult.groups["ReqConCode"]?.value == "1"
            )
        }
    }
}