// Gli adapter: implementano le porte del dominio. Qui vivono gli unici usi di SquidLib.
plugins {
    id("srl.java-library")
}

dependencies {
    api(project(":domain"))
    implementation(libs.squidlib.util)

    testImplementation(testFixtures(project(":domain")))
}
