package io.github.matteocardarelli.simpleroguelike.save;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class SavedGameTest {

    @Test
    void parse_ofLines_givesTheSameGame() {
        SavedGame game =
                new SavedGame(42, List.of("move up", "use item=3"), List.of("format 1", "seed 42"));

        assertThat(SavedGame.parse(game.lines())).isEqualTo(game);
    }

    @Test
    void lines_putTheJournalFirst_thenTheSession() {
        SavedGame game = new SavedGame(-7, List.of("move left"), List.of("format 1"));

        assertThat(game.lines())
                .containsExactly("journal -7", "cmd move left", "session", "format 1");
    }

    @Test
    void parse_withoutTheSessionLine_throwsIllegalArgument() {
        assertThatThrownBy(() -> SavedGame.parse(List.of("journal 1", "cmd move up")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SavedGame.parse(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SavedGame.parse(List.of("journal x", "session")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
