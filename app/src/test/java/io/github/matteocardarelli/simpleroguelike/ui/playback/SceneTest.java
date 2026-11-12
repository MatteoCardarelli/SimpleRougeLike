package io.github.matteocardarelli.simpleroguelike.ui.playback;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.di.GameConfig;
import io.github.matteocardarelli.simpleroguelike.di.TestGraph;
import io.github.matteocardarelli.simpleroguelike.generation.FixedFloorSource;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.snapshot.GameSnapshot;
import org.junit.jupiter.api.Test;

class SceneTest {

    static final Kind PLAYER = Kind.of("player");

    static GameSnapshot snapshot() {
        return TestGraph.session(
                        GameConfig.standard(),
                        new FixedFloorSource("#######", "#@..g.#", "#######"))
                .newGame(1)
                .snapshot();
    }

    @Test
    void syncWith_hasThePlayerLastAndTheVisibleEnemies() {
        Scene scene = new Scene(PLAYER);

        scene.syncWith(snapshot());

        assertThat(scene.sprites())
                .extracting(Sprite::kind)
                .containsExactly(Kind.of("goblin"), PLAYER);
        assertThat(scene.sprites().get(1).x()).isEqualTo(1f);
        assertThat(scene.sprites().get(0).x()).isEqualTo(4f);
    }

    @Test
    void syncWith_removesWhatIsNoLongerInTheSnapshot() {
        Scene scene = new Scene(PLAYER);
        scene.spawn(new EntityId(99), Kind.of("goblin"), new Position(2, 2));

        scene.syncWith(snapshot());

        assertThat(scene.sprite(new EntityId(99))).isEmpty();
    }

    @Test
    void syncWith_keepsFacingAndStepsOfWhoWasAlreadyThere() {
        Scene scene = new Scene(PLAYER);
        GameSnapshot snapshot = snapshot();
        scene.syncWith(snapshot);
        Sprite hero = scene.sprite(snapshot.player().id()).orElseThrow();
        hero.face(Direction.LEFT);
        hero.countStep();
        hero.setWalkFrame(2);

        scene.syncWith(snapshot);

        Sprite again = scene.sprite(snapshot.player().id()).orElseThrow();
        assertThat(again.facing()).isEqualTo(Direction.LEFT);
        assertThat(again.steps()).isEqualTo(1);
        assertThat(again.walkFrame()).isEqualTo(-1);
    }
}
