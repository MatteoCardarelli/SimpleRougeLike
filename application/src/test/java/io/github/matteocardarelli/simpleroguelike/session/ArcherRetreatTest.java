package io.github.matteocardarelli.simpleroguelike.session;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.command.MoveCommand;
import io.github.matteocardarelli.simpleroguelike.generation.FixedFloorSource;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.snapshot.ActorView;
import org.junit.jupiter.api.Test;

/** Issue #54: l'arciere scappa, ma non due turni di fila, e chi lo insegue lo può colpire. */
class ArcherRetreatTest {

    private static Position archer(TurnResult result) {
        return result.snapshot().visibleEnemies().stream()
                .map(ActorView::position)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void archer_retreatsOnce_thenFightsTheChasingPlayer() {
        GameSession session =
                Sessions.session(new FixedFloorSource("#######", "#@a...#", "#######"));
        session.newGame(1);

        // Un passo contro il muro: il turno passa e l'arciere, accanto, si allontana.
        TurnResult first = session.handle(new MoveCommand(Direction.UP));
        assertThat(archer(first)).isEqualTo(new Position(3, 1));

        // Il player lo raggiunge: appena scappato, l'arciere resta e combatte da vicino.
        TurnResult second = session.handle(new MoveCommand(Direction.RIGHT));
        assertThat(second.snapshot().player().position()).isEqualTo(new Position(2, 1));
        assertThat(archer(second)).isEqualTo(new Position(3, 1));
    }
}
