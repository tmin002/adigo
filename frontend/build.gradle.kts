// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    //alias(libs.plugins.kotlin.compose) apply false
    id("io.realm.kotlin") version "1.16.0" // 최신 버전 확인 필요

    id("com.google.gms.google-services") version "4.4.2" apply false
}


