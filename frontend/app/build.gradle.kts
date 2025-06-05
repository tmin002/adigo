plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    //alias(libs.plugins.kotlin.compose)
    id("io.realm.kotlin")
    id("com.google.gms.google-services")
}

// 버전 관리 변수
val majorVersion = 1
val minorVersion = 4
val patchVersion = 3
val buildNumber = 7

android {
    namespace = "kr.gachon.adigo"
    compileSdk = 35



    defaultConfig {
        applicationId = "kr.gachon.adigo"
        minSdk = 33
        targetSdk = 35
        versionCode = buildNumber
        versionName = "$majorVersion.$minorVersion.$patchVersion"

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
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
}

dependencies {

    // ───────── Passkey (Credential Manager) ─────────
    implementation(libs.androidx.credentials)


    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))//
    // 지도 컴포즈 통합 라이브러리 (Google Maps Compose)
    implementation(libs.google.maps.compose)

    // Coil - 이미지 로딩 라이브러리
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Jetpack Compose용 머티리얼 디자인 컴포넌트
    implementation(libs.androidx.compose.material)


    // JWT (JSON Web Token) 라이브러리 - 인증 및 토큰 검증용
    implementation(libs.auth0.java.jwt)

    // Jetpack Compose 내 네비게이션 처리 (버전 카탈로그 사용)
    implementation(libs.androidx.navigation.compose)

    // 프래그먼트 기반 네비게이션 지원 (버전 카탈로그 사용)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    
    // RealmKotlin
    implementation("io.realm.kotlin:library-base:1.16.0")

    // 동적 기능 모듈에서의 네비게이션 지원 (버전 카탈로그 사용)
    implementation(libs.androidx.navigation.dynamic.features.fragment)
    implementation(libs.material)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.firebase.messaging)

    // 네비게이션 관련 테스트 라이브러리 (버전 카탈로그 사용)
    androidTestImplementation(libs.androidx.navigation.testing)

    // UWB (Ultra-Wideband) API: 근거리 통신 및 위치 측정 기능
    implementation(libs.androidx.core.uwb)
    implementation(libs.androidx.core.uwb.rxjava3)

    // RxJava: 비동기 및 반응형 프로그래밍 지원
    implementation(libs.io.reactivex.rxjava3)

    // Retrofit: REST API 통신을 위한 HTTP 클라이언트
    implementation(libs.square.retrofit)

    // Retrofit에서 JSON 직렬화/역직렬화를 위한 Gson 컨버터 (권장)
    implementation(libs.square.retrofit.converter.gson)

    //위치 의존성 추가
    implementation("com.google.android.gms:play-services-location:21.0.1")

    //glide 의존성 추가
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // OkHttp: 네트워크 요청을 위한 HTTP 클라이언트 및 로깅 지원
    implementation(libs.square.okhttp3.okhttp)
    implementation(libs.square.okhttp3.logging.interceptor)

    // Guava: 구글의 유틸리티 라이브러리 (다양한 편리 기능 제공)
    implementation(libs.google.guava)





    // 추가 AndroidX 라이브러리들
    implementation(libs.androidx.core.ktx)
    implementation(libs.google.services.auth)
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
    implementation(libs.androidx.security.crypto)

    // 테스트 관련 라이브러리들
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
