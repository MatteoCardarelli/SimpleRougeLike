package io.github.matteocardarelli.simpleroguelike.ui.playback;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.command.MoveCommand;
import io.github.matteocardarelli.simpleroguelike.di.GameConfig;
import io.github.matteocardarelli.simpleroguelike.di.TestGraph;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.session.GameSession;
import io.github.matteocardarelli.simpleroguelike.session.TurnResult;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TurnPlaybackTest {

    private final ControlledEventPlayer player = new ControlledEventPlayer();
    private final Scene scene = new Scene(SceneTest.PLAYER);
    private final TurnPlayback playback = new TurnPlayback(player, scene);
    private final AtomicInteger idle = new AtomicInteger();
    private GameSession session;

    @BeforeEach
    void setUp() {
        playback.setOnIdle(idle::incrementAndGet);
        session = TestGraph.session(GameConfig.standard());
    }

    @Test
    void show_animated_isBusyUntilThePlayerFinishes_thenSyncsAndCallsOnIdle() {
        TurnResult result = session.newGame(1);

        playback.show(result, true);
        boolean busyWhilePlaying = playback.isBusy();
        player.complete();

        assertThat(busyWhilePlaying).isTrue();
        assertThat(playback.isBusy()).isFalse();
        assertThat(idle).hasValue(1);
        assertThat(scene.sprites()).isNotEmpty();
    }

    @Test
    void show_notAnimated_syncsTheSceneWithoutPlaying() {
        playback.show(session.newGame(1), false);

        assertThat(player.played).isEmpty();
        assertThat(playback.isBusy()).isFalse();
        assertThat(scene.sprites()).isNotEmpty();
    }

    /** Aprire l'inventario pubblica lo stesso risultato: non deve interrompere niente. */
    @Test
    void show_sameRevisionAgain_doesNothing() {
        TurnResult result = session.newGame(1);
        playback.show(result, true);

        playback.show(result, true);

        assertThat(player.played).hasSize(1);
        assertThat(player.finishCalls).isZero();
    }

    /** "Nuova partita" durante un'animazione: quella in corso salta alla fine, senza onIdle. */
    @Test
    void show_newRevisionWhileBusy_finishesTheCurrentOne_andPlaysTheNew() {
        playback.show(session.newGame(1), true);

        playback.show(session.handle(new MoveCommand(Direction.UP)), true);
        int idleBeforeTheSecondEnds = idle.get();
        player.complete();

        assertThat(player.finishCalls).isEqualTo(1);
        assertThat(player.played).hasSize(2);
        assertThat(idleBeforeTheSecondEnds).isZero();
        assertThat(idle).hasValue(1);
        assertThat(playback.isBusy()).isFalse();
    }
}
