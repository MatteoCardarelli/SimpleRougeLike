// Mutation testing con PIT: misura se i test si accorgono di un errore nel codice, non solo se
// eseguono le sue righe. Si applica solo ai moduli che hanno una soglia, oggi solo :domain.
import info.solidsoft.gradle.pitest.PitestPluginExtension

plugins {
    id("info.solidsoft.pitest")
}

val libs = the<VersionCatalogsExtension>().named("libs")

// Per tipo: "pitest" è anche il nome di una configurazione di dipendenze.
configure<PitestPluginExtension> {
    pitestVersion = libs.findVersion("pitest").get().requiredVersion
    junit5PluginVersion = libs.findVersion("pitest-junit5-plugin").get().requiredVersion
    targetClasses = setOf("io.github.matteocardarelli.simpleroguelike.*")
    // I test di proprietà provano centinaia di sequenze: dentro PIT, che ripete i test per ogni
    // mutante, costerebbero troppo. Le stesse regole hanno anche test d'esempio.
    excludedTestClasses = setOf("*Properties", "*ArchitectureTest")
    mutationThreshold = 70
    outputFormats = setOf("HTML", "XML")
    timestampedReports = false
    threads = 2
}
