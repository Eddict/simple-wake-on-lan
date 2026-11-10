import java.util.Properties

plugins {
    id("com.android.application")
    // The Glance widget is Compose code, so the Compose compiler runs for
    // the app module too.
    id("org.jetbrains.kotlin.plugin.compose")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

// Release signing material, kept out of the repository. Absent on a plain
// clone, in which case the release build falls back to the debug keys below.
val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("key.properties")
val hasReleaseKeystore = keystorePropertiesFile.exists()
if (hasReleaseKeystore) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }

    // Fail here, with names, rather than deep inside the signing task with a
    // cryptic null when one of them was forgotten.
    val missing = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
        .filter { keystoreProperties.getProperty(it).isNullOrBlank() }
    check(missing.isEmpty()) { "key.properties is missing: ${missing.joinToString()}" }
}

val appId = providers.gradleProperty("android.applicationId").get()

android {
    namespace = appId
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = appId
        // Wake-on-LAN needs no platform APIs newer than this, and 28 is what
        // the project has shipped against.
        minSdk = 28
        targetSdk = flutter.targetSdkVersion
        // Uses the version code from pubspec.yaml. When using split APKs, 1000 * ABI_VERSION
        // is added automatically by Flutter. (https://developer.android.com/studio/build/configure-apk-splits#configure-APK-versions)
        // You can force using the value of versionCode by specifying the `-P force-version-code-ignoring-abi=true`
        // flag during build.
        versionCode = flutter.versionCode
        versionName = flutter.versionName

        // Target a Single Specific ABI
        ndk {
            // Clear Flutter's automatically applied filters
            abiFilters.clear() 
            // Explicitly add only the ABIs to compile
            abiFilters.addAll(listOf("arm64-v8a")) 
        }
    }

    if (hasReleaseKeystore) {
        signingConfigs {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                // So `flutter run --release` still works without the keystore.
                signingConfigs.getByName("debug")
            }

            // https://github.com/flutter/flutter/issues/29509#issuecomment-648044290
            applicationVariants.all { variant ->
                variant.outputs.all {
                    def appName = "nl.eddict.simplewol_"
                    def buildType = variant.variantData.variantConfiguration.buildType.name
                    def newName
                    if (buildType == 'debug'){
                        newName = "app-${variant.getFlavorName()}-debug.apk"
                    } else {
                        newName = "${appName}${defaultConfig.versionName}_${variant.getFlavorName()}.apk"
                    }
                    outputFileName = newName
                }
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

flutter {
    source = "../.."
}

dependencies {
    implementation("androidx.glance:glance-appwidget:1.2.0")
    implementation("androidx.glance:glance-material3:1.2.0")
    // glance-material3 wraps M3 color schemes but does not re-export the
    // types the fallback palette is built from.
    implementation("androidx.compose.material3:material3:1.4.0")
}
