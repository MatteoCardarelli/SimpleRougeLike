package io.github.matteocardarelli.simpleroguelike.dungeon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.matteocardarelli.simpleroguelike.content.StandardItemCatalog;
import io.github.matteocardarelli.simpleroguelike.event.CoinsPickedUp;
import io.github.matteocardarelli.simpleroguelike.event.DropRefused;
import io.github.matteocardarelli.simpleroguelike.event.ItemPickedUp;
import io.github.matteocardarelli.simpleroguelike.event.ItemPutDown;
import io.github.matteocardarelli.simpleroguelike.event.ItemUnequipped;
import io.github.matteocardarelli.simpleroguelike.event.WareTaken;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FloorDropTest {

    private static final EntityId PLAYER = new EntityId(1);
    private static final EntityId FIRST = new EntityId(2);
    private static final EntityId SECOND = new EntityId(3);
    private static final Kind POTION = Kind.of("potion");

    /**
     * Il player raccoglie quello che trova camminando verso destra, per il numero di passi dato.
     */
    private static Floor walkRight(int capacity, int steps, String row) {
        Floor floor = FloorFixtures.parse(capacity, row);
        for (int i = 0; i < steps; i++) {
            floor.move(PLAYER, Direction.RIGHT);
            floor.pickUp(PLAYER);
        }
        return floor;
    }

    /** La riga ha una sola merce in vendita (la spada); la zona sono le celle date. */
    private static Floor shop(String row, int steps, int price, int... zoneXs) {
        Floor floor = FloorFixtures.parse(row);
        EntityId merchant = floor.enemies().get(0).id();
        EntityId sword =
                floor.itemsOnGround().values().stream()
                        .filter(item -> item.kind().equals(StandardItemCatalog.SWORD))
                        .findFirst()
                        .orElseThrow()
                        .id();
        Set<Position> zone = new LinkedHashSet<>();
        for (int x : zoneXs) {
            zone.add(new Position(x, 0));
        }
        Map<EntityId, Integer> prices = new LinkedHashMap<>();
        prices.put(sword, price);
        floor.openShop(new Shop(merchant, zone, prices));
        for (int i = 0; i < steps; i++) {
            floor.move(PLAYER, Direction.RIGHT);
        }
        return floor;
    }

    @Test
    @DisplayName("drop: in una cella vuota l'oggetto resta a terra e lascia l'inventario")
    void drop_onAnEmptyCell_putsTheItemOnTheGround() {
        Floor floor = walkRight(5, 1, "@!.");
        floor.move(PLAYER, Direction.RIGHT);
        Position at = new Position(2, 0);

        var events = floor.drop(PLAYER, FIRST);

        assertThat(events).containsExactly(new ItemPutDown(FIRST, POTION, at));
        assertThat(floor.player().inventory().items()).isEmpty();
        assertThat(floor.itemAt(at)).get().extracting(Item::id).isEqualTo(FIRST);
    }

    @Test
    @DisplayName("drop: un oggetto equipaggiato viene prima tolto")
    void drop_anEquippedItem_isUnequippedFirst() {
        Floor floor = walkRight(5, 1, "@/");
        floor.equip(PLAYER, FIRST);
        Position at = new Position(1, 0);

        var events = floor.drop(PLAYER, FIRST);

        assertThat(events)
                .containsExactly(
                        new ItemUnequipped(FIRST, StandardItemCatalog.SWORD, at),
                        new ItemPutDown(FIRST, StandardItemCatalog.SWORD, at));
        assertThat(floor.player().isEquipped(FIRST)).isFalse();
        assertThat(floor.player().stats()).isEqualTo(FloorFixtures.PLAYER_STATS);
        assertThat(floor.itemAt(at)).isPresent();
    }

    @Test
    @DisplayName("drop: una pila si lascia intera, come un solo oggetto")
    void drop_aStack_dropsTheWholeStack() {
        Floor floor = walkRight(5, 3, "@---");
        Position at = new Position(3, 0);

        var events = floor.drop(PLAYER, FIRST);

        assertThat(events)
                .containsExactly(new ItemPutDown(FIRST, StandardItemCatalog.THROWING_KNIFE, at));
        assertThat(floor.player().inventory().items()).isEmpty();
        assertThat(floor.itemAt(at)).get().extracting(Item::count).isEqualTo(3);
    }

    @Test
    @DisplayName("drop: su un oggetto qualunque i due si scambiano, anche con l'inventario pieno")
    void drop_onAPlainItem_swapsThem() {
        Floor floor = walkRight(1, 1, "@!/");
        floor.move(PLAYER, Direction.RIGHT);
        Position at = new Position(2, 0);

        var events = floor.drop(PLAYER, FIRST);

        assertThat(events)
                .containsExactly(
                        new ItemPutDown(FIRST, POTION, at),
                        new ItemPickedUp(SECOND, StandardItemCatalog.SWORD, at));
        assertThat(floor.player().inventory().items()).extracting(Item::id).containsExactly(SECOND);
        assertThat(floor.itemAt(at)).get().extracting(Item::id).isEqualTo(FIRST);
    }

    @Test
    @DisplayName("drop: su delle monete le prende nel contatore")
    void drop_onCoins_swapsAndCountsTheCoins() {
        Floor floor = walkRight(5, 1, "@!c");
        floor.move(PLAYER, Direction.RIGHT);
        Position at = new Position(2, 0);

        var events = floor.drop(PLAYER, FIRST);

        assertThat(events)
                .containsExactly(new ItemPutDown(FIRST, POTION, at), new CoinsPickedUp(15, at));
        assertThat(floor.player().coins()).isEqualTo(15);
        assertThat(floor.player().inventory().items()).isEmpty();
        assertThat(floor.itemAt(at)).get().extracting(Item::id).isEqualTo(FIRST);
    }

    @Test
    @DisplayName("drop: su una chiave la prende nel contatore delle chiavi")
    void drop_onAKey_swapsAndCountsTheKey() {
        Floor floor = walkRight(5, 1, "@!k");
        floor.move(PLAYER, Direction.RIGHT);
        Position at = new Position(2, 0);

        var events = floor.drop(PLAYER, FIRST);

        assertThat(events)
                .containsExactly(
                        new ItemPutDown(FIRST, POTION, at),
                        new ItemPickedUp(SECOND, StandardItemCatalog.KEY, at));
        assertThat(floor.player().keys(KeyType.COMMON)).isEqualTo(1);
        assertThat(floor.player().inventory().items()).isEmpty();
    }

    @Test
    @DisplayName("drop: su una merce la prende da pagare")
    void drop_onAWare_takesItUnpaid() {
        Floor floor = shop("@!/M", 1, 60, 0, 1, 2, 3);
        floor.pickUp(PLAYER);
        floor.move(PLAYER, Direction.RIGHT);
        Position at = new Position(2, 0);

        var events = floor.drop(PLAYER, FIRST);

        assertThat(events)
                .containsExactly(
                        new ItemPutDown(FIRST, POTION, at),
                        new WareTaken(SECOND, StandardItemCatalog.SWORD, 60, at));
        assertThat(floor.shop().orElseThrow().unpaid()).containsExactly(SECOND);
        assertThat(floor.shop().orElseThrow().bill()).isEqualTo(60);
        assertThat(floor.player().inventory().items()).extracting(Item::id).containsExactly(SECOND);
        assertThat(floor.itemAt(at)).get().extracting(Item::id).isEqualTo(FIRST);
        assertThat(floor.wareAt(at)).isEmpty();
    }

    @Test
    @DisplayName("drop: una merce non pagata lasciata nel negozio torna in vendita")
    void drop_anUnpaidWareInsideTheShop_putsItBackOnSale() {
        Floor floor = shop("@/M", 1, 60, 0, 1, 2);
        Position at = new Position(1, 0);
        floor.takeWare(PLAYER);
        assertThat(floor.shop().orElseThrow().bill()).isEqualTo(60);

        var events = floor.drop(PLAYER, FIRST);

        assertThat(events).containsExactly(new ItemPutDown(FIRST, StandardItemCatalog.SWORD, at));
        assertThat(floor.shop().orElseThrow().unpaid()).isEmpty();
        assertThat(floor.shop().orElseThrow().bill()).isZero();
        assertThat(floor.wareAt(at)).get().extracting(Item::id).isEqualTo(FIRST);
        assertThat(floor.player().inventory().items()).isEmpty();
    }

    @Test
    @DisplayName("drop: una merce non pagata fuori dal negozio si rifiuta e non cambia niente")
    void drop_anUnpaidWareOutsideTheShop_isRefused() {
        Floor floor = shop(".@/M", 0, 60, 2, 3);
        floor.move(PLAYER, Direction.RIGHT);
        floor.takeWare(PLAYER);
        floor.move(PLAYER, Direction.LEFT);
        Position at = new Position(1, 0);

        var events = floor.drop(PLAYER, FIRST);

        assertThat(events)
                .containsExactly(
                        new DropRefused(
                                FIRST, StandardItemCatalog.SWORD, at, DropRefused.Reason.UNPAID));
        assertThat(floor.shop().orElseThrow().unpaid()).containsExactly(FIRST);
        assertThat(floor.player().inventory().find(FIRST)).isPresent();
        assertThat(floor.itemAt(at)).isEmpty();
    }

    @Test
    @DisplayName("drop: su un oggetto fisso si rifiuta e non cambia niente")
    void drop_onAFixture_isRefused() {
        Floor floor = walkRight(5, 1, "@!>");
        floor.move(PLAYER, Direction.RIGHT);
        Position at = new Position(2, 0);

        var events = floor.drop(PLAYER, FIRST);

        assertThat(events)
                .containsExactly(new DropRefused(FIRST, POTION, at, DropRefused.Reason.FIXTURE));
        assertThat(floor.player().inventory().find(FIRST)).isPresent();
        assertThat(floor.itemAt(at)).isEmpty();
    }

    @Test
    @DisplayName("drop: un oggetto che non è nell'inventario è un errore")
    void drop_notInTheInventory_throws() {
        Floor floor = FloorFixtures.parse("@!");

        assertThatThrownBy(() -> floor.drop(PLAYER, FIRST))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
