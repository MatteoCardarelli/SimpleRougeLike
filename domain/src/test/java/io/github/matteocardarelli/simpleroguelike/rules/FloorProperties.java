package io.github.matteocardarelli.simpleroguelike.rules;

import static io.github.matteocardarelli.simpleroguelike.rules.Rules.ACTIONS;
import static io.github.matteocardarelli.simpleroguelike.rules.Rules.DAMAGE;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.dungeon.Actor;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorFixtures;
import io.github.matteocardarelli.simpleroguelike.dungeon.Item;
import io.github.matteocardarelli.simpleroguelike.event.EventLogWriter;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.random.SeededRandomSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Le invarianti del piano dopo ogni azione, su sequenze casuali di passi, danni, cure, uso di
 * oggetti e rimozione dei morti.
 */
class FloorProperties {

    private static final String[] MAP = {
        "#########", "#@.!.g..#", "#.#!#.#.#", "#g..!..g#", "#..g.!..#", "#########",
    };

    /** Un'azione: chi la fa (indice nell'elenco degli id), che cosa e con quale valore. */
    record Action(int kind, int actor, Direction direction, int amount) {}

    @Property(tries = 300)
    void invariantsHoldAfterEveryAction(@ForAll("actions") List<Action> actions) {
        Floor floor = FloorFixtures.parse(2, MAP);
        List<EntityId> actors = actorIds(floor);
        TreeSet<EntityId> dead = new TreeSet<>();
        SeededRandomSource random = new SeededRandomSource(1);

        for (Action action : actions) {
            EntityId id = actors.get(action.actor() % actors.size());
            perform(floor, action, id, random);
            checkInvariants(floor, actors, dead);
        }
    }

    @Property(tries = 100)
    void sameActions_giveSameEvents(@ForAll("actions") List<Action> actions) {
        assertThat(play(actions)).isEqualTo(play(actions));
    }

    @Provide
    Arbitrary<List<Action>> actions() {
        Arbitrary<Action> action =
                Combinators.combine(
                                Arbitraries.integers().between(0, 9),
                                Arbitraries.integers().between(0, 5),
                                Arbitraries.of(Direction.class),
                                Arbitraries.integers().between(0, 60))
                        .as(Action::new);
        return action.list().ofMaxSize(60);
    }

    private static List<String> play(List<Action> actions) {
        Floor floor = FloorFixtures.parse(2, MAP);
        List<EntityId> actors = actorIds(floor);
        SeededRandomSource random = new SeededRandomSource(1);
        List<String> log = new ArrayList<>();
        EventLogWriter writer = new EventLogWriter();
        for (Action action : actions) {
            EntityId id = actors.get(action.actor() % actors.size());
            log.addAll(writer.lines(0, perform(floor, action, id, random)));
        }
        return log;
    }

    private static List<GameEvent> perform(
            Floor floor, Action action, EntityId id, SeededRandomSource random) {
        boolean alive = floor.actor(id).map(Actor::isAlive).orElse(false);
        switch (action.kind()) {
            case 0, 1, 2, 3, 4, 5 -> {
                if (alive) {
                    return ACTIONS.step(floor, id, action.direction()).events();
                }
            }
            case 6 -> {
                if (floor.actor(id).isPresent()) {
                    return DAMAGE.apply(floor, Optional.of(floor.playerId()), id, action.amount());
                }
            }
            case 7 -> {
                if (floor.actor(id).isPresent()) {
                    return floor.heal(id, action.amount())
                            .<List<GameEvent>>map(List::of)
                            .orElse(List.of());
                }
            }
            case 8 -> {
                List<Item> items = floor.player().inventory().items();
                if (floor.player().isAlive() && !items.isEmpty()) {
                    return floor.useItem(floor.playerId(), items.get(0).id(), random);
                }
            }
            default -> floor.removeDead();
        }
        return List.of();
    }

    private static void checkInvariants(Floor floor, List<EntityId> actors, Set<EntityId> dead) {
        List<Position> occupied = new ArrayList<>();
        for (EntityId id : actors) {
            Optional<Actor> found = floor.actor(id);
            if (found.isEmpty()) {
                assertThat(dead).as("solo un morto sparisce dal piano").contains(id);
                continue;
            }
            Actor actor = found.get();
            assertThat(actor.health()).isBetween(0, actor.stats().maxHealth());
            if (dead.contains(id)) {
                assertThat(actor.isAlive()).as("un morto resta morto").isFalse();
            }
            if (!actor.isAlive()) {
                dead.add(id);
                assertThat(floor.actorAt(floor.positionOf(id)).map(Actor::id))
                        .isNotEqualTo(Optional.of(id));
                continue;
            }
            Position at = floor.positionOf(id);
            assertThat(floor.isWalkable(at)).as("mai su un muro").isTrue();
            assertThat(occupied).as("mai due attori vivi nella stessa cella").doesNotContain(at);
            occupied.add(at);
        }
        assertThat(floor.enemies()).allMatch(Actor::isAlive);
        assertThat(floor.player().experience()).isLessThan(floor.player().experienceToNextLevel());
        assertThat(floor.player().inventory().items().size())
                .isLessThanOrEqualTo(floor.player().inventory().capacity());
    }

    private static List<EntityId> actorIds(Floor floor) {
        List<EntityId> ids = new ArrayList<>();
        ids.add(floor.playerId());
        floor.enemies().forEach(enemy -> ids.add(enemy.id()));
        return List.copyOf(ids);
    }
}
