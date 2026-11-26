plugins {
    id("com.android.application")
}

android {
    namespace = "dev.veyra.xposed"

    // Android 17 uses the minor-versioned SDK platform package android-37.0.
    // Keep targetSdk at 36 so this module does not opt into Android 17 runtime behavior.
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "dev.veyra.xposed"
        minSdk = 26
        targetSdk = 36
        versionCode = 101
        versionName = "1.0.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            // A runner debug key allows personal test installs without storing a private key in Git.
            // Use GitHub Secrets for a stable signing key before distributing updates.
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*"
            )
        }
    }
}


dependencies {
    // The Xposed runtime supplies the API classes in the target process.
    compileOnly("io.github.libxposed:api:102.0.0")

    // Central currently publishes the matching service artifact as 102.0.0.
    implementation("io.github.libxposed:service:102.0.0")

    implementation("androidx.dynamicanimation:dynamicanimation:1.0.0")
}
