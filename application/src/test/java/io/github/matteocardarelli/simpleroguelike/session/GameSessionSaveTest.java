package io.github.matteocardarelli.simpleroguelike.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.matteocardarelli.simpleroguelike.command.MoveCommand;
import io.github.matteocardarelli.simpleroguelike.content.StandardEnemyCatalog;
import io.github.matteocardarelli.simpleroguelike.content.StandardItemCatalog;
import io.github.matteocardarelli.simpleroguelike.generation.FixedFloorSource;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameSessionSaveTest {

    private static final SaveCodec CODEC =
            new SaveCodec(new StandardItemCatalog(), new StandardEnemyCatalog());

    private static final String[] ROWS = {
        "##########", "#@...g...#", "#..!.....#", "#....g..>#", "##########",
    };

    /**
     * Passi che portano il player tra i goblin e poi alle scale, con qualche urto contro i muri.
     */
    private static final Direction[] STEPS = {
        Direction.RIGHT, Direction.DOWN, Direction.RIGHT, Direction.UP, Direction.RIGHT,
        Direction.RIGHT, Direction.DOWN, Direction.DOWN, Direction.RIGHT, Direction.RIGHT,
        Direction.RIGHT, Direction.RIGHT, Direction.DOWN, Direction.LEFT, Direction.UP,
        Direction.UP, Direction.LEFT, Direction.LEFT, Direction.DOWN, Direction.RIGHT,
    };

    private static GameSession session() {
        return Sessions.multiFloorSession(new FixedFloorSource(ROWS), 2);
    }

    @Test
    void resume_inAnotherSession_givesTheSameSnapshot() {
        GameSession original = session();
        original.newGame(7);
        for (int i = 0; i < 5; i++) {
            original.handle(new MoveCommand(STEPS[i]));
        }

        GameSession resumed = session();
        TurnResult result = resumed.resume(CODEC, original.save(CODEC));

        assertThat(result.snapshot()).isEqualTo(original.current().snapshot());
        assertThat(result.events()).isEmpty();
    }

    /** Il resto della partita, a partire dal salvataggio, è uguale turno per turno. */
    @Test
    void resumed_playsTheSameTurns_asTheOriginal() {
        for (int savedAt = 0; savedAt < STEPS.length; savedAt += 4) {
            GameSession original = session();
            original.newGame(7);
            for (int i = 0; i < savedAt; i++) {
                original.handle(new MoveCommand(STEPS[i]));
            }
            GameSession resumed = session();
            resumed.resume(CODEC, original.save(CODEC));

            for (int i = savedAt; i < STEPS.length; i++) {
                TurnResult expected = original.handle(new MoveCommand(STEPS[i]));
                TurnResult actual = resumed.handle(new MoveCommand(STEPS[i]));

                assertThat(actual.events())
                        .as("eventi al passo %d", i)
                        .isEqualTo(expected.events());
                assertThat(actual.snapshot())
                        .as("snapshot al passo %d", i)
                        .isEqualTo(expected.snapshot());
            }
        }
    }

    @Test
    void save_ofTheResumedGame_isTheSameSave() {
        GameSession original = session();
        original.newGame(7);
        original.handle(new MoveCommand(Direction.RIGHT));
        List<String> saved = original.save(CODEC);

        GameSession resumed = session();
        resumed.resume(CODEC, saved);

        assertThat(resumed.save(CODEC)).isEqualTo(saved);
    }

    @Test
    void resume_ofAnotherFormat_throws_andKeepsTheGameInProgress() {
        GameSession session = session();
        TurnResult started = session.newGame(7);
        List<String> saved = new ArrayList<>(session.save(CODEC));
        saved.set(0, "format " + (SaveCodec.FORMAT + 1));

        assertThatThrownBy(() -> session.resume(CODEC, saved))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(session.current().snapshot()).isEqualTo(started.snapshot());
    }

    @Test
    void resume_ofATruncatedSave_throwsIllegalArgument() {
        GameSession session = session();
        session.newGame(7);
        List<String> saved = session.save(CODEC);

        assertThatThrownBy(() -> session().resume(CODEC, saved.subList(0, saved.size() / 2)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> session().resume(CODEC, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void save_beforeAGame_throwsIllegalState() {
        assertThatThrownBy(() -> session().save(CODEC)).isInstanceOf(IllegalStateException.class);
    }
}
