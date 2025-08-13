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
    if (propsFile.exists()) load(propsFile.inputStream())
}

val supabaseUrl     = localProperties.getProperty("SUPABASE_URL") ?: "MISSING_URL"
val supabaseAnonKey = localProperties.getProperty("SUPABASE_ANON_KEY") ?: "MISSING_KEY"
val googleClientId  = localProperties.getProperty("GOOGLE_ANDROID_CLIENT_ID") ?: "MISSING_CLIENT_ID"
val deepgramApiKey  = localProperties.getProperty("DEEPGRAM_API_KEY") ?: "MISSING_KEY"
val translateApiKey = localProperties.getProperty("GOOGLE_TRANSLATE_API_KEY") ?: "MISSING_TRANSLATE_KEY"

// Read API base from local.properties, fallback to your Railway app (no trailing slash)
val apiBaseUrl = localProperties.getProperty("API_BASE_URL")
    ?: "https://echoscholar-web-production-59f5.up.railway.app"

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
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
        buildConfigField("String", "DEEPGRAM_API_KEY", "\"$deepgramApiKey\"")
        buildConfigField("String", "GOOGLE_TRANSLATE_API_KEY", "\"$translateApiKey\"")
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")

        vectorDrawables.useSupportLibrary = true
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
    kotlinOptions { jvmTarget = "11" }

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
    implementation(libs.androidx.ui.test.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    implementation(platform("io.github.jan-tennert.supabase:bom:3.1.4"))
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:postgrest-kt:3.1.4")
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation("com.squareup.okio:okio:3.7.0")
    implementation("io.ktor:ktor-client-core:3.1.3")
    implementation("io.ktor:ktor-client-okhttp:3.1.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.1.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.3")
    implementation("io.ktor:ktor-client-logging:3.1.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")

}
