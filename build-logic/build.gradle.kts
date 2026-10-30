// Plugin di convenzione: la configurazione comune dei moduli sta qui, non in ogni build.gradle.kts.
plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.spotless.gradle.plugin)
    implementation(libs.errorprone.gradle.plugin)
    implementation(libs.animalsniffer.gradle.plugin)
    implementation(libs.pitest.gradle.plugin)
    implementation(libs.hilt.gradle.plugin)
}
