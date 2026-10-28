package io.github.matteocardarelli.simpleroguelike.dungeon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.matteocardarelli.simpleroguelike.event.Damaged;
import io.github.matteocardarelli.simpleroguelike.event.Died;
import io.github.matteocardarelli.simpleroguelike.event.Healed;
import io.github.matteocardarelli.simpleroguelike.event.InventoryFull;
import io.github.matteocardarelli.simpleroguelike.event.ItemPickedUp;
import io.github.matteocardarelli.simpleroguelike.event.ItemUsed;
import io.github.matteocardarelli.simpleroguelike.event.Moved;
import io.github.matteocardarelli.simpleroguelike.model.CellSet;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import io.github.matteocardarelli.simpleroguelike.random.SeededRandomSource;
import java.util.List;
import org.junit.jupiter.api.Test;

class FloorTest {

    private static final EntityId PLAYER = new EntityId(1);
    private static final EntityId GOBLIN = new EntityId(2);
    private static final LevelingPolicy LEVELING =
            new LevelingPolicy() {
                @Override
                public Stats bonusPerLevel() {
                    return new Stats(20, 2);
                }

                @Override
                public int nextThreshold(int current) {
                    return current * 2;
                }
            };

    @Test
    void move_toFreeCell_movesAndReturnsMoved() {
        Floor floor = FloorFixtures.parse("#####", "#@..#", "#####");

        Moved moved = floor.move(PLAYER, Direction.RIGHT);

        assertThat(moved)
                .isEqualTo(new Moved(PLAYER, Player.KIND, new Position(1, 1), new Position(2, 1)));
        assertThat(floor.positionOf(PLAYER)).isEqualTo(new Position(2, 1));
    }

    @Test
    void move_intoWall_throws() {
        Floor floor = FloorFixtures.parse("###", "#@#", "###");

        assertThatThrownBy(() -> floor.move(PLAYER, Direction.UP))
                .isInstanceOf(IllegalStateException.class);
        assertThat(floor.positionOf(PLAYER)).isEqualTo(new Position(1, 1));
    }

