package io.github.matteocardarelli.simpleroguelike.ui.playback;

class InstantEventPlayerTest extends EventPlayerContractTest {

    @Override
    EventPlayer create(EventListener listener) {
        return new InstantEventPlayer(new CompositeEventListener(java.util.List.of(listener)));
    }
}
