package io.github.matteocardarelli.simpleroguelike.dungeon;

import static io.github.matteocardarelli.simpleroguelike.random.FixedRandomSource.ALWAYS_HIT;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.event.CoinsPickedUp;
import io.github.matteocardarelli.simpleroguelike.event.MerchantAngered;
import io.github.matteocardarelli.simpleroguelike.event.MerchantGreeted;
import io.github.matteocardarelli.simpleroguelike.event.NotEnoughCoins;
import io.github.matteocardarelli.simpleroguelike.event.WareTaken;
import io.github.matteocardarelli.simpleroguelike.event.WaresPaid;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Faction;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import io.github.matteocardarelli.simpleroguelike.rules.ActionRules;
import io.github.matteocardarelli.simpleroguelike.rules.CombatRules;
import io.github.matteocardarelli.simpleroguelike.rules.DamageRules;
import io.github.matteocardarelli.simpleroguelike.rules.LevelingRules;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ShopTest {

    private static final ActionRules ACTIONS =
            new ActionRules(
                    new CombatRules(new DamageRules(new LevelingRules(new Stats(20, 2), 150))));

    /** Il player in (2, 0), la spada in vendita in (3, 0), il mercante in (4, 0). */
    private static Floor shop(String row, int price) {
        Floor floor = FloorFixtures.parse(row);
        EntityId merchant = floor.enemies().get(0).id();
        EntityId sword = floor.itemAt(new Position(3, 0)).orElseThrow().id();
        Set<Position> zone = new LinkedHashSet<>();
        for (int x = 2; x <= 5; x++) {
            zone.add(new Position(x, 0));
        }
        Map<EntityId, Integer> prices = new LinkedHashMap<>();
        prices.put(sword, price);
        floor.openShop(new Shop(merchant, zone, prices));
        return floor;
    }

    @Test
    void coins_goToTheCounter_notToTheInventory() {
        Floor floor = FloorFixtures.parse("@c");

        var result = ACTIONS.step(floor, floor.playerId(), Direction.RIGHT, ALWAYS_HIT);

        assertThat(result.events()).anyMatch(CoinsPickedUp.class::isInstance);
        assertThat(floor.player().coins()).isEqualTo(15);
        assertThat(floor.player().inventory().items()).isEmpty();
    }

    @Test
    void merchant_isNeutral_untilAngered() {
        Floor floor = shop("..@/M.", 60);
        Enemy merchant = floor.enemies().get(0);

        assertThat(merchant.faction()).isEqualTo(Faction.NEUTRAL);
        assertThat(Faction.PLAYER.isHostileTo(merchant.faction())).isFalse();
    }

    @Test
    void ware_isNotPickedUpByStepping_butTaken_andPaidByBumpingTheMerchant() {
        Floor floor = shop("..@/M.", 60);
        EntityId player = floor.playerId();

        ACTIONS.step(floor, player, Direction.RIGHT, ALWAYS_HIT);
        assertThat(floor.player().inventory().items()).isEmpty();
        assertThat(floor.wareAt(new Position(3, 0))).isPresent();

        assertThat(floor.takeWare(player)).get().isInstanceOf(WareTaken.class);
        assertThat(floor.shop().orElseThrow().bill()).isEqualTo(60);

        var poor = ACTIONS.step(floor, player, Direction.RIGHT, ALWAYS_HIT);
        assertThat(poor.events()).singleElement().isInstanceOf(NotEnoughCoins.class);

        floor.player().addCoins(100);
        var paid = ACTIONS.step(floor, player, Direction.RIGHT, ALWAYS_HIT);
        assertThat(paid.events()).containsExactly(new WaresPaid(60, new Position(3, 0)));
        assertThat(floor.player().coins()).isEqualTo(40);
        assertThat(ACTIONS.step(floor, player, Direction.RIGHT, ALWAYS_HIT).events())
                .singleElement()
                .isInstanceOf(MerchantGreeted.class);
    }

    @Test
    void leavingTheShopWithoutPaying_angersTheMerchant() {
        Floor floor = shop("..@/M.", 60);
        EntityId player = floor.playerId();
        ACTIONS.step(floor, player, Direction.RIGHT, ALWAYS_HIT);
        floor.takeWare(player);
        ACTIONS.step(floor, player, Direction.LEFT, ALWAYS_HIT);

        var escape = ACTIONS.step(floor, player, Direction.LEFT, ALWAYS_HIT);

        assertThat(escape.events()).anyMatch(MerchantAngered.class::isInstance);
        Enemy merchant = floor.enemies().get(0);
        assertThat(merchant.isAngry()).isTrue();
        assertThat(merchant.faction()).isEqualTo(Faction.MONSTERS);
        assertThat(floor.player().inventory().items()).hasSize(1);
        assertThat(floor.shop().orElseThrow().unpaid()).isEmpty();
    }

    @Test
    void leavingAfterPaying_isFine() {
        Floor floor = shop("..@/M.", 60);
        EntityId player = floor.playerId();
        floor.player().addCoins(60);
        ACTIONS.step(floor, player, Direction.RIGHT, ALWAYS_HIT);
        floor.takeWare(player);
        ACTIONS.step(floor, player, Direction.RIGHT, ALWAYS_HIT);
        ACTIONS.step(floor, player, Direction.LEFT, ALWAYS_HIT);

        var leave = ACTIONS.step(floor, player, Direction.LEFT, ALWAYS_HIT);

        assertThat(leave.events()).noneMatch(MerchantAngered.class::isInstance);
        assertThat(floor.enemies().get(0).isAngry()).isFalse();
    }

    @Test
    void giveBack_removesTheWareFromTheBill_andKeepsItsPrice() {
        Floor floor = shop("..@/M.", 60);
        floor.move(floor.playerId(), Direction.RIGHT);
        floor.takeWare(floor.playerId());
        Shop shop = floor.shop().orElseThrow();
        EntityId sword = shop.unpaid().iterator().next();

        shop.giveBack(sword);

        assertThat(shop.unpaid()).isEmpty();
        assertThat(shop.bill()).isZero();
        assertThat(shop.priceOf(sword)).contains(60);
    }
}
