package io.github.matteocardarelli.simpleroguelike.dungeon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import org.junit.jupiter.api.Test;

class FloorBuilderTest {

    private static final Grid<TileType> TERRAIN = FloorFixtures.terrain("#...");

    @Test
    void build_withoutTerrain_isRejected() {
        FloorBuilder builder = new FloorBuilder().player(player(1), new Position(1, 0));

        assertThatThrownBy(builder::build).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void build_withoutPlayer_isRejected() {
        FloorBuilder builder = new FloorBuilder().terrain(TERRAIN);

        assertThatThrownBy(builder::build).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void player_twice_isRejected() {
        FloorBuilder builder = new FloorBuilder().player(player(1), new Position(1, 0));

        assertThatThrownBy(() -> builder.player(player(2), new Position(2, 0)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void twoActorsInTheSameCell_areRejected() {
        FloorBuilder builder = new FloorBuilder().player(player(1), new Position(1, 0));

        assertThatThrownBy(() -> builder.enemy(goblin(2), new Position(1, 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void twoItemsInTheSameCell_areRejected() {
        FloorBuilder builder = new FloorBuilder().item(potion(1), new Position(1, 0));

        assertThatThrownBy(() -> builder.item(potion(2), new Position(1, 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void duplicateIds_areRejected() {
        FloorBuilder builder = new FloorBuilder().player(player(1), new Position(1, 0));

        assertThatThrownBy(() -> builder.enemy(goblin(1), new Position(2, 0)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.item(potion(1), new Position(2, 0)))
                .isInstanceOf(IllegalArgumentException.class);
        builder.item(potion(5), new Position(2, 0));
        assertThatThrownBy(() -> builder.enemy(goblin(5), new Position(3, 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void actorOnWall_isRejected() {
        FloorBuilder builder =
                new FloorBuilder().terrain(TERRAIN).player(player(1), new Position(0, 0));

        assertThatThrownBy(builder::build).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void actorOutsideTerrain_isRejected() {
        FloorBuilder builder =
                new FloorBuilder().terrain(TERRAIN).player(player(1), new Position(4, 0));

        assertThatThrownBy(builder::build).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void itemOnWall_isRejected() {
        FloorBuilder builder =
                new FloorBuilder()
                        .terrain(TERRAIN)
                        .player(player(1), new Position(1, 0))
                        .item(potion(2), new Position(0, 0));

        assertThatThrownBy(builder::build).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void build_copiesWhatItReceived() {
        FloorBuilder builder =
                new FloorBuilder().terrain(TERRAIN).player(player(1), new Position(1, 0));
        Floor first = builder.build();

        builder.enemy(goblin(2), new Position(2, 0));

        assertThat(first.enemies()).isEmpty();
        assertThat(builder.build().enemies()).hasSize(1);
    }

    @Test
    void itemOnPlayerCell_isAllowed() {
        Floor floor =
                new FloorBuilder()
                        .terrain(TERRAIN)
                        .player(player(1), new Position(1, 0))
                        .item(potion(2), new Position(1, 0))
                        .build();

        assertThat(floor.itemAt(new Position(1, 0))).isPresent();
    }

    private static Player player(int id) {
        return new Player(new EntityId(id), new Stats(10, 1), 10, 3);
    }

    private static Enemy goblin(int id) {
        return new Enemy(new EntityId(id), FloorFixtures.goblin());
    }

    private static Item potion(int id) {
        return new Item(new EntityId(id), FloorFixtures.potion());
    }
}
