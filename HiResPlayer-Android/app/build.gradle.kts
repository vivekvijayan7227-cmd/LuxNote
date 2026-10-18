import java.util.Properties
import java.io.FileInputStream

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.audioplayer.hires"
    compileSdk = 36
    ndkVersion = "26.1.10909125"

    defaultConfig {
        applicationId = "com.audioplayer.hires"
        minSdk = 24
        targetSdk = 36
        versionCode = 8
        versionName = "3.4.3"

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++20"
                arguments += "-DANDROID_STL=c++_shared"
            }
        }
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            val keystoreProperties = Properties()
            if (keystorePropertiesFile.exists()) {
                FileInputStream(keystorePropertiesFile).use { keystoreProperties.load(it) }
            }

            val keystoreRawPath = System.getenv("KEYSTORE_PATH")
                ?: project.findProperty("KEYSTORE_PATH") as? String
                ?: keystoreProperties.getProperty("KEYSTORE_PATH")
                ?: keystoreProperties.getProperty("keystore.path")

            val storePasswordStr = System.getenv("KEYSTORE_PASSWORD")
                ?: project.findProperty("KEYSTORE_PASSWORD") as? String
                ?: keystoreProperties.getProperty("KEYSTORE_PASSWORD")
                ?: keystoreProperties.getProperty("keystore.password")

            val keyAliasStr = System.getenv("KEY_ALIAS")
                ?: project.findProperty("KEY_ALIAS") as? String
                ?: keystoreProperties.getProperty("KEY_ALIAS")
                ?: keystoreProperties.getProperty("key.alias")

            val keyPasswordStr = System.getenv("KEY_PASSWORD")
                ?: project.findProperty("KEY_PASSWORD") as? String
                ?: keystoreProperties.getProperty("KEY_PASSWORD")
                ?: keystoreProperties.getProperty("key.password")

            if (!keystoreRawPath.isNullOrBlank()) {
                val candidateFile = if (file(keystoreRawPath).isAbsolute) {
                    file(keystoreRawPath)
                } else {
                    listOf(
                        rootProject.file(keystoreRawPath),
                        file(keystoreRawPath),
                        rootProject.rootDir.resolve(keystoreRawPath)
                    ).firstOrNull { it.exists() } ?: rootProject.file(keystoreRawPath)
                }
                if (candidateFile.exists() &&
                    !storePasswordStr.isNullOrBlank() &&
                    !keyAliasStr.isNullOrBlank() &&
                    !keyPasswordStr.isNullOrBlank()) {
                    storeFile = candidateFile
                    storePassword = storePasswordStr
                    keyAlias = keyAliasStr
                    keyPassword = keyPasswordStr
                }
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
        create("internalTest") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            isDebuggable = false
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }

    testOptions {
      unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation("androidx.compose.material:material-icons-extended")
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
}
