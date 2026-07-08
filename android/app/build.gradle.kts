plugins { id("com.android.application"); kotlin("android"); kotlin("plugin.serialization") }
android {
    namespace = "com.aion.host"; compileSdk = 36
    defaultConfig { applicationId = "com.aion.host"; minSdk = 31; targetSdk = 36; versionCode = 1; versionName = "0.1.0-alpha" }
    buildFeatures { compose = true }
}
dependencies {
    implementation(project(":brain"))
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")
}
