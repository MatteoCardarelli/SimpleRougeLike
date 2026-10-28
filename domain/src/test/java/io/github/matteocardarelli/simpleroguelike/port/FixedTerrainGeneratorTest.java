package io.github.matteocardarelli.simpleroguelike.port;

/** Il fake passa lo stesso contratto dell'adapter vero. */
class FixedTerrainGeneratorTest extends TerrainGeneratorContractTest {

    @Override
    protected TerrainGenerator createGenerator() {
        return new FixedTerrainGenerator();
    }
}
