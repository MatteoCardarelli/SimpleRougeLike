package io.github.matteocardarelli.simpleroguelike.port;

/** Il fake passa lo stesso contratto dell'adapter vero. */
class StraightLinePathfinderTest extends PathfinderContractTest {

    @Override
    protected Pathfinder createPathfinder() {
        return new StraightLinePathfinder();
    }
}
