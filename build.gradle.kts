import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("dev.mantikafasi.manticord.plugin")
}

val releaseSigningFile = rootProject.file("keystore.properties")
val releaseSigning = Properties().apply {
    if (releaseSigningFile.exists()) releaseSigningFile.inputStream().use(::load)
}

manticordPlugin {
    id.set("platformindicators")
    entrypoint.set("io.github.mmaarij123.aplatformindicators.PlatformIndicatorsPlugin")
    description.set("Shows active Discord client platforms in chat and in the profile badge row")
    manticordVersionMinimum.set(1)
    manticordVersionMaximumExclusive.set(2)
    usesResources.set(true)
    async()
}

android {
    namespace = "io.github.mmaarij123.aplatformindicators"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "io.github.mmaarij123.aplatformindicators"
        minSdk = 23
        targetSdk = 36
        versionCode = 3
        versionName = "1.1.1"
    }

    signingConfigs {
        if (releaseSigningFile.exists()) create("release") {
            storeFile = rootProject.file(releaseSigning.getProperty("storeFile"))
            storePassword = releaseSigning.getProperty("storePassword")
            keyAlias = releaseSigning.getProperty("keyAlias")
            keyPassword = releaseSigning.getProperty("keyPassword")
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    val localPluginApi = file("../manticordrebuild/build/plugin-sdk/manticord-plugin-api-1.0.6.jar")
    compileOnly(
        if (localPluginApi.exists()) files(localPluginApi)
        else "dev.mantikafasi.manticord:plugin-api:1.0.6",
    )
    compileOnly(kotlin("stdlib"))
    compileOnly(libs.rxjava)
}
