// Le regole del gioco: Java puro, senza Android e senza librerie esterne.
plugins {
    id("srl.java-library")
    id("srl.mutation-testing")
}

srl {
    minimumLineCoverage = 0.80
}
