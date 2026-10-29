// Configurazione comune del modulo :app.
plugins {
    id("com.android.application")
    id("srl.spotless")
}

// Il commit da cui è compilata l'app, con -dirty se ci sono modifiche non committate: una nota di
// collaudo lo salva, e una partita si rigioca identica solo con lo stesso codice.
val gitCommit = providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
    isIgnoreExitValue = true
}.standardOutput.asText.map { it.trim().ifEmpty { "unknown" } }
val gitDirty = providers.exec {
    commandLine("git", "status", "--porcelain", "--untracked-files=no")
    isIgnoreExitValue = true
}.standardOutput.asText.map { it.isNotBlank() }

android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    defaultConfig {
        val commit = gitCommit.get() + if (gitDirty.get()) "-dirty" else ""
        buildConfigField("String", "GIT_COMMIT", "\"$commit\"")
    }
    lint {
        abortOnError = true
        checkDependencies = true
        // Tutto il testo dell'interfaccia sta nelle risorse, mai nel codice o nei layout.
        error += "HardcodedText"
        // Errori del codice scritto prima del refactoring: il file sparisce con quel codice.
        baseline = file("lint-baseline.xml")
    }
    testOptions {
        // I test sulla JVM usano JUnit 5, come nei moduli Java: basta la piattaforma di JUnit,
        // senza plugin. I test strumentati restano su JUnit 4, come vuole AndroidX Test.
        unitTests.all { it.useJUnitPlatform() }
    }
}

val libs = the<VersionCatalogsExtension>().named("libs")
fun lib(alias: String) = libs.findLibrary(alias).get()

dependencies {
    testImplementation(platform(lib("junit-bom")))
    testImplementation(lib("junit-jupiter"))
    testImplementation(lib("assertj-core"))
    testImplementation(lib("archunit-junit5"))
    testRuntimeOnly(lib("junit-platform-launcher"))
}
