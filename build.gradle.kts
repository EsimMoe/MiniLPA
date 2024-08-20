
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import java.io.InputStream
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.io.path.pathString
import kotlin.io.path.readAttributes
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.commons.lang3.ArchUtils
import org.apache.commons.lang3.SystemUtils
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.kohsuke.github.GHRelease
import org.kohsuke.github.GitHub
import org.panteleyev.jpackage.ImageType

plugins {
    application
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.serialization") version "1.9.23"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.ktor.plugin") version "2.3.9"
    id("org.panteleyev.jpackageplugin") version "1.6.0"
    id("com.github.gmazzo.buildconfig") version "5.3.5"
}

group = "moe.sekiu"
version = "1.1.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.formdev:flatlaf:3.4.1")
    implementation("com.formdev:flatlaf-extras:3.4.1")
    implementation("com.formdev:flatlaf-intellij-themes:3.4.1")
    implementation("com.miglayout:miglayout-swing:11.3")
    implementation("com.charleskorn.kaml:kaml:0.57.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-cio")
    implementation("io.ktor:ktor-client-websockets")
    implementation("app.softwork:kotlinx-uuid-core:0.0.25")
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.zxing:javase:3.5.3")
    implementation("org.apache.commons:commons-lang3:3.14.0")
    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("com.github.Dansoftowner:jSystemThemeDetector:3.9.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("org.kohsuke:github-api:1.321")
        classpath("org.apache.commons:commons-lang3:3.14.0")
        classpath("io.ktor:ktor-client-core:2.3.9")
        classpath("io.ktor:ktor-client-cio:2.3.9")
    }
}

buildConfig {
    packageName("moe.sekiu.minilpa")
    buildConfigField("IS_PACKAGED",
        (gradle.startParameter.taskNames.firstOrNull() == "jpackage" || project.switchProperty("is-package"))
                && project.findProperty("type") != "app-image"
    )
    buildConfigField("VERSION", "$version")
    buildConfigField("SHORT_COMMIT_ID", project.findProperty("short-commit-id") as String? ?: "internal")
}

application {
    mainClass.set("moe.sekiu.minilpa.MainKt")
}

