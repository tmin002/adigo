plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    //alias(libs.plugins.kotlin.compose)
    id("io.realm.kotlin")
}



android {
    namespace = "kr.gachon.adigo"
    compileSdk = 35



    defaultConfig {
        applicationId = "kr.gachon.adigo"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
}

dependencies {

    val navversion = "2.8.9"

    // RealmKotlin
    implementation("io.realm.kotlin:library-base:1.16.0")

    //jwt
    implementation ("com.auth0:java-jwt:4.5.0")
    
    // Jetpack Compose integration
    implementation("androidx.navigation:navigation-compose:$navversion")

    // Views/Fragments integration
    implementation("androidx.navigation:navigation-fragment:$navversion")
    implementation("androidx.navigation:navigation-ui:$navversion")

    // Feature module support for Fragments
    implementation("androidx.navigation:navigation-dynamic-features-fragment:$navversion")

    // Testing Navigation
    androidTestImplementation("androidx.navigation:navigation-testing:$navversion")

    implementation ("androidx.core.uwb:uwb:1.0.0-alpha08")
    implementation ("androidx.core.uwb:uwb-rxjava3:1.0.0-alpha08")

    implementation ("io.reactivex.rxjava3:rxjava:3.1.5")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")

    // JSON 변환을 위한 Gson 추가 (권장)
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // OkHttp (옵션이지만 일반적으로 사용됨)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation ("com.google.guava:guava:32.0.1-jre")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.foundation.layout.android)
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.foundation.android)
    implementation (libs.androidx.security.crypto)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}