package io.github.matteocardarelli.simpleroguelike.ai;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.dungeon.Enemy;
import io.github.matteocardarelli.simpleroguelike.dungeon.EntityIdSequence;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorBuilder;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorFixtures;
import io.github.matteocardarelli.simpleroguelike.dungeon.Footprint;
import io.github.matteocardarelli.simpleroguelike.dungeon.JumpPhase;
import io.github.matteocardarelli.simpleroguelike.dungeon.Player;
import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.random.FixedRandomSource;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Lo slime nella sua stanza vera: un cerchio di 8 celle di diametro con l'angolo in (1, 1), la
 * porta in (5, 9) e il corridoio sotto, dove sta il player all'inizio.
 */
class SlimeBehaviorTest {

    private static final Footprint ROOM = Footprint.circle(8);
    private static final Position ROOM_AT = new Position(1, 1);
    private static final Position DOOR = new Position(5, 9);
    private static final Position CORRIDOR = new Position(5, 10);
    private static final SlimeBehavior SLIME_AI = new SlimeBehavior();

    private static Floor room(Position playerAt) {
        List<Position> room = ROOM.at(ROOM_AT);
        Grid<TileType> terrain =
                Grid.of(
                        10,
                        12,
                        cell ->
                                room.contains(cell) || cell.equals(DOOR) || cell.equals(CORRIDOR)
                                        ? TileType.FLOOR
                                        : TileType.WALL);
        EntityIdSequence ids = new EntityIdSequence();
        Player player =
                new Player(
                        ids.next(),
                        FloorFixtures.PLAYER_STATS,
                        FloorFixtures.PLAYER_FIRST_THRESHOLD,
                        FloorFixtures.INVENTORY_CAPACITY);
        return new FloorBuilder()
                .terrain(terrain)
                .player(player, playerAt)
                .enemy(new Enemy(ids.next(), FloorFixtures.giantSlime()), new Position(3, 3))
                .bossRoom(room)
                .build();
    }

    private static Enemy slime(Floor floor) {
        return floor.enemies().get(0);
    }

    @Test
    void everyCellOfTheRoom_hasAShadowInsideTheRoom_withThePlayerOnItsBorder() {
        Floor floor = room(CORRIDOR);
        Enemy slime = slime(floor);

        for (Position player : ROOM.at(ROOM_AT)) {
            Position anchor = SlimeBehavior.shadow(slime, floor, player).orElseThrow();
            List<Position> shadow = slime.footprint().at(anchor);

            assertThat(shadow).contains(player);
            assertThat(floor.bossRoom().positions()).containsAll(shadow);
            assertThat(Direction.values())
                    .anyMatch(direction -> !shadow.contains(player.translate(direction)));
        }
    }

    @Test
    void shadow_inTheMiddle_leavesTwoWaysOut_againstAWallOne_inANarrowEdgeNone() {
        Floor floor = room(CORRIDOR);
        Enemy slime = slime(floor);

        assertThat(exits(floor, slime, new Position(5, 5))).isEqualTo(2);
        assertThat(exits(floor, slime, new Position(1, 4))).isEqualTo(1);
        assertThat(exits(floor, slime, new Position(3, 1))).isZero();
    }

    /** I passi che portano il player fuori dall'ombra scelta, su una cella del pavimento. */
    private static long exits(Floor floor, Enemy slime, Position player) {
        List<Position> shadow =
                slime.footprint().at(SlimeBehavior.shadow(slime, floor, player).orElseThrow());
        int count = 0;
        for (Direction direction : Direction.values()) {
            Position next = player.translate(direction);
            if (!shadow.contains(next) && floor.isWalkable(next)) {
                count++;
            }
        }
        return count;
    }

    @Test
    void cycle_startsWhenThePlayerEntersTheRoom_andWaitsOutside() {
        Floor floor = room(CORRIDOR);
        Enemy slime = slime(floor);

        assertThat(decide(floor)).isEqualTo(new WaitIntent());
        floor.move(floor.playerId(), Direction.UP);
        assertThat(decide(floor)).isEqualTo(new WaitIntent());
        floor.move(floor.playerId(), Direction.UP);

        assertThat(decide(floor)).isEqualTo(new RestIntent());
        floor.rest(slime.id());
        assertThat(decide(floor)).isEqualTo(new ChargeIntent());
        floor.chargeJump(slime.id());
        floor.move(floor.playerId(), Direction.DOWN);
        assertThat(decide(floor)).isEqualTo(new WaitIntent());
        assertThat(slime.jumpPhase()).isEqualTo(JumpPhase.CHARGING);
        floor.move(floor.playerId(), Direction.UP);

        EnemyIntent jump = decide(floor);

        assertThat(jump).isInstanceOf(JumpIntent.class);
        assertThat(slime.footprint().at(((JumpIntent) jump).anchor()))
                .contains(floor.positionOf(floor.playerId()));
    }

    private static EnemyIntent decide(Floor floor) {
        return SLIME_AI.decide(slime(floor), floor, new FixedRandomSource(1));
    }
}