tasks.named<KotlinCompilationTask<*>>("compileKotlin").configure {
    compilerOptions.freeCompilerArgs.addAll(
        "-opt-in=kotlin.io.encoding.ExperimentalEncodingApi",
        "-opt-in=kotlinx.coroutines.DelicateCoroutinesApi",
        "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
    )
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

val lpacPlatformToArtifact = mapOf(
    "macos_universal" to "lpac-darwin-universal",
    "windows_x86" to "lpac-windows-x86_64-mingw",
    "windows_aarch64" to "lpac-windows-arm64-mingw",
    "linux_x86" to "lpac-linux-x86_64"
)

val buildPath = layout.buildDirectory.asFile.get().toPath()
val jpackagePath = buildPath.resolve("jpackage")
val archiveFileName = tasks.shadowJar.get().archiveFileName.get()
val target = project.findProperty("target")?.toString()?.lowercase() ?: getPlatformInfo()
val githubToken = project.findProperty("github-token")?.toString()

val latestRelease : GHRelease by lazy {
    val gitHub = if (githubToken != null) GitHub.connectUsingOAuth(githubToken) else GitHub.connectAnonymously()
    val repository = gitHub.getRepository("estkme-group/lpac")
    repository.latestRelease
}

val listAssets by lazy { latestRelease.listAssets().toList() }

tasks.generateBuildConfig { dependsOn("setupResources") }

task("setupResources")
{
    group = "Package"
    doLast {
        val skip = project.switchProperty("skip-setup-resources")
        val lpacBuildTimePath = buildPath.resolve("lpac_build_time")
        var lpacBuildTime = if (lpacBuildTimePath.notExists()) 0
        else Files.readString(lpacBuildTimePath).toLong()
        val languagePackPath = buildPath.resolve("languages.zip")
        val eUICCInfoUpdateTimePath = buildPath.resolve("euicc_info_update_time")

        if (!skip)
        {
            val lpacLatestBuildTime = latestRelease.createdAt.time
            if (target == "all" ||
                buildPath.resolve("lpac").notExists() ||
                buildPath.resolve("lpac").resolve(target).notExists() ||
                lpacLatestBuildTime > lpacBuildTime)
            {
                if (target == "all")
                {
                    runBlocking { lpacPlatformToArtifact.keys.map { async { downloadlpac(it) } }.awaitAll() }
                }
                else
                {
                    lpacPlatformToArtifact[target] ?: throw IllegalArgumentException("Unknown target $target\nValid values are: [${lpacPlatformToArtifact.keys.joinToString(", ")}]")
                    runBlocking { downloadlpac(target) }
                }
                lpacBuildTimePath.writeText("$lpacLatestBuildTime")
                lpacBuildTime = lpacLatestBuildTime
            }

            FileSystems.newFileSystem(languagePackPath, mapOf("create" to true)).use { fs ->
                val languages = projectDir.toPath().resolve("src/main/languages/")
                for (path in Files.list(languages)) Files.copy(path, fs.getPath(path.name), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
            }
        }

        buildConfig {
            buildConfigField("LPAC_BUILD_TIME", lpacBuildTime)
            buildConfigField("LANGUAGE_PACK_UPDATE_TIME", languagePackPath.readAttributes<BasicFileAttributes>().lastModifiedTime().toMillis())
            buildConfigField("EUICC_INFO_UPDATE_TIME", eUICCInfoUpdateTimePath.readText().toLong())
        }
    }
}

task("setupjpackage")
{
    group = "Package"
    dependsOn("shadowJar")
    doLast {
        Files.createDirectories(jpackagePath)
        Files.copy(
            buildPath.resolve("libs/${archiveFileName}"),
            jpackagePath.resolve(archiveFileName),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.COPY_ATTRIBUTES
        )
    }
}

tasks.processResources {
    if (target == "all")
    {
        from(*lpacPlatformToArtifact.keys.map { buildPath.resolve("lpac").resolve("$it.zip") }.toTypedArray())
    }
    else
    {
        lpacPlatformToArtifact[target] ?: throw IllegalArgumentException("Unknown target $target\nValid values are: [${lpacPlatformToArtifact.keys.joinToString(", ")}]")
        from("${buildPath.resolve("lpac").resolve("$target.zip")}")
    }
    from(buildPath.resolve("languages.zip"))
}

tasks.jpackage {
    group = "Package"
    dependsOn("setupjpackage")
    input = jpackagePath.pathString
    destination = buildPath.resolve("dist").pathString
    appName = project.name
    vendor = "esim.moe"
    mainJar = archiveFileName
    mainClass = application.mainClass.get()
    javaOptions = mutableListOf("-Dfile.encoding=UTF-8")
    addModules = listOf("java.base", "java.desktop", "java.naming", "jdk.unsupported")

    if (project.hasProperty("type"))
    {
        val value = "${project.property("type")}"
        type = ImageType.values().find { it.value == value } ?: throw IllegalArgumentException("Type $value not found")
    }

    if (project.hasProperty("native-wayland")) javaOptions.add("Dawt.toolkit.name=WLToolkit")
    windows {
        icon = projectDir.toPath().resolve("src/main/icons/window.ico").pathString
        if (type != ImageType.APP_IMAGE)
        {
            winDirChooser = true
            winMenu = true
            winMenuGroup = "MiniLPA"
            winShortcut = true
            winShortcutPrompt = true
        }
    }

    linux {
        icon = projectDir.toPath().resolve("src/main/icons/window.png").pathString
        if (type != ImageType.APP_IMAGE)
        {
            linuxShortcut = true
            linuxMenuGroup = "MiniLPA"
            installDir = "/usr/"
        }
    }

    mac {
        icon = projectDir.toPath().resolve("src/main/icons/window.icns").pathString
        if (type != ImageType.APP_IMAGE)
        {
            macPackageIdentifier = "moe.sekiu.MiniLPA"
            macAppCategory = "MiniLPA"
        }
    }
}

fun getPlatformInfo() : String
{
    val os = if (SystemUtils.IS_OS_MAC_OSX) "macos"
    else if (SystemUtils.IS_OS_WINDOWS) "windows"
    else if (SystemUtils.IS_OS_LINUX) "linux"
    else throw UnsupportedOperationException("Unsupported os ${SystemUtils.OS_NAME}")

    val processor = ArchUtils.getProcessor()
    val arch = if (os == "macos") "universal"
    else if (processor.isX86) "x86"
    else if (processor.isAarch64) "aarch64"
    else throw UnsupportedOperationException("Unsupported arch ${processor.type.label}")
    return "${os}_${arch}"
}

val client = HttpClient()
suspend fun downloadlpac(platform : String)
{
    val asset = listAssets.first { it.name == "${lpacPlatformToArtifact[platform]}.zip" }
    val response = client.get(asset.browserDownloadUrl)
    if (!response.status.isSuccess()) throw IllegalStateException("Network error")
    withContext(Dispatchers.IO) {
        val lpacFolderPath = buildPath.resolve("lpac")
        Files.createDirectories(lpacFolderPath)
        Files.newOutputStream(lpacFolderPath.resolve("$platform.zip")).buffered()
            .use { out -> response.body<InputStream>().buffered().use { `in` -> `in`.copyTo(out) } }
    }
}

fun Project.switchProperty(propertyName : String) : Boolean
{
    val value = "${findProperty(propertyName)}"
    if (value.isBlank()) return true
    return value.toBoolean()
}
