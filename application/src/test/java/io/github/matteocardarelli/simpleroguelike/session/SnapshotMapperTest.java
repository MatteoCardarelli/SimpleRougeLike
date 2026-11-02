package io.github.matteocardarelli.simpleroguelike.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorFixtures;
import io.github.matteocardarelli.simpleroguelike.model.CellSet;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.snapshot.ActorView;
import io.github.matteocardarelli.simpleroguelike.snapshot.CellView;
import io.github.matteocardarelli.simpleroguelike.snapshot.FixtureView;
import io.github.matteocardarelli.simpleroguelike.snapshot.GameSnapshot;
import io.github.matteocardarelli.simpleroguelike.snapshot.InventoryEntry;
import io.github.matteocardarelli.simpleroguelike.snapshot.ItemView;
import io.github.matteocardarelli.simpleroguelike.snapshot.Phase;
import io.github.matteocardarelli.simpleroguelike.snapshot.PlayerView;
import io.github.matteocardarelli.simpleroguelike.snapshot.Visibility;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SnapshotMapperTest {

    private static final SnapshotMapper MAPPER = new SnapshotMapper();

    @Test
    void from_showsOnlyWhatIsVisibleNow() {
        Floor floor = FloorFixtures.parse("#@!g.!g#");
        floor.updateVisibility(visible(floor, 0, 1, 2, 3));
        floor.updateVisibility(visible(floor, 0, 1, 2, 3, 4));

        GameSnapshot snapshot = MAPPER.from(floor, Phase.PLAYING, 9, 4);

        assertThat(snapshot.seed()).isEqualTo(9);
        assertThat(snapshot.turn()).isEqualTo(4);
        assertThat(snapshot.phase()).isEqualTo(Phase.PLAYING);
        assertThat(snapshot.visibleEnemies())
                .containsExactly(
                        new ActorView(
                                new EntityId(3), Kind.of("goblin"), new Position(3, 0), 30, 30));
        assertThat(snapshot.visibleItems())
                .containsExactly(
                        new ItemView(new EntityId(2), Kind.of("potion"), new Position(2, 0)));
    }

    @Test
    void from_cellsTellWallsAndVisibility() {
        Floor floor = FloorFixtures.parse("#@..g#");
        floor.updateVisibility(visible(floor, 0, 1, 2));
        floor.updateVisibility(visible(floor, 0, 1));

        GameSnapshot snapshot = MAPPER.from(floor, Phase.PLAYING, 1, 1);

        assertThat(snapshot.cells().get(new Position(0, 0)))
                .isEqualTo(new CellView(true, Visibility.VISIBLE));
        assertThat(snapshot.cells().get(new Position(2, 0)))
                .isEqualTo(new CellView(false, Visibility.EXPLORED));
        assertThat(snapshot.cells().get(new Position(3, 0)))
                .isEqualTo(new CellView(false, Visibility.UNEXPLORED));
    }

    @Test
    void from_playerAndInventory() {
        Floor floor = FloorFixtures.parse("@!g");
        floor.move(new EntityId(1), Direction.RIGHT);
        floor.pickUp(new EntityId(1));
        floor.damage(new EntityId(1), 7);

        GameSnapshot snapshot = MAPPER.from(floor, Phase.GAME_OVER, 1, 2);

        assertThat(snapshot.player())
                .isEqualTo(
                        new PlayerView(
                                new EntityId(1),
                                new Position(1, 0),
                                93,
                                100,
                                1,
                                0,
                                100,
                                10,
                                0,
                                0,
                                0));
        assertThat(snapshot.inventory())
                .containsExactly(new InventoryEntry(new EntityId(2), Kind.of("potion")));
    }

    @Test
    void from_fixtures_onlyInExploredCells_withTheirState() {
        Floor floor = FloorFixtures.parse("@+.$");
        floor.updateVisibility(visible(floor, 0, 1));

        GameSnapshot snapshot = MAPPER.from(floor, Phase.PLAYING, 1, 1);

        assertThat(snapshot.fixtures())
                .containsExactly(new FixtureView(new Position(1, 0), Kind.of("door"), false));
    }

    @Test
    void from_deadEnemiesAreNotShown() {
        Floor floor = FloorFixtures.parse("@g");
        floor.updateVisibility(visible(floor, 0, 1));
        floor.damage(new EntityId(2), 1000);

        assertThat(MAPPER.from(floor, Phase.VICTORY, 1, 1).visibleEnemies()).isEmpty();
    }

    @Test
    void snapshot_isImmutable() {
        Floor floor = FloorFixtures.parse("@!g");
        floor.updateVisibility(visible(floor, 0, 1, 2));

        GameSnapshot snapshot = MAPPER.from(floor, Phase.PLAYING, 1, 1);

        assertThatThrownBy(() -> snapshot.visibleEnemies().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> snapshot.visibleItems().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> snapshot.inventory().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void turnResult_copiesEvents() {
        Floor floor = FloorFixtures.parse("@g");
        TurnResult result =
                new TurnResult(1, true, List.of(), MAPPER.from(floor, Phase.PLAYING, 1, 1));

        assertThatThrownBy(() -> result.events().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static CellSet visible(Floor floor, int... xs) {
        List<Position> cells = new ArrayList<>();
        for (int x : xs) {
            cells.add(new Position(x, 0));
        }
        return CellSet.of(floor.terrain().width(), floor.terrain().height(), cells);
    }
}
