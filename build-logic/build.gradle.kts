// Plugin di convenzione: la configurazione comune dei moduli sta qui, non in ogni build.gradle.kts.
plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.spotless.gradle.plugin)
}