    @Test
    void move_intoLiveActor_throws() {
        Floor floor = FloorFixtures.parse("#@g#");

        assertThatThrownBy(() -> floor.move(PLAYER, Direction.RIGHT))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void move_outsideTerrain_throws() {
        Floor floor = FloorFixtures.parse("@.");

        assertThatThrownBy(() -> floor.move(PLAYER, Direction.LEFT))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void move_deadActor_throws() {
        Floor floor = FloorFixtures.parse("@g.");
        floor.damage(GOBLIN, 1000);

        assertThatThrownBy(() -> floor.move(GOBLIN, Direction.RIGHT))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void damage_notLethal_emitsDamagedOnly() {
        Floor floor = FloorFixtures.parse("@g");

        DamageResult result = floor.damage(GOBLIN, 10);

        assertThat(result.applied()).isEqualTo(10);
        assertThat(result.remainingHealth()).isEqualTo(20);
        assertThat(result.killed()).isFalse();
        assertThat(result.events())
                .containsExactly(new Damaged(GOBLIN, new Position(1, 0), 10, 20));
    }

    @Test
    void damage_lethal_emitsDamagedThenDied_andClampsToRemainingHealth() {
        Floor floor = FloorFixtures.parse("@g");

        DamageResult result = floor.damage(GOBLIN, 100);

        assertThat(result.applied()).isEqualTo(30);
        assertThat(result.killed()).isTrue();
        assertThat(result.events())
                .containsExactly(
                        new Damaged(GOBLIN, new Position(1, 0), 30, 0),
                        new Died(GOBLIN, Kind.of("goblin"), new Position(1, 0)));
    }

    @Test
    void damage_zero_emitsNothing() {
        Floor floor = FloorFixtures.parse("@g");

        DamageResult result = floor.damage(GOBLIN, 0);

        assertThat(result.events()).isEmpty();
        assertThat(result.killed()).isFalse();
        assertThat(result.remainingHealth()).isEqualTo(30);
    }

    @Test
    void damage_alreadyDead_doesNothing() {
        Floor floor = FloorFixtures.parse("@g");
        floor.damage(GOBLIN, 100);

        DamageResult again = floor.damage(GOBLIN, 5);

        assertThat(again).isEqualTo(new DamageResult(0, 0, false, List.of()));
    }

    @Test
    void damage_negative_isRejected() {
        Floor floor = FloorFixtures.parse("@g");

        assertThatThrownBy(() -> floor.damage(GOBLIN, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void damage_unknownActor_isRejected() {
        Floor floor = FloorFixtures.parse("@g");

        assertThatThrownBy(() -> floor.damage(new EntityId(99), 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deadActor_doesNotExistForTheFloor() {
        Floor floor = FloorFixtures.parse("@g");

        floor.damage(GOBLIN, 100);

        assertThat(floor.actorAt(new Position(1, 0))).isEmpty();
        assertThat(floor.isFree(new Position(1, 0))).isTrue();
        assertThat(floor.enemies()).isEmpty();
        assertThat(floor.enemy(GOBLIN)).isPresent();
        assertThat(floor.positionOf(GOBLIN)).isEqualTo(new Position(1, 0));
    }

    @Test
    void heal_restoresUpToMaxHealth() {
        Floor floor = FloorFixtures.parse("@g");
        floor.damage(PLAYER, 10);

        assertThat(floor.heal(PLAYER, 25)).contains(new Healed(PLAYER, new Position(0, 0), 10));
        assertThat(floor.player().health()).isEqualTo(100);
    }

    @Test
    void heal_fullHealth_emitsNothing() {
        Floor floor = FloorFixtures.parse("@");

        assertThat(floor.heal(PLAYER, 25)).isEmpty();
    }

    @Test
    void heal_dead_emitsNothing_andStaysDead() {
        Floor floor = FloorFixtures.parse("@g");
        floor.damage(GOBLIN, 100);

        assertThat(floor.heal(GOBLIN, 25)).isEmpty();
        assertThat(floor.enemy(GOBLIN).orElseThrow().isAlive()).isFalse();
    }

    @Test
    void heal_negative_isRejected() {
        Floor floor = FloorFixtures.parse("@");

        assertThatThrownBy(() -> floor.heal(PLAYER, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pickUp_item_movesItToInventory() {
        Floor floor = FloorFixtures.parse("@!");
        floor.move(PLAYER, Direction.RIGHT);

        var event = floor.pickUp(PLAYER);

        assertThat(event)
                .contains(new ItemPickedUp(new EntityId(2), Kind.of("potion"), new Position(1, 0)));
        assertThat(floor.itemAt(new Position(1, 0))).isEmpty();
        assertThat(floor.player().inventory().items())
                .extracting(Item::id)
                .containsExactly(new EntityId(2));
    }

    @Test
    void pickUp_nothingOnTheGround_isEmpty() {
        Floor floor = FloorFixtures.parse("@.");

        assertThat(floor.pickUp(PLAYER)).isEmpty();
    }

    @Test
    void pickUp_inventoryFull_leavesItemOnGround() {
        Floor floor = FloorFixtures.parse(1, "@!!");
        floor.move(PLAYER, Direction.RIGHT);
        floor.pickUp(PLAYER);
        floor.move(PLAYER, Direction.RIGHT);

        var event = floor.pickUp(PLAYER);

        assertThat(event)
                .contains(
                        new InventoryFull(new EntityId(3), Kind.of("potion"), new Position(2, 0)));
        assertThat(floor.itemAt(new Position(2, 0))).isPresent();
        assertThat(floor.player().inventory().items()).hasSize(1);
    }

    @Test
    void pickUp_notThePlayer_isRejected() {
        Floor floor = FloorFixtures.parse("@g");

        assertThatThrownBy(() -> floor.pickUp(GOBLIN)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void useItem_consumable_appliesEffectAndRemovesIt() {
        Floor floor = playerWithPotion();
        floor.damage(PLAYER, 40);

        var events = floor.useItem(PLAYER, new EntityId(2), new SeededRandomSource(1));

        assertThat(events)
                .containsExactly(
                        new ItemUsed(new EntityId(2), Kind.of("potion"), new Position(1, 0)),
                        new Healed(PLAYER, new Position(1, 0), 25));
        assertThat(floor.player().health()).isEqualTo(85);
        assertThat(floor.player().inventory().items()).isEmpty();
    }

    @Test
    void useItem_notConsumable_staysInInventory() {
        ItemDefinition charm = new ItemDefinition(Kind.of("charm"), context -> List.of(), false, 1);
        Floor floor =
                new FloorBuilder()
                        .terrain(FloorFixtures.terrain(".."))
                        .player(new Player(PLAYER, new Stats(10, 1), 10, 3), new Position(0, 0))
                        .item(new Item(new EntityId(2), charm), new Position(1, 0))
                        .build();
        floor.move(PLAYER, Direction.RIGHT);
        floor.pickUp(PLAYER);

        var events = floor.useItem(PLAYER, new EntityId(2), new SeededRandomSource(1));

        assertThat(events).hasSize(1);
        assertThat(floor.player().inventory().items()).hasSize(1);
    }

    @Test
    void useItem_effectReceivesUserFloorAndRandom() {
        var random = new SeededRandomSource(1);
        EffectContext[] seen = new EffectContext[1];
        ItemDefinition probe =
                new ItemDefinition(
                        Kind.of("probe"),
                        context -> {
                            seen[0] = context;
                            return List.of();
                        },
                        true,
                        1);
        Floor floor =
                new FloorBuilder()
                        .terrain(FloorFixtures.terrain(".."))
                        .player(new Player(PLAYER, new Stats(10, 1), 10, 3), new Position(0, 0))
                        .item(new Item(new EntityId(2), probe), new Position(0, 0))
                        .build();
        floor.pickUp(PLAYER);

        floor.useItem(PLAYER, new EntityId(2), random);

        assertThat(seen[0]).isEqualTo(new EffectContext(PLAYER, floor, random));
    }

    @Test
    void useItem_notInInventory_isRejected() {
        Floor floor = FloorFixtures.parse("@!");

        assertThatThrownBy(() -> floor.useItem(PLAYER, new EntityId(2), new SeededRandomSource(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void useItem_byDeadPlayer_isRejected() {
        Floor floor = playerWithPotion();
        floor.damage(PLAYER, 1000);

        assertThatThrownBy(() -> floor.useItem(PLAYER, new EntityId(2), new SeededRandomSource(1)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(floor.player().health()).isZero();
    }

    @Test
    void useItem_byEnemy_isRejected() {
        Floor floor = FloorFixtures.parse("@g");

        assertThatThrownBy(() -> floor.useItem(GOBLIN, new EntityId(3), new SeededRandomSource(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void grantExperience_levelsUpAsManyTimesAsNeeded() {
        Floor floor = FloorFixtures.parse("@");

        int levels = floor.grantExperience(350, LEVELING);

        Player player = floor.player();
        assertThat(levels).isEqualTo(2);
        assertThat(player.level()).isEqualTo(3);
        assertThat(player.experience()).isEqualTo(50);
        assertThat(player.experienceToNextLevel()).isEqualTo(400);
        assertThat(player.stats()).isEqualTo(new Stats(140, 14));
        assertThat(player.health()).isEqualTo(140);
    }

    @Test
    void grantExperience_belowThreshold_onlyAccumulates() {
        Floor floor = FloorFixtures.parse("@");

        assertThat(floor.grantExperience(99, LEVELING)).isZero();
        assertThat(floor.player().experience()).isEqualTo(99);
        assertThat(floor.player().level()).isEqualTo(1);
    }

    @Test
    void grantExperience_exactThreshold_levelsUp() {
        Floor floor = FloorFixtures.parse("@");

        assertThat(floor.grantExperience(100, LEVELING)).isEqualTo(1);
        assertThat(floor.player().experience()).isZero();
    }

    @Test
    void grantExperience_deadPlayer_givesNothing() {
        Floor floor = FloorFixtures.parse("@");
        floor.damage(PLAYER, 1000);

        assertThat(floor.grantExperience(500, LEVELING)).isZero();
        assertThat(floor.player().experience()).isZero();
    }

    @Test
    void grantExperience_policyThatDoesNotGrow_isRejected() {
        Floor floor = FloorFixtures.parse("@");
        LevelingPolicy flat =
                new LevelingPolicy() {
                    @Override
                    public Stats bonusPerLevel() {
                        return new Stats(0, 0);
                    }

                    @Override
                    public int nextThreshold(int current) {
                        return current;
                    }
                };

        assertThatThrownBy(() -> floor.grantExperience(100, flat))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void grantExperience_negative_isRejected() {
        Floor floor = FloorFixtures.parse("@");

        assertThatThrownBy(() -> floor.grantExperience(-1, LEVELING))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void removeDead_removesOnlyDeadEnemies() {
        Floor floor = FloorFixtures.parse("@gg");
        floor.damage(GOBLIN, 100);

        List<Enemy> removed = floor.removeDead();

        assertThat(removed).extracting(Enemy::id).containsExactly(GOBLIN);
        assertThat(floor.enemy(GOBLIN)).isEmpty();
        assertThat(floor.enemies()).extracting(Enemy::id).containsExactly(new EntityId(3));
        assertThatThrownBy(() -> floor.positionOf(GOBLIN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void removeDead_keepsDeadPlayer() {
        Floor floor = FloorFixtures.parse("@g");
        floor.damage(PLAYER, 1000);

        floor.removeDead();

        assertThat(floor.positionOf(PLAYER)).isEqualTo(new Position(0, 0));
        assertThat(floor.actor(PLAYER)).isPresent();
    }

    @Test
    void updateVisibility_replacesVisibleAndGrowsExplored() {
        Floor floor = FloorFixtures.parse("@...");
        CellSet first = CellSet.of(4, 1, List.of(new Position(0, 0), new Position(1, 0)));
        CellSet second = CellSet.of(4, 1, List.of(new Position(3, 0)));

        floor.updateVisibility(first);
        CellSet visibleBefore = floor.visible();
        floor.updateVisibility(second);

        assertThat(visibleBefore).isEqualTo(first);
        assertThat(floor.visible()).isEqualTo(second);
        assertThat(floor.explored().positions())
                .containsExactly(new Position(0, 0), new Position(1, 0), new Position(3, 0));
        assertThat(floor.isExplored(new Position(1, 0))).isTrue();
        assertThat(floor.isExplored(new Position(2, 0))).isFalse();
    }

    @Test
    void updateVisibility_wrongDimensions_isRejected() {
        Floor floor = FloorFixtures.parse("@...");

        assertThatThrownBy(() -> floor.updateVisibility(CellSet.empty(3, 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> floor.updateVisibility(CellSet.empty(4, 2)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void newFloor_seesNothingYet() {
        Floor floor = FloorFixtures.parse("@.");

        assertThat(floor.visible().isEmpty()).isTrue();
        assertThat(floor.explored().isEmpty()).isTrue();
    }

    @Test
    void enemies_areInIdOrder() {
        Floor floor = FloorFixtures.parse("g@", "gg");

        assertThat(floor.enemies())
                .extracting(Enemy::id)
                .containsExactly(new EntityId(1), new EntityId(3), new EntityId(4));
    }

    @Test
    void getters_doNotExposeInternalState() {
        Floor floor = FloorFixtures.parse("@!g");

        assertThatThrownBy(() -> floor.enemies().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> floor.itemsOnGround().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> floor.player().inventory().items().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void readers_describeTheFloor() {
        Floor floor = FloorFixtures.parse("#@!g");

        assertThat(floor.playerId()).isEqualTo(new EntityId(1));
        assertThat(floor.terrain().get(new Position(0, 0))).isEqualTo(TileType.WALL);
        assertThat(floor.isWalkable(new Position(0, 0))).isFalse();
        assertThat(floor.isWalkable(new Position(9, 0))).isFalse();
        assertThat(floor.isFree(new Position(2, 0))).isTrue();
        assertThat(floor.isFree(new Position(3, 0))).isFalse();
        assertThat(floor.actorAt(new Position(3, 0))).map(Actor::id).contains(new EntityId(3));
        assertThat(floor.itemAt(new Position(2, 0))).map(Item::id).contains(new EntityId(2));
        assertThat(floor.itemsOnGround()).containsOnlyKeys(new Position(2, 0));
        assertThat(floor.actor(new EntityId(99))).isEmpty();
    }

    private static Floor playerWithPotion() {
        Floor floor = FloorFixtures.parse("@!");
        floor.move(PLAYER, Direction.RIGHT);
        floor.pickUp(PLAYER);
        return floor;
    }
}
