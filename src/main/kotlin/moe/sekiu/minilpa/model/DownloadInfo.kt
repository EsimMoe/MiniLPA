package moe.sekiu.minilpa.model

data class DownloadInfo(
    val SMDP : String?,
    val matchingId : String?,
    val confirmCode : String?,
    val IMEI : String?
)
{
    fun toCommand() : Array<String>
    {
        val command = mutableListOf("profile", "download")
        if (!SMDP.isNullOrBlank())
        {
            command.add("-s")
            command.add(SMDP)
        }
        if (!matchingId.isNullOrBlank())
        {
            command.add("-m")
            command.add(matchingId)
        }
        if (!confirmCode.isNullOrBlank())
        {
            command.add("-c")
            command.add(confirmCode)
        }
        if (!IMEI.isNullOrBlank())
        {
            command.add("-i")
            command.add(IMEI)
        }
        return command.toTypedArray()
    }
}