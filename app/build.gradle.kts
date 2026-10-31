plugins {
    id("srl.android-application")
}

android {
    namespace = "io.github.matteocardarelli.simpleroguelike"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.matteocardarelli.simpleroguelike"
        minSdk = 30
        targetSdk = 36

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets {
        // Il giocatore automatico del test golden e la sessione dei test servono sia sulla JVM
        // sia sul dispositivo: lo stesso codice deve dare la stessa partita nei due posti.
        getByName("test").java.srcDir("src/sharedTest/java")
        getByName("androidTest").java.srcDir("src/sharedTest/java")
        // Il registro atteso del golden, letto anche dal test sul dispositivo.
        getByName("androidTest").assets.srcDir("src/test/resources/golden")
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
    implementation(libs.lifecycle.viewmodel.savedstate)
    implementation(libs.lifecycle.livedata)
    implementation(libs.fragment)
    implementation(libs.recyclerview)

    testImplementation(testFixtures(project(":domain")))

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.espresso.core)
    // Sul dispositivo servono solo le classi dei testFixtures, come EventLogWriter: JUnit 5,
    // jqwik e AssertJ, che i testFixtures portano per i test di contratto, restano fuori
    // dall'APK di test.
    val domainFixtures = testFixtures(project(":domain")) as ModuleDependency
    domainFixtures.exclude(group = "org.junit")
    domainFixtures.exclude(group = "org.junit.jupiter")
    domainFixtures.exclude(group = "org.junit.platform")
    domainFixtures.exclude(group = "org.assertj")
    domainFixtures.exclude(group = "net.jqwik")
    androidTestImplementation(domainFixtures)
}
