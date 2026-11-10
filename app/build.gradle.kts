import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) localFile.inputStream().use { load(it) }
}
val configuredSupabaseUrl = providers.gradleProperty("SUPABASE_URL")
    .orElse(localProperties.getProperty("SUPABASE_URL", ""))
    .get()
val configuredSupabaseKey = providers.gradleProperty("SUPABASE_ANON_KEY")
    .orElse(localProperties.getProperty("SUPABASE_ANON_KEY", ""))
    .get()
val configuredGoogleWebClientId = providers.gradleProperty("GOOGLE_WEB_CLIENT_ID")
    .orElse(localProperties.getProperty("GOOGLE_WEB_CLIENT_ID", ""))
    .get()

android {
    namespace = "com.example.reloop_technologies_mobile_app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.reloop_technologies_mobile_app"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SUPABASE_URL", "\"$configuredSupabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$configuredSupabaseKey\"")
        buildConfigField("String", "RELOOP_API_URL", "\"${configuredSupabaseUrl.trimEnd('/')}/functions/v1/reloop-api/\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$configuredGoogleWebClientId\"")
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
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation(platform("io.github.jan-tennert.supabase:bom:3.9.0"))
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.ktor:ktor-client-android:3.3.3")
    implementation("com.squareup.retrofit2:retrofit:2.12.0")
    implementation("com.squareup.retrofit2:converter-gson:2.12.0")
    implementation("com.google.code.gson:gson:2.11.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation("androidx.camera:camera-core:1.6.2")
    implementation("androidx.camera:camera-camera2:1.6.2")
    implementation("androidx.camera:camera-lifecycle:1.6.2")
    implementation("androidx.camera:camera-view:1.6.2")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
