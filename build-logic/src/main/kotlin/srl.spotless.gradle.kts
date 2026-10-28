// Formattazione del codice Java, uguale in tutti i moduli.
plugins {
    id("com.diffplug.spotless")
}

val libs = the<VersionCatalogsExtension>().named("libs")

spotless {
    java {
        target("src/**/*.java")
        // Versione fissata: una nuova può riformattare il codice, quindi si aggiorna in una PR a sé.
        googleJavaFormat(libs.findVersion("google-java-format").get().requiredVersion).aosp()
    }
}
