plugins {
    id("srl.android-application")
}

android {
    namespace = "com.github.matteoCarda.simplerougelike"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.github.matteoCarda.simplerougelike"
        minSdk = 30
        targetSdk = 36
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
}

// Il file atteso del test golden si rigenera solo di proposito, quando cambia una regola del
// gioco: ./gradlew :app:updateGolden. Gli altri task lo confrontano e basta.
val updatingGolden = gradle.startParameter.taskNames.any { it.substringAfterLast(':') == "updateGolden" }
val goldenDir = layout.projectDirectory.dir("src/test/resources/golden").asFile.absolutePath

tasks.withType<Test>().configureEach {
    systemProperty("srl.goldenDir", goldenDir)
    systemProperty("srl.updateGolden", updatingGolden)
    if (updatingGolden) {
        filter.includeTestsMatching("*GoldenGameTest")
        outputs.upToDateWhen { false }
    }
}

tasks.register("updateGolden") {
    group = "verification"
    description = "Rigenera il file atteso del test golden dalla partita giocata adesso."
    dependsOn("testDebugUnitTest")
}

dependencies {
    implementation(project(":application"))
    implementation(project(":infrastructure"))
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.squidlib.util)

    testImplementation(testFixtures(project(":domain")))
    testImplementation(libs.junit4)
    testImplementation(libs.mockito.core)
    // I test del codice di prima del refactoring sono scritti con JUnit 4: il motore vintage li
    // esegue sulla piattaforma di JUnit 5 finché non spariscono insieme a quel codice.
    testRuntimeOnly(libs.junit.vintage.engine)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
