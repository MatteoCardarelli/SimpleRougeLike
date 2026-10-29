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

// Le note di collaudo scritte sul telefono: ./gradlew :app:pullFeedback le copia in build/feedback,
// ./gradlew :app:replayFeedback -Pnote=<file> ne rigioca una sulla JVM.
val feedbackDir = layout.buildDirectory.dir("feedback")
val replaying = gradle.startParameter.taskNames.any { it.substringAfterLast(':') == "replayFeedback" }
val replayNote = providers.gradleProperty("note")
if (replaying && !replayNote.isPresent) {
    throw GradleException("Indicare la nota da rigiocare: ./gradlew :app:replayFeedback -Pnote=<file>")
}

tasks.withType<Test>().configureEach {
    systemProperty("srl.goldenDir", goldenDir)
    systemProperty("srl.updateGolden", updatingGolden)
    systemProperty("srl.feedbackDir", feedbackDir.get().asFile.absolutePath)
    if (updatingGolden) {
        filter.includeTestsMatching("*GoldenGameTest")
        outputs.upToDateWhen { false }
    }
    if (replaying) {
        // Il percorso vale dalla cartella del progetto, oppure è solo il nome di una nota copiata.
        val note = rootProject.file(replayNote.get()).takeIf { it.exists() }
            ?: feedbackDir.get().file(replayNote.get()).asFile
        systemProperty("srl.feedbackNote", note.absolutePath)
        filter.includeTestsMatching("*FeedbackReplayTest")
        outputs.upToDateWhen { false }
    }
}

tasks.register("updateGolden") {
    group = "verification"
    description = "Rigenera il file atteso del test golden dalla partita giocata adesso."
    dependsOn("testDebugUnitTest")
}

tasks.register("replayFeedback") {
    group = "verification"
    description = "Rigioca sulla JVM la partita di una nota di collaudo: -Pnote=<file>."
    dependsOn("testDebugUnitTest")
}

tasks.register("pullFeedback") {
    group = "verification"
    description = "Copia in build/feedback le note di collaudo dal telefono collegato con adb."
    val adb = androidComponents.sdkComponents.adb
    val applicationId = android.defaultConfig.applicationId!!
    val output = feedbackDir
    doLast {
        // run-as legge la memoria privata dell'app senza root, ma solo nelle build di debug.
        fun adb(vararg args: String): Triple<Int, ByteArray, String> {
            val process = ProcessBuilder(adb.get().asFile.absolutePath, *args).start()
            val output = process.inputStream.readBytes()
            val error = process.errorStream.readBytes().toString(Charsets.UTF_8)
            return Triple(process.waitFor(), output, error)
        }
        val (status, listing, error) = adb("shell", "run-as", applicationId, "ls", "files/feedback")
        val text = listing.toString(Charsets.UTF_8) + error
        val names = when {
            status == 0 -> text.lines().map { it.trim() }.filter { it.endsWith(".txt") }
            // L'app c'è ma non ha ancora scritto note.
            text.contains("No such file") -> emptyList()
            else -> throw GradleException(
                "adb non riesce a leggere le note: ${text.trim()}. Serve il telefono collegato con " +
                    "il debug USB e la build di debug di $applicationId installata."
            )
        }
        val directory = output.get().asFile
        directory.mkdirs()
        for (name in names) {
            val (copied, bytes, _) = adb("exec-out", "run-as", applicationId, "cat", "files/feedback/$name")
            if (copied != 0) {
                throw GradleException("Impossibile leggere la nota $name dal telefono")
            }
            directory.resolve(name).writeBytes(bytes)
        }
        logger.lifecycle("${names.size} note copiate in $directory")
    }
}

dependencies {
    implementation(project(":application"))
    implementation(project(":infrastructure"))
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.fragment)
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
