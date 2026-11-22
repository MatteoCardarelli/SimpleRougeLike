package io.github.matteocardarelli.simpleroguelike.dungeon;

import static io.github.matteocardarelli.simpleroguelike.random.FixedRandomSource.ALWAYS_HIT;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.event.Damaged;
import io.github.matteocardarelli.simpleroguelike.event.Died;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.event.Poisoned;
import io.github.matteocardarelli.simpleroguelike.event.TrapDiscovered;
import io.github.matteocardarelli.simpleroguelike.event.TrapTriggered;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import io.github.matteocardarelli.simpleroguelike.random.FixedRandomSource;
import io.github.matteocardarelli.simpleroguelike.random.SeededRandomSource;
import io.github.matteocardarelli.simpleroguelike.rules.ActionRules;
import io.github.matteocardarelli.simpleroguelike.rules.CombatRules;
import io.github.matteocardarelli.simpleroguelike.rules.DamageRules;
import io.github.matteocardarelli.simpleroguelike.rules.LevelingRules;
import io.github.matteocardarelli.simpleroguelike.rules.TrapRules;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrapsTest {

    private static final EntityId PLAYER = new EntityId(1);
    private static final DamageRules DAMAGE =
            new DamageRules(new LevelingRules(new Stats(20, 2), 150));
    private static final ActionRules ACTIONS = new ActionRules(new CombatRules(DAMAGE));
    private static final TrapRules TRAPS = new TrapRules(DAMAGE);
    private static final Kind SPIKES = Kind.of("trap_spikes");
    private static final Kind DART = Kind.of("trap_dart");

    @Test
    void enteringSpikes_hurtsForEightDamage_andSpendsTheTrap() {
        Floor floor = FloorFixtures.parse("@^.");
        Position trap = new Position(1, 0);

        var result = ACTIONS.step(floor, PLAYER, Direction.RIGHT, ALWAYS_HIT);

        assertThat(result.events())
                .contains(new TrapTriggered(SPIKES, trap, 8), new Damaged(PLAYER, trap, 8, 92));
        assertThat(floor.player().health()).isEqualTo(92);
        assertThat(floor.player().isPoisoned()).isFalse();
        assertThat(floor.fixtureAt(trap).orElseThrow().isOpen()).isTrue();
        assertThat(floor.fixtureAt(trap).orElseThrow().isHidden()).isFalse();
    }

    @Test
    void enteringADart_hurtsForThree_andPoisons() {
        Floor floor = FloorFixtures.parse("@v.");
        Position trap = new Position(1, 0);

        var result = ACTIONS.step(floor, PLAYER, Direction.RIGHT, ALWAYS_HIT);

        assertThat(result.events())
                .contains(
                        new TrapTriggered(DART, trap, 3),
                        new Damaged(PLAYER, trap, 3, 97),
                        new Poisoned(PLAYER, trap, Floor.POISON_TURNS));
        assertThat(floor.player().isPoisoned()).isTrue();
    }

    @Test
    void aTrap_triggersOnlyOnce() {
        Floor floor = FloorFixtures.parse("@^.");
        ACTIONS.step(floor, PLAYER, Direction.RIGHT, ALWAYS_HIT);
        ACTIONS.step(floor, PLAYER, Direction.RIGHT, ALWAYS_HIT);

        var back = ACTIONS.step(floor, PLAYER, Direction.LEFT, ALWAYS_HIT);

        assertThat(back.events()).noneMatch(e -> e instanceof TrapTriggered);
        assertThat(floor.player().health()).isEqualTo(92);
    }

    @Test
    void aDiscoveredTrap_stillTriggers() {
        Floor floor = FloorFixtures.parse("@^.");
        floor.discoverTrap(new Position(1, 0));

        ACTIONS.step(floor, PLAYER, Direction.RIGHT, ALWAYS_HIT);

        assertThat(floor.player().health()).isEqualTo(92);
    }

    @Test
    void enemies_walkOverTrapsWithoutTriggeringThem() {
        Floor floor = FloorFixtures.parse("@.^g");
        Position trap = new Position(2, 0);
        EntityId goblin = new EntityId(2);
        int health = floor.enemy(goblin).orElseThrow().health();

        var result = ACTIONS.step(floor, goblin, Direction.LEFT, ALWAYS_HIT);

        assertThat(result.performed()).isTrue();
        assertThat(floor.positionOf(goblin)).isEqualTo(trap);
        assertThat(result.events()).noneMatch(e -> e instanceof TrapTriggered);
        assertThat(floor.fixtureAt(trap).orElseThrow().isOpen()).isFalse();
        assertThat(floor.fixtureAt(trap).orElseThrow().isHidden()).isTrue();
        assertThat(floor.enemy(goblin).orElseThrow().health()).isEqualTo(health);
    }

    @Test
    void aTrapThatKillsThePlayer_emitsDied() {
        Floor floor = FloorFixtures.parse("@^");
        floor.damage(PLAYER, 95);

        var result = ACTIONS.step(floor, PLAYER, Direction.RIGHT, ALWAYS_HIT);

        assertThat(floor.player().isAlive()).isFalse();
        assertThat(result.events()).anyMatch(e -> e instanceof Died);
    }

    @Test
    void discover_diagonalTraps_areNeverFound() {
        Floor floor = FloorFixtures.parse("^.^", ".@.", "^.^");

        assertThat(TRAPS.discover(floor, ALWAYS_HIT)).isEmpty();
    }

    @Test
    void discover_adjacentTraps_areFoundWhenTheRollIsZero_andNotOtherwise() {
        Floor floor = FloorFixtures.parse(".^.", "^@v", ".^.");

        assertThat(TRAPS.discover(floor, new FixedRandomSource(1))).isEmpty();

        List<GameEvent> events = TRAPS.discover(floor, ALWAYS_HIT);

        assertThat(events)
                .containsExactlyInAnyOrder(
                        new TrapDiscovered(SPIKES, new Position(1, 0)),
                        new TrapDiscovered(SPIKES, new Position(0, 1)),
                        new TrapDiscovered(DART, new Position(2, 1)),
                        new TrapDiscovered(SPIKES, new Position(1, 2)));
        assertThat(floor.fixtureAt(new Position(1, 0)).orElseThrow().isHidden()).isFalse();
        assertThat(TRAPS.discover(floor, ALWAYS_HIT)).isEmpty();
    }

    @Test
    void discover_farTraps_staySilent() {
        Floor floor = FloorFixtures.parse("..^..", ".....", "..@..", "..^..", "..^..");

        TRAPS.discover(floor, ALWAYS_HIT);

        assertThat(floor.fixtureAt(new Position(2, 0)).orElseThrow().isHidden()).isTrue();
        assertThat(floor.fixtureAt(new Position(2, 3)).orElseThrow().isHidden()).isFalse();
        assertThat(floor.fixtureAt(new Position(2, 4)).orElseThrow().isHidden()).isTrue();
    }

    @Test
    void discover_roughlyOneInThree_andDeterministicFromTheSeed() {
        int found = 0;
        int rounds = 3000;
        for (int seed = 0; seed < rounds; seed++) {
            found += TRAPS.discover(FloorFixtures.parse("@^"), new SeededRandomSource(seed)).size();
        }

        assertThat(found).isBetween(rounds / 3 - 150, rounds / 3 + 150);
        List<List<GameEvent>> first = new ArrayList<>();
        List<List<GameEvent>> second = new ArrayList<>();
        for (List<List<GameEvent>> into : List.of(first, second)) {
            for (int seed = 0; seed < 50; seed++) {
                into.add(TRAPS.discover(FloorFixtures.parse("@^"), new SeededRandomSource(seed)));
            }
        }
        assertThat(first).isEqualTo(second);
    }

    @Test
    void aSpentTrap_isNotDiscoveredAgain() {
        Floor floor = FloorFixtures.parse("@^.");
        ACTIONS.step(floor, PLAYER, Direction.RIGHT, ALWAYS_HIT);
        ACTIONS.step(floor, PLAYER, Direction.RIGHT, ALWAYS_HIT);

        assertThat(TRAPS.discover(floor, ALWAYS_HIT)).isEmpty();
        assertThat(floor.discoverTrap(new Position(1, 0))).isEmpty();
    }

    @Test
    void trapsDoNotBlockMovementOrSight() {
        Floor floor = FloorFixtures.parse("@^.");

        assertThat(floor.isWalkable(new Position(1, 0))).isTrue();
        assertThat(floor.sightGrid().get(new Position(1, 0))).isEqualTo(TileType.FLOOR);
    }
}
