// I casi d'uso della partita: un turno dall'inizio alla fine, le fasi, cosa vede il giocatore.
plugins {
    id("srl.java-library")
}

srl {
    minimumLineCoverage = 0.80
}

dependencies {
    api(project(":domain"))

    testImplementation(testFixtures(project(":domain")))
}
