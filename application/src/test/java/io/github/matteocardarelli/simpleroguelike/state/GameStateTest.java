package io.github.matteocardarelli.simpleroguelike.state;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.command.MoveCommand;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorFixtures;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.snapshot.Phase;
import org.junit.jupiter.api.Test;

/** Le tre fasi della partita e le loro transizioni. */
class GameStateTest {

    private static final MoveCommand MOVE = new MoveCommand(Direction.UP);

    @Test
    void playing_acceptsCommands_andStaysPlayingWhileTheGameGoesOn() {
        Floor floor = FloorFixtures.parse("@g");
        GameState playing = new PlayingState();

        assertThat(playing.accepts(MOVE)).isTrue();
        assertThat(playing.phase()).isEqualTo(Phase.PLAYING);
        assertThat(playing.next(floor)).isSameAs(playing);
    }

    @Test
    void playing_playerDead_isGameOver() {
        Floor floor = FloorFixtures.parse("@g");
        floor.damage(new EntityId(1), 1000);

        assertThat(new PlayingState().next(floor)).isEqualTo(new GameOverState());
    }

    @Test
    void playing_noEnemyLeft_isVictory() {
        Floor floor = FloorFixtures.parse("@g");
        floor.damage(new EntityId(2), 1000);

        assertThat(new PlayingState().next(floor)).isEqualTo(new VictoryState());
    }

    @Test
    void playing_playerAndLastEnemyBothDead_defeatWins() {
        Floor floor = FloorFixtures.parse("@g");
        floor.damage(new EntityId(2), 1000);
        floor.damage(new EntityId(1), 1000);

        assertThat(new PlayingState().next(floor)).isEqualTo(new GameOverState());
    }

    @Test
    void terminalStates_rejectCommands_andNeverChange() {
        Floor floor = FloorFixtures.parse("@g");

        for (GameState terminal : new GameState[] {new GameOverState(), new VictoryState()}) {
            assertThat(terminal.accepts(MOVE)).isFalse();
            assertThat(terminal.next(floor)).isSameAs(terminal);
        }
        assertThat(new GameOverState().phase()).isEqualTo(Phase.GAME_OVER);
        assertThat(new VictoryState().phase()).isEqualTo(Phase.VICTORY);
    }
}
