// Configurazione comune del modulo :app.
plugins {
    id("com.android.application")
    id("com.google.dagger.hilt.android")
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

// La versione è il tag vX.Y.Z del commit, se c'è: un tag sulla branch principale è una release.
// Senza tag la versione dice che è una build di sviluppo.
val gitTag = providers.exec {
    commandLine("git", "describe", "--tags", "--exact-match", "--match", "v*")
    isIgnoreExitValue = true
}.standardOutput.asText.map { it.trim() }

// La firma della release arriva da variabili d'ambiente, in CI dai secret di GitHub: il keystore
// non sta mai nel repository. Senza variabili la release si compila non firmata.
val releaseKeystore = providers.environmentVariable("SRL_KEYSTORE_FILE")

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

        // Per l'APK dei test strumentati quando l'app è compilata con R8.
        testProguardFiles("proguard-test-rules.pro")

        val tag = Regex("""v(\d+)\.(\d+)\.(\d+)""").matchEntire(gitTag.get())
        if (tag != null) {
            val (major, minor, patch) = tag.destructured
            versionName = "$major.$minor.$patch"
            versionCode = major.toInt() * 10000 + minor.toInt() * 100 + patch.toInt()
        } else {
            versionName = "0.0.0-dev"
            versionCode = 1
        }
    }
    signingConfigs {
        if (releaseKeystore.isPresent) {
            create("release") {
                storeFile = file(releaseKeystore.get())
                storePassword = providers.environmentVariable("SRL_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("SRL_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("SRL_KEY_PASSWORD").get()
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
        // Come debug, ma con R8: un problema di R8 si vede prima della release, con gli stessi
        // test strumentati. Non è debuggable, altrimenti R8 non ottimizza né offusca niente; resta
        // firmata con la chiave di debug, quindi si installa e si prova come la debug.
        create("minified") {
            initWith(getByName("debug"))
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "proguard-minified-rules.pro",
            )
            matchingFallbacks += "debug"
        }
    }
    // I test strumentati girano sulla build di debug; -Psrl.testBuildType=minified li fa girare
    // sulla build con R8.
    testBuildType = providers.gradleProperty("srl.testBuildType").getOrElse("debug")
    lint {
        abortOnError = true
        checkDependencies = true
        // Tutto il testo dell'interfaccia sta nelle risorse, mai nel codice o nei layout.
        error += "HardcodedText"
    }
    testOptions {
        // I test sulla JVM usano JUnit 5, come nei moduli Java: basta la piattaforma di JUnit,
        // senza plugin. I test strumentati restano su JUnit 4, come vuole AndroidX Test.
        unitTests.all { it.useJUnitPlatform() }
        // Senza animazioni i test strumentati non aspettano transizioni, e sono più stabili.
        animationsDisabled = true
        // Un dispositivo gestito da Gradle: lo stesso emulatore in CI e in locale, con
        // l'immagine ATD, più leggera, e l'API minima dell'app.
        managedDevices {
            localDevices {
                create("pixel2api30") {
                    device = "Pixel 2"
                    apiLevel = 30
                    systemImageSource = "aosp-atd"
                    // L'immagine a 64 bit, come il runner della CI e i computer di sviluppo:
                    // senza, Gradle sceglie quella x86.
                    require64Bit = true
                }
            }
        }
    }
}

val libs = the<VersionCatalogsExtension>().named("libs")
fun lib(alias: String) = libs.findLibrary(alias).get()

dependencies {
    // Hilt collega gli oggetti dell'app: solo in :app, i moduli Java restano senza framework.
    implementation(lib("hilt-android"))
    "annotationProcessor"(lib("hilt-compiler"))

    testImplementation(platform(lib("junit-bom")))
    testImplementation(lib("junit-jupiter"))
    testImplementation(lib("assertj-core"))
    testImplementation(lib("archunit-junit5"))
    testRuntimeOnly(lib("junit-platform-launcher"))
}
