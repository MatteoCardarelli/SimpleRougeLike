package io.github.matteocardarelli.simpleroguelike.ui.playback;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.command.MoveCommand;
import io.github.matteocardarelli.simpleroguelike.di.GameConfig;
import io.github.matteocardarelli.simpleroguelike.di.TestGraph;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.session.GameSession;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Cosa promette ogni {@link EventPlayer}: lo estende il riproduttore istantaneo, e lo estenderà
 * quello animato. Il riproduttore lo si prova portandolo alla fine con {@code finish()}.
 */
abstract class EventPlayerContractTest {

    /** Un riproduttore nuovo che avvisa {@code listener} di ogni evento mostrato. */
    abstract EventPlayer create(EventListener listener);

    private static List<GameEvent> someEvents() {
        GameSession session = TestGraph.session(GameConfig.standard());
        session.newGame(1);
        List<GameEvent> events = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            events.addAll(session.handle(new MoveCommand(direction)).events());
        }
        return events;
    }

    @Test
    void play_notifiesEveryEventInOrder_andFinishesOnce() {
        List<GameEvent> heard = new ArrayList<>();
        AtomicInteger finished = new AtomicInteger();
        EventPlayer player = create(heard::add);
        List<GameEvent> events = someEvents();

        player.play(events, new Scene(SceneTest.PLAYER), finished::incrementAndGet);
        player.finish();

        assertThat(events).isNotEmpty();
        assertThat(heard).containsExactlyElementsOf(events);
        assertThat(finished).hasValue(1);
    }

    @Test
    void finish_withoutAPlayback_doesNothing() {
        EventPlayer player = create(event -> {});

        player.finish();
    }
}
