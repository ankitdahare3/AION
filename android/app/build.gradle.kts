plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.aion.host"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.aion.host"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-alpha"
        // T-022: HiltTestRunner swaps in HiltTestApplication so @HiltAndroidTest works on-device.
        testInstrumentationRunner = "com.aion.host.HiltTestRunner"
    }
    buildFeatures {
        compose = true
        // T-043: ShizukuBridge's shell-exec UserService is bound over an AIDL interface —
        // Shizuku privatized the old direct newProcess() shell exec in recent api versions.
        aidl = true
    }
    kotlinOptions { jvmTarget = "17" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    sourceSets {
        // T-060: MigrationTestHelper reads exported schema JSONs as test assets.
        getByName("androidTest") { assets.srcDirs("$projectDir/schemas") }
    }
}

ksp {
    // Room DOC-019 §1 — export schema JSON for future migration testing (T-060).
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":brain"))

    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.security.crypto)
    implementation(libs.shizuku.api)
    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.sqlite)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.translate)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.biometric)
    implementation(libs.onnxruntime.android)
    // T-102 — GmailPlugin/TelegramPlugin (:brain) default-construct an HttpClient; that type must
    // be resolvable here even though :android:app never builds one itself.
    implementation(libs.ktor.client.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    androidTestImplementation(libs.androidx.room.testing)
}
