plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

/*
 * Stable release signing is supplied by the release workflow from protected
 * GitHub Actions secrets. Never commit a keystore or its passwords.
 */
val signingStorePath = System.getenv("PHONEAGENT_SIGNING_STORE_PATH")
val signingStorePassword = System.getenv("PHONEAGENT_SIGNING_STORE_PASSWORD")
val signingKeyAlias = System.getenv("PHONEAGENT_SIGNING_KEY_ALIAS")
val signingKeyPassword = System.getenv("PHONEAGENT_SIGNING_KEY_PASSWORD")
val stableReleaseSigningConfigured =
    !signingStorePath.isNullOrBlank() &&
    !signingStorePassword.isNullOrBlank() &&
    !signingKeyAlias.isNullOrBlank() &&
    !signingKeyPassword.isNullOrBlank()

android {
    namespace = "io.phoneagent.mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.phoneagent.mobile"
        minSdk = 26
        targetSdk = 36
        versionCode = System.getenv("PHONEAGENT_VERSION_CODE")?.toIntOrNull() ?: 2
        versionName = System.getenv("PHONEAGENT_VERSION_NAME")
            ?.takeIf { it.isNotBlank() } ?: "0.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (stableReleaseSigningConfigured) {
            create("phoneAgentRelease") {
                storeFile = file(signingStorePath!!)
                storeType = "JKS"
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            // CI-only identity. This variant is not uploaded as an installable artifact.
            applicationIdSuffix = ".ci"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            if (stableReleaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("phoneAgentRelease")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.12.00"))
    implementation("androidx.activity:activity-compose:1.12.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
