plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.admobnextgensdk"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.admobnextgensdk"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            resValue("string", "admob_app_id", "ca-app-pub-3940256099942544~3347511713")

            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        release {
            resValue("string", "admob_app_id", "ca-app-pub-3940256099942544~3347511713")

            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    bundle {
        language {
            enableSplit = false
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Import the Firebase BoM
    implementation(platform(libs.firebase.bom))
    // Firebase Dependencies
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.config)
    
//    koin boom
    implementation(platform(libs.koin.bom))
    // Core Android features
    implementation(libs.insert.koin.koin.android)
    // Kotlin extensions (KTX) for coroutines support
    implementation(libs.billing.ktx)
//    lottie
    implementation(libs.lottie)
    // AdMob Next Gen SDK
    implementation(libs.ads.mobile.sdk.v0220beta04)

    // Navigation Component
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Shimmer (for native ad loading skeleton)
    implementation(libs.shimmer)

    // User Messaging Platform (UMP) for consent
    implementation(libs.user.messaging.platform)

    //    include modules for ads...
    implementation(project(":sdkAdsNextGen"))
    implementation(project(":core"))
}