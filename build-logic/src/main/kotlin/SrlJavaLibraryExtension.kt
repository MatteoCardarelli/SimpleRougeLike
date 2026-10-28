import org.gradle.api.provider.Property

/** Impostazioni di `srl.java-library` che cambiano da un modulo all'altro. */
abstract class SrlJavaLibraryExtension {
    /**
     * Copertura minima delle righe, tra 0 e 1, sotto la quale `check` fallisce. Se non è impostata
     * JaCoCo produce solo il report.
     */
    abstract val minimumLineCoverage: Property<Double>
}
