package io.github.matteocardarelli.simpleroguelike.feedback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.snapshot.Phase;
import io.github.matteocardarelli.simpleroguelike.snapshot.PlayerView;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class FeedbackEntryTest {

    static FeedbackEntry entry(String text) {
        return new FeedbackEntry(
                "2026-10-29T10:15:03",
                "1.0",
                "3baf5e8",
                20_261_029L,
                12,
                15,
                Phase.PLAYING,
                new PlayerView(
                        new EntityId(1), new Position(3, 4), 80, 100, 1, 25, 100, 12, 3, 10, 5, 45),
                true,
                text,
                List.of("move up", "use item=12"));
    }

    @Test
    void toLines_writesKeysTextAndCommands() {
        List<String> lines = entry("Il goblin attraversa il muro").toLines();

        assertThat(lines)
                .containsExactly(
                        "created=2026-10-29T10:15:03",
                        "version=1.0",
                        "commit=3baf5e8",
                        "seed=20261029",
                        "turn=12",
                        "revision=15",
                        "phase=playing",
                        "player=1",
                        "position=3,4",
                        "health=80/100",
                        "level=1",
                        "experience=25/100",
                        "coins=45",
                        "stats=12/3/10/5",
                        "inventory_open=true",
                        "---",
                        "Il goblin attraversa il muro",
                        "---",
                        "move up",
                        "use item=12");
    }

    @Test
    void parse_isTheInverseOfToLines() {
        FeedbackEntry entry = entry("Prima riga\n---\nUna riga uguale al separatore");

        assertThat(FeedbackEntry.parse(entry.toLines())).isEqualTo(entry);
    }

    @Test
    void parse_withoutSeparators_throws() {
        assertThatThrownBy(() -> FeedbackEntry.parse(List.of("seed=1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_copiesTheCommands() {
        List<String> commands = new ArrayList<>(List.of("move up"));
        FeedbackEntry entry =
                new FeedbackEntry(
                        "t",
                        "1.0",
                        "c",
                        1,
                        1,
                        1,
                        Phase.PLAYING,
                        entry("x").player(),
                        false,
                        "x",
                        commands);

        commands.add("move down");

        assertThat(entry.commands()).containsExactly("move up");
        assertThatThrownBy(() -> entry.commands().add("move left"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
