package moe.sekiu.minilpa.model

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import java.io.File
import java.io.InputStream
import java.nio.file.FileSystems
import kotlin.io.path.Path
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.decodeFromStream
import moe.sekiu.minilpa.BuildConfig
import moe.sekiu.minilpa.appDataFolder
import moe.sekiu.minilpa.bufferedResourceStream
import moe.sekiu.minilpa.cast
import moe.sekiu.minilpa.json
import moe.sekiu.minilpa.language
import moe.sekiu.minilpa.openLink
import moe.sekiu.minilpa.setting

sealed class Manifest<T : Any>
{
    abstract val path : String
    abstract val updateURL : String
    val data : MutableList<T> = mutableListOf()

    internal inline fun <reified T> loadManifest0() : Boolean
    {
        var update = false
        val file = File(appDataFolder, path)
        if (!file.exists() || file.length() == 0L || BuildConfig.EUICC_INFO_UPDATE_TIME > setting.`euicc-info-update-time`)
        {
            file.outputStream().buffered().use { out -> bufferedResourceStream(path).use { `in` -> `in`.copyTo(out) } }
            update = true
        }
        val list = file.inputStream().buffered().use { `in` -> json.decodeFromStream<List<T>>(`in`) }
        data.clear()
        data.addAll(list.cast())
        return update
    }

    abstract fun loadManifest() : Boolean

    suspend fun updateManifest()
    {
        HttpClient().use { client ->
            val response = client.request(updateURL)
            if (response.status.isSuccess())
            {
                File(path).outputStream().buffered().use { out ->
                    response.body<InputStream>().copyTo(out)
                }
            }
        }
    }

    companion object
    {
        suspend fun updateManifests()
        {
            EumManifest.updateManifest()
            CIManifest.updateManifest()
            setting.update { `euicc-info-update-time` = System.currentTimeMillis() }
        }

        fun loadManifests()
        {
            if (EumManifest.loadManifest() || CIManifest.loadManifest())
            {
                setting.update { `euicc-info-update-time` = BuildConfig.EUICC_INFO_UPDATE_TIME }
            }
        }
    }
}

data object EumManifest : Manifest<EumManifest.Manufacturer>()
{
    override val path = "eum-manifest.json"
    override val updateURL = "https://euicc-manual.osmocom.org/docs/pki/eum/manifest.json"
    val eums = data

    override fun loadManifest() = loadManifest0<Manufacturer>()

    @Serializable
    data class Manufacturer(
        val eum : String,
        val country : String,
        val manufacturer : String,
        val products : List<Product> = emptyList()
    )
    {
        @Serializable
        data class Product(
            val pattern : String,
            val name : String,
            val chip : String? = null
        )
    }

    fun findEum(eid : String) : Manufacturer?
    {
        for (eum in eums)
        {
            if (eid.startsWith(eum.eum)) return eum
        }
        return null
    }

    fun findProduct(eum : Manufacturer, eid : String) : Manufacturer.Product?
    {
        val path = Path(eid)
        for (product in eum.products)
        {
            val matcher = FileSystems.getDefault().getPathMatcher("glob:${product.pattern}")
            if (matcher.matches(path)) return product
        }
        return null
    }
}

data object CIManifest : Manifest<CIManifest.CertificateIssuer>()
{
    override val path = "ci-manifest.json"
    override val updateURL = "https://euicc-manual.osmocom.org/docs/pki/ci/manifest.json"
    val cis = data

    override fun loadManifest() = loadManifest0<CertificateIssuer>()

    @Serializable
    data class CertificateIssuer(
        @SerialName("key-id")
        val keyId : String,
        val country : String? = null,
        val name : String = language.unknown,
        val crls : List<String>? = null)
    {
        fun openExternalLink() = "https://euicc-manual.osmocom.org/docs/pki/ci/files/${keyId.substring(0..5)}.txt".openLink()
    }

    fun findCIs(euiccInfo2 : JsonObject) : List<CertificateIssuer>
    {
        val euiccInfo2Lite = json.decodeFromJsonElement<ChipInfo.EuiccInfo2Lite>(euiccInfo2)
        val intersection = euiccInfo2Lite.euiccCiPKIdListForSigning intersect euiccInfo2Lite.euiccCiPKIdListForVerification
        return intersection.map { cis.find { ci -> it.startsWith(ci.keyId) }?.copy(keyId = it) ?: CertificateIssuer(keyId = it) }
    }
}