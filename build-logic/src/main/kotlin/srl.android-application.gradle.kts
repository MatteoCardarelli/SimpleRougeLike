// Configurazione comune del modulo :app.
plugins {
    id("com.android.application")
    id("srl.spotless")
}

android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        viewBinding = true
    }
    lint {
        abortOnError = true
        checkDependencies = true
        // Tutto il testo dell'interfaccia sta nelle risorse, mai nel codice o nei layout.
        error += "HardcodedText"
        // Errori del codice scritto prima del refactoring: il file sparisce con quel codice.
        baseline = file("lint-baseline.xml")
    }
}
