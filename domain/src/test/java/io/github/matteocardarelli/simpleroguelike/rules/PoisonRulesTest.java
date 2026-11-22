package io.github.matteocardarelli.simpleroguelike.rules;

import static io.github.matteocardarelli.simpleroguelike.random.FixedRandomSource.ALWAYS_HIT;
import static io.github.matteocardarelli.simpleroguelike.random.FixedRandomSource.ALWAYS_MISS;
import static io.github.matteocardarelli.simpleroguelike.rules.Rules.COMBAT;
import static io.github.matteocardarelli.simpleroguelike.rules.Rules.POISON;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.content.StandardItemCatalog;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorFixtures;
import io.github.matteocardarelli.simpleroguelike.dungeon.Item;
import io.github.matteocardarelli.simpleroguelike.event.Damaged;
import io.github.matteocardarelli.simpleroguelike.event.Died;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.event.PoisonCured;
import io.github.matteocardarelli.simpleroguelike.event.PoisonEnded;
import io.github.matteocardarelli.simpleroguelike.event.Poisoned;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.random.SeededRandomSource;
import java.util.List;
import org.junit.jupiter.api.Test;

class PoisonRulesTest {

    private static final EntityId PLAYER = new EntityId(1);
    private static final EntityId SPIDER = new EntityId(2);
    private static final Position AT = new Position(0, 0);

    @Test
    void tick_poisonedPlayer_takesTwoDamageForFiveRounds_thenItEnds() {
        Floor floor = FloorFixtures.parse("@");
        floor.poison(PLAYER);

        for (int round = 1; round <= 5; round++) {
            List<GameEvent> events = POISON.tick(floor);

            assertThat(events).contains(new Damaged(PLAYER, AT, 2, 100 - 2 * round));
            assertThat(events.contains(new PoisonEnded(PLAYER, AT))).isEqualTo(round == 5);
        }
        assertThat(floor.player().isPoisoned()).isFalse();
        assertThat(POISON.tick(floor)).isEmpty();
        assertThat(floor.player().health()).isEqualTo(90);
    }

    @Test
    void poison_again_restartsTheTurns_withoutStackingTheDamage() {
        Floor floor = FloorFixtures.parse("@");
        floor.poison(PLAYER);
        POISON.tick(floor);
        POISON.tick(floor);
        assertThat(floor.player().poisonTurns()).isEqualTo(3);

        var again = floor.poison(PLAYER);

        assertThat(again).contains(new Poisoned(PLAYER, AT, Floor.POISON_TURNS));
        assertThat(floor.player().poisonTurns()).isEqualTo(5);
        assertThat(POISON.tick(floor)).filteredOn(Damaged.class::isInstance).hasSize(1);
        assertThat(floor.player().health()).isEqualTo(100 - 4 - 2);
    }

    @Test
    void tick_poisonCanKill_withDiedAndNoExperience() {
        Floor floor = FloorFixtures.parse("@x");
        floor.damage(SPIDER, 11);
        floor.poison(SPIDER);

        List<GameEvent> events = POISON.tick(floor);

        assertThat(events).anyMatch(Died.class::isInstance);
        assertThat(events).noneMatch(PoisonEnded.class::isInstance);
        assertThat(floor.enemy(SPIDER).orElseThrow().isAlive()).isFalse();
        assertThat(floor.player().experience()).isZero();
        assertThat(floor.poisonedActors()).isEmpty();
    }

    @Test
    void tick_poisonCanKillThePlayer() {
        Floor floor = FloorFixtures.parse("@");
        floor.damage(PLAYER, 99);
        floor.poison(PLAYER);

        assertThat(POISON.tick(floor)).anyMatch(Died.class::isInstance);
        assertThat(floor.player().isAlive()).isFalse();
    }

    @Test
    void tick_poisonsEveryoneOncePerRound() {
        Floor floor = FloorFixtures.parse("@xx");
        floor.poison(PLAYER);
        floor.poison(new EntityId(2));

        List<GameEvent> events = POISON.tick(floor);

        assertThat(events).filteredOn(Damaged.class::isInstance).hasSize(2);
        assertThat(floor.poisonedActors()).containsExactly(PLAYER, new EntityId(2));
    }

    @Test
    void spiderBite_thatHits_poisons() {
        Floor floor = FloorFixtures.parse("@x");

        List<GameEvent> events = COMBAT.resolveAttack(floor, SPIDER, PLAYER, ALWAYS_HIT);

        assertThat(events).contains(new Poisoned(PLAYER, AT, 5));
        assertThat(floor.player().poisonTurns()).isEqualTo(5);
    }

    @Test
    void spiderBite_thatMisses_doesNotPoison() {
        Floor floor = FloorFixtures.parse("@x");

        List<GameEvent> events = COMBAT.resolveAttack(floor, SPIDER, PLAYER, ALWAYS_MISS);

        assertThat(events).noneMatch(Poisoned.class::isInstance);
        assertThat(floor.player().isPoisoned()).isFalse();
    }

    @Test
    void goblinBite_doesNotPoison() {
        Floor floor = FloorFixtures.parse("@g");

        COMBAT.resolveAttack(floor, new EntityId(2), PLAYER, ALWAYS_HIT);

        assertThat(floor.player().isPoisoned()).isFalse();
    }

    @Test
    void playerAttack_doesNotPoisonTheSpider() {
        Floor floor = FloorFixtures.parse("@x");

        COMBAT.resolveAttack(floor, PLAYER, SPIDER, ALWAYS_HIT);

        assertThat(floor.enemy(SPIDER).orElseThrow().isPoisoned()).isFalse();
    }

    @Test
    void antidote_curesAndIsConsumed() {
        Floor floor = FloorFixtures.parse("@");
        Item antidote =
                new Item(new EntityId(50), FloorFixtures.item(StandardItemCatalog.ANTIDOTE));
        give(floor, antidote);
        floor.poison(PLAYER);

        List<GameEvent> events = floor.useItem(PLAYER, antidote.id(), new SeededRandomSource(1));

        assertThat(events).contains(new PoisonCured(PLAYER, AT));
        assertThat(floor.player().isPoisoned()).isFalse();
        assertThat(floor.player().inventory().find(antidote.id())).isEmpty();
    }

    @Test
    void antidote_withoutPoison_doesNothing_andIsNotConsumed() {
        Floor floor = FloorFixtures.parse("@");
        Item antidote =
                new Item(new EntityId(50), FloorFixtures.item(StandardItemCatalog.ANTIDOTE));
        give(floor, antidote);

        List<GameEvent> events = floor.useItem(PLAYER, antidote.id(), new SeededRandomSource(1));

        assertThat(events).isEmpty();
        assertThat(floor.player().inventory().find(antidote.id())).isPresent();
    }

    @Test
    void potion_doesNotCureThePoison() {
        Floor floor = FloorFixtures.parse("@");
        Item potion = new Item(new EntityId(50), FloorFixtures.potion());
        give(floor, potion);
        floor.damage(PLAYER, 40);
        floor.poison(PLAYER);

        floor.useItem(PLAYER, potion.id(), new SeededRandomSource(1));

        assertThat(floor.player().isPoisoned()).isTrue();
    }

    private static void give(Floor floor, Item item) {
        floor.dropItem(floor.positionOf(floor.playerId()), item);
        floor.pickUp(floor.playerId());
    }
}
