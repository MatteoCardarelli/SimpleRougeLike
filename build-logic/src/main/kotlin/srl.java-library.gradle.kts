// Configurazione comune dei moduli Java puri: :domain, :application e :infrastructure.
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    `java-library`
    `java-test-fixtures`
    jacoco
    id("com.android.lint")
    id("net.ltgt.errorprone")
    id("ru.vyarus.animalsniffer")
    id("srl.spotless")
}

val libs = the<VersionCatalogsExtension>().named("libs")
fun lib(alias: String) = libs.findLibrary(alias).get()

val srl = extensions.create<SrlJavaLibraryExtension>("srl")

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    api(lib("jspecify"))
    testFixturesApi(lib("jspecify"))
    // I test di contratto delle porte stanno nei testFixtures: chi li estende eredita JUnit e AssertJ.
    testFixturesApi(platform(lib("junit-bom")))
    testFixturesApi(lib("junit-jupiter"))
    testFixturesApi(lib("assertj-core"))

    errorprone(lib("errorprone-core"))
    errorprone(lib("nullaway"))

    // Le API di Java che esistono su Android 11 (API 30): il minSdk dell'app.
    signature("com.toasttab.android:gummy-bears-api-30:${libs.findVersion("gummy-bears").get().requiredVersion}@signature")

    testImplementation(platform(lib("junit-bom")))
    testImplementation(lib("junit-jupiter"))
    testImplementation(lib("jqwik"))
    testImplementation(lib("assertj-core"))
    testImplementation(lib("archunit-junit5"))
    testRuntimeOnly(lib("junit-platform-launcher"))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.errorprone {
        disableWarningsInGeneratedCode = true
        // Un valore che può essere null e viene usato senza controllo è un errore di compilazione.
        check("NullAway", CheckSeverity.ERROR)
        option("NullAway:OnlyNullMarked", "true")
    }
}

// Questi moduli compilano contro il JDK, ma girano su Android: un metodo che manca da Android 11
// compila e poi fallisce sul telefono. Lint qui non lo segnala, Animal Sniffer sì.
// I test girano solo sulla JVM, quindi non si controllano.
animalsniffer {
    sourceSets = listOf(the<SourceSetContainer>()["main"], the<SourceSetContainer>()["testFixtures"])
    // Classi che su Android 11 non esistono ma che D8 traduce quando compila l'app: la superclasse
    // dei record, i metodi generati dei record e lo switch con i pattern (verificato sul dex).
    ignore = listOf(
        "java.lang.Record",
        "java.lang.runtime.ObjectMethods",
        "java.lang.runtime.SwitchBootstraps",
        "java.lang.MatchException",
    )
}

lint {
    abortOnError = true
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }
}

afterEvaluate {
    if (srl.minimumLineCoverage.isPresent) {
        tasks.jacocoTestCoverageVerification {
            violationRules {
                rule {
                    limit {
                        counter = "LINE"
                        minimum = srl.minimumLineCoverage.get().toBigDecimal()
                    }
                }
            }
        }
        tasks.check {
            dependsOn(tasks.jacocoTestCoverageVerification)
        }
    }
}
