import java.util.Properties
import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    id("org.jetbrains.kotlin.kapt")

}

// Load from local.properties
val localProperties = Properties().apply {
    val propsFile = File(rootProject.rootDir, "local.properties")
    if (propsFile.exists()) {
        load(propsFile.inputStream())
    }
}

val supabaseUrl = localProperties.getProperty("SUPABASE_URL") ?: "MISSING_URL"
val supabaseAnonKey = localProperties.getProperty("SUPABASE_ANON_KEY") ?: "MISSING_KEY"
val googleClientId = localProperties.getProperty("GOOGLE_ANDROID_CLIENT_ID") ?: "MISSING_CLIENT_ID"


android {
    namespace = "com.bibintomj.echoscholar"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bibintomj.echoscholar"
        minSdk = 35
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GOOGLE_ANDROID_CLIENT_ID", "\"$googleClientId\"")
//        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")

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
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.activity)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    // Supabase (auth) with BOM
    implementation(platform("io.github.jan-tennert.supabase:bom:3.1.4"))
//    implementation(platform("io.github.jan-tennert.supabase:bom:3.2.0"))

    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("com.google.android.gms:play-services-auth:20.7.0")



    implementation("com.squareup.okio:okio:3.7.0")
    // ✅ Ktor 2.3.7 — this version works with Supabase 3.1.4
    implementation("io.ktor:ktor-client-core:3.1.3")
    implementation("io.ktor:ktor-client-okhttp:3.1.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.1.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.3")
    implementation("io.ktor:ktor-client-logging:3.1.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
    implementation("io.github.jan-tennert.supabase:postgrest-kt:3.1.4")

}