plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.bettermarkdown"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.bettermarkdown"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    compileOnly(files("libs/plugins-api.jar"))
}
