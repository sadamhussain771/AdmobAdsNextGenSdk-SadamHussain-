plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.zoobiapps.admobpreloader"
    compileSdk = 36

    defaultConfig {
        minSdk = 29
    }

    buildTypes {
        debug {
            resValue("string", "admob_app_open_id", "ca-app-pub-3940256099942544/9257395921")

            resValue("string", "admob_banner_entrance_id", "ca-app-pub-3940256099942544/2014213617")
            resValue("string", "admob_banner_splash_id", "ca-app-pub-3940256099942544/2014213617")

//            resValue("string", "admob_banner_dashboard_id", "ca-app-pub-3940256099942544/2014213617")
//            resValue("string", "admob_banner_feature_one_a_id", "ca-app-pub-3940256099942544/2014213617")
//            resValue("string", "admob_banner_feature_one_b_id", "ca-app-pub-3940256099942544/2014213617")
//            resValue("string", "admob_banner_feature_two_a_id", "ca-app-pub-3940256099942544/2014213617")
//            resValue("string", "admob_banner_feature_two_b_id", "ca-app-pub-3940256099942544/2014213617")

            resValue("string", "admob_inter_entrance_id", "ca-app-pub-3940256099942544/1033173712")
            resValue(
                "string",
                "admob_inter_on_boarding_id",
                "ca-app-pub-3940256099942544/1033173712"
            )
            resValue("string", "admob_inter_dashboard_id", "ca-app-pub-3940256099942544/1033173712")
            resValue("string", "inter_bookmark_admob_id", "ca-app-pub-3940256099942544/1033173712")
            resValue("string", "inter_dpmaker_admob_id", "ca-app-pub-3940256099942544/1033173712")
            resValue("string", "inter_save_admob_id", "ca-app-pub-3940256099942544/1033173712")
            resValue("string", "inter_videos_admob_id", "ca-app-pub-3940256099942544/1033173712")

//            resValue("string", "admob_inter_bottom_navigation_id", "ca-app-pub-3940256099942544/1033173712")
//            resValue("string", "admob_inter_back_press_id", "ca-app-pub-3940256099942544/1033173712")
//            resValue("string", "admob_inter_exit_id", "ca-app-pub-3940256099942544/1033173712")

            resValue("string", "admob_native_language_id", "ca-app-pub-3940256099942544/2247696110")
            resValue(
                "string",
                "admob_native_language2_id",
                "ca-app-pub-3940256099942544/2247696110"
            )

            resValue(
                "string",
                "admob_native_on_boarding_id",
                "ca-app-pub-3940256099942544/2247696110"
            )
            resValue(
                "string",
                "admob_native_on_boarding2_id",
                "ca-app-pub-3940256099942544/2247696110"
            )
            resValue(
                "string",
                "admob_native_on_boarding3_id",
                "ca-app-pub-3940256099942544/2247696110"
            )

            resValue(
                "string",
                "admob_native_get_started_id",
                "ca-app-pub-3940256099942544/2247696110"
            )

            resValue("string", "admob_native_home_id", "ca-app-pub-3940256099942544/2247696110")
            resValue("string", "admob_native_feature_id", "ca-app-pub-3940256099942544/2247696110")
            resValue(
                "string",
                "admob_native_full_screen_id",
                "ca-app-pub-3940256099942544/2247696110"
            )
            resValue(
                "string",
                "native_full_screen2",
                "ca-app-pub-3940256099942544/2247696110"
            )

            // resValue("string", "admob_native_exit_id", "ca-app-pub-3940256099942544/2247696110")

            resValue("string", "admob_rewarded_id", "ca-app-pub-3940256099942544/5224354917")
            resValue("string", "downloader_rewarded_id", "ca-app-pub-3940256099942544/5224354917")

            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        release {
            resValue("string", "admob_app_open_id", "ca-app-pub-3940256099942544/9257395921")

            resValue("string", "admob_banner_entrance_id", "ca-app-pub-3940256099942544/2014213617")
            resValue("string", "admob_banner_splash_id", "ca-app-pub-3940256099942544/2014213617")


//            resValue("string", "admob_banner_on_boarding_id", "ca-app-pub-9498660037072845/2786780825")
//            resValue("string", "admob_banner_dashboard_id", "ca-app-pub-9498660037072845/2786780825")
//            resValue("string", "admob_banner_feature_one_a_id", "ca-app-pub-9498660037072845/2786780825")
//            resValue("string", "admob_banner_feature_one_b_id", "ca-app-pub-9498660037072845/2786780825")
//            resValue("string", "admob_banner_feature_two_a_id", "ca-app-pub-9498660037072845/2786780825")
//            resValue("string", "admob_banner_feature_two_b_id", "ca-app-pub-9498660037072845/2786780825")

            resValue("string", "admob_inter_entrance_id", "ca-app-pub-3940256099942544/1033173712")
            resValue(
                "string",
                "admob_inter_on_boarding_id",
                "ca-app-pub-3940256099942544/1033173712"
            )
            resValue("string", "admob_inter_dashboard_id", "ca-app-pub-3940256099942544/1033173712")
            resValue("string", "inter_bookmark_admob_id", "ca-app-pub-3940256099942544/1033173712")
            resValue("string", "inter_dpmaker_admob_id", "ca-app-pub-3940256099942544/1033173712")
            resValue("string", "inter_save_admob_id", "ca-app-pub-3940256099942544/1033173712")
            resValue("string", "inter_videos_admob_id", "ca-app-pub-3940256099942544/1033173712")


//            resValue("string", "admob_inter_bottom_navigation_id", "ca-app-pub-9498660037072845/3341314591")
//            resValue("string", "admob_inter_back_press_id", "ca-app-pub-9498660037072845/3341314591")
//            resValue("string", "admob_inter_exit_id", "ca-app-pub-9498660037072845/3341314591")

            resValue("string", "admob_native_language_id", "ca-app-pub-3940256099942544/2247696110")
            resValue(
                "string",
                "admob_native_language2_id",
                "ca-app-pub-3940256099942544/2247696110"
            )

            resValue(
                "string",
                "admob_native_on_boarding_id",
                "ca-app-pub-3940256099942544/2247696110"
            )
            resValue(
                "string",
                "admob_native_on_boarding2_id",
                "ca-app-pub-3940256099942544/2247696110"
            )
            resValue(
                "string",
                "admob_native_on_boarding3_id",
                "ca-app-pub-3940256099942544/2247696110"
            )

            resValue(
                "string",
                "admob_native_get_started_id",
                "ca-app-pub-3940256099942544/2247696110"
            )

            resValue("string", "admob_native_home_id", "ca-app-pub-3940256099942544/2247696110")
            resValue("string", "admob_native_feature_id", "ca-app-pub-3940256099942544/2247696110")
            resValue(
                "string",
                "admob_native_full_screen_id",
                "ca-app-pub-3940256099942544/2247696110"
            )
            resValue(
                "string",
                "native_full_screen2",
                "ca-app-pub-3940256099942544/2247696110"
            )

            // resValue("string", "admob_native_exit_id", "ca-app-pub-9498660037072845/9108646591")

            resValue("string", "admob_rewarded_id", "ca-app-pub-3940256099942544/5224354917")
            resValue("string", "downloader_rewarded_id", "ca-app-pub-3940256099942544/5224354917")

            isMinifyEnabled = false
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Koin
    implementation(libs.koin.android)

    // Shimmer
    implementation(libs.shimmer)
    // GMS (NextGen)
    implementation(libs.ads.mobile.sdk.v0220beta04)
}