package io.github.matteocardarelli.simpleroguelike.ui.playback;

import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Un riproduttore che finisce solo quando il test lo dice, come farà quello animato. */
final class ControlledEventPlayer implements EventPlayer {

    final List<List<GameEvent>> played = new ArrayList<>();
    int finishCalls;
    private @Nullable Runnable onFinished;

    @Override
    public void play(List<GameEvent> events, Scene scene, Runnable onFinished) {
        played.add(events);
        this.onFinished = onFinished;
    }

    @Override
    public void finish() {
        finishCalls++;
        complete();
    }

    /** La riproduzione in corso arriva alla fine. */
    void complete() {
        Runnable finished = onFinished;
        onFinished = null;
        if (finished != null) {
            finished.run();
        }
    }
}
