package io.github.matteocardarelli.simpleroguelike.dungeon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.matteocardarelli.simpleroguelike.content.StandardEnemyCatalog;
import io.github.matteocardarelli.simpleroguelike.content.StandardItemCatalog;
import io.github.matteocardarelli.simpleroguelike.model.CellSet;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FloorCodecTest {

    private static final FloorCodec CODEC =
            new FloorCodec(new StandardItemCatalog(), new StandardEnemyCatalog());

    /**
     * Un piano a metà partita: scrigno aperto con la pozione a terra sotto, leva tirata e cancello
     * aperto, player ferito con monete, spada e anello equipaggiati e due coltelli, goblin ferito,
     * mercante arrabbiato, negozio con una merce presa e non pagata, celle esplorate diverse dalle
     * visibili.
     */
    private static Floor played() {
        EntityIdSequence ids = new EntityIdSequence();
        Floor floor =
                FloorFixtures.parse(
                        ids, 8, "###L#####", "#.$@G.g.#", "#./-....#", "#..+.M[.#", "#########");
        EntityId player = floor.playerId();
        floor.interact(player, new Position(2, 1));
        floor.interact(player, new Position(3, 0));
        Item sword = new Item(ids.next(), FloorFixtures.item(StandardItemCatalog.SWORD));
        Item ring = new Item(ids.next(), FloorFixtures.item(StandardItemCatalog.RING_OF_VIGOR));
        Item accuracy =
                new Item(ids.next(), FloorFixtures.item(StandardItemCatalog.RING_OF_ACCURACY));
        floor.player().inventory().add(sword);
        floor.player().inventory().add(ring);
        floor.player().inventory().add(accuracy);
        for (int i = 0; i < 2; i++) {
            floor.player()
                    .inventory()
                    .add(
                            new Item(
                                    ids.next(),
                                    FloorFixtures.item(StandardItemCatalog.THROWING_KNIFE)));
        }
        floor.equip(player, sword.id());
        floor.equip(player, ring.id());
        floor.damage(player, 7);
        floor.player().addCoins(45);
        Enemy goblin = floor.enemies().get(0);
        floor.damage(goblin.id(), 3);
        goblin.restoreState(JumpPhase.AIRBORNE, Optional.of(new Position(6, 2)), false);
        floor.recordRetreat(goblin.id(), true);
        Enemy merchant = floor.enemies().get(1);
        merchant.restoreState(JumpPhase.CHARGING, Optional.empty(), true);
        Set<Position> zone = new LinkedHashSet<>();
        for (int x = 4; x <= 7; x++) {
            zone.add(new Position(x, 2));
            zone.add(new Position(x, 3));
        }
        Map<EntityId, Integer> prices = new LinkedHashMap<>();
        prices.put(floor.itemAt(new Position(6, 3)).orElseThrow().id(), 40);
        prices.put(accuracy.id(), 25);
        Shop shop = new Shop(merchant.id(), zone, prices);
        shop.take(accuracy.id());
        floor.openShop(shop);
        floor.updateVisibility(cells(new Position(1, 1), new Position(2, 1), new Position(3, 1)));
        floor.updateVisibility(cells(new Position(3, 1), new Position(4, 1), new Position(3, 2)));
        return floor;
    }

    private static CellSet cells(Position... positions) {
        return CellSet.of(9, 5, List.of(positions));
    }

    @Test
    void read_ofWrite_givesTheSameLines() {
        List<String> lines = CODEC.write(played());

        assertThat(CODEC.write(CODEC.read(lines))).isEqualTo(lines);
    }

    @Test
    void read_restoresPlayer_withEquipment_coinsAndStacks() {
        Floor floor = played();

        Floor read = CODEC.read(CODEC.write(floor));

        Player player = read.player();
        assertThat(player.stats()).isEqualTo(floor.player().stats());
        assertThat(player.health()).isEqualTo(floor.player().health());
        assertThat(player.coins()).isEqualTo(45);
        assertThat(player.equipped()).hasSize(2);
        assertThat(player.inventory().items())
                .filteredOn(item -> item.kind().equals(StandardItemCatalog.THROWING_KNIFE))
                .singleElement()
                .extracting(Item::count)
                .isEqualTo(2);
        assertThat(read.positionOf(read.playerId())).isEqualTo(new Position(3, 1));
    }

    @Test
    void read_restoresFixtures_theOpenedChestsItem_andTheShop() {
        Floor floor = played();

        Floor read = CODEC.read(CODEC.write(floor));

        assertThat(read.fixtureAt(new Position(2, 1))).map(Fixture::isOpen).contains(true);
        assertThat(read.itemAt(new Position(2, 1))).isPresent();
        assertThat(read.fixtureAt(new Position(4, 1))).map(Fixture::isOpen).contains(true);
        assertThat(read.fixtureAt(new Position(3, 3))).map(Fixture::isOpen).contains(false);
        assertThat(read.shop().orElseThrow().bill()).isEqualTo(25);
        assertThat(read.enemies().get(1).isAngry()).isTrue();
        assertThat(read.enemies().get(0).pendingJump()).contains(new Position(6, 2));
        assertThat(read.enemies().get(0).jumpPhase()).isEqualTo(JumpPhase.AIRBORNE);
        assertThat(read.enemies().get(1).jumpPhase()).isEqualTo(JumpPhase.CHARGING);
        assertThat(read.enemies().get(0).hasJustRetreated()).isTrue();
        assertThat(read.enemies().get(1).hasJustRetreated()).isFalse();
    }

    @Test
    void read_restoresExploredAndVisibleCells() {
        Floor floor = played();

        Floor read = CODEC.read(CODEC.write(floor));

        assertThat(read.visible().positions()).isEqualTo(floor.visible().positions());
        assertThat(read.explored().positions()).isEqualTo(floor.explored().positions());
        assertThat(read.explored().positions()).hasSize(5);
    }

    /** Un salvataggio del formato 1 non ha gli ultimi campi dei nemici: fuga e livello. */
    @Test
    void read_ofAnEnemyLineWithoutTheNewerFields_readsIt() {
        List<String> lines = new ArrayList<>();
        for (String line : CODEC.write(played())) {
            if (line.startsWith("enemy ")) {
                String[] parts = line.split(" ");
                lines.add(String.join(" ", java.util.Arrays.copyOf(parts, 10)));
            } else {
                lines.add(line);
            }
        }

        Floor read = CODEC.read(lines);

        assertThat(read.enemies().get(0).hasJustRetreated()).isFalse();
        assertThat(read.enemies().get(0).level()).isEqualTo(1);
    }

    /**
     * Prima un salto annunciato lasciava il nemico a terra fino all'atterraggio: un salvataggio di
     * allora si rilegge con il nemico in aria, sopra la sua ombra; senza salto è fermo.
     */
    @Test
    void read_ofAnOldAnnouncedJump_putsTheEnemyInTheAirAboveItsShadow() {
        List<String> lines = new ArrayList<>();
        for (String line : CODEC.write(played())) {
            if (line.startsWith("enemy ")) {
                String[] parts = line.split(" ");
                lines.add(String.join(" ", java.util.Arrays.copyOf(parts, 12)));
            } else {
                lines.add(line);
            }
        }

        Floor read = CODEC.read(lines);

        Enemy goblin = read.enemies().get(0);
        assertThat(goblin.jumpPhase()).isEqualTo(JumpPhase.AIRBORNE);
        assertThat(read.positionOf(goblin.id())).isEqualTo(new Position(6, 2));
        assertThat(read.actorAt(new Position(6, 2))).isEmpty();
        assertThat(read.enemies().get(1).jumpPhase()).isEqualTo(JumpPhase.RESTING);
    }

    @Test
    void read_restoresTheBossRoom_andAnOldSaveHasNone() {
        Floor floor = played();
        List<String> lines = new ArrayList<>();
        for (String line : CODEC.write(floor)) {
            if (line.startsWith("explored ")) {
                lines.add(line);
            } else if (line.startsWith("visible ")) {
                lines.add(line);
                lines.add("room 5 2 6 2 5 3");
            } else {
                lines.add(line);
            }
        }

        Floor read = CODEC.read(lines);

        assertThat(read.bossRoom().positions())
                .containsExactly(new Position(5, 2), new Position(6, 2), new Position(5, 3));
        assertThat(CODEC.write(read)).contains("room 5 2 6 2 5 3");
        assertThat(CODEC.read(CODEC.write(floor)).bossRoom().isEmpty()).isTrue();
    }

    @Test
    void read_restoresTheKeys() {
        Floor floor = played();
        floor.player().addKeys(KeyType.COMMON, 2);
        floor.player().addKeys(KeyType.BOSS, 1);

        Player player = CODEC.read(CODEC.write(floor)).player();

        assertThat(player.keys(KeyType.COMMON)).isEqualTo(2);
        assertThat(player.keys(KeyType.BOSS)).isEqualTo(1);
    }

    /** Prima del contatore le chiavi stavano nell'inventario: si rileggono nel contatore. */
    @Test
    void read_ofAKeyInTheInventory_putsItInTheCounter() {
        List<String> lines = new ArrayList<>();
        for (String line : CODEC.write(played())) {
            if (line.startsWith("player ")) {
                String[] parts = line.split(" ");
                lines.add(String.join(" ", java.util.Arrays.copyOf(parts, 15)));
                lines.add("inv 90 key 2 0");
            } else {
                lines.add(line);
            }
        }

        Player player = CODEC.read(lines).player();

        assertThat(player.keys(KeyType.COMMON)).isEqualTo(2);
        assertThat(player.inventory().items()).noneMatch(item -> item.kind().value().equals("key"));
    }

    @Test
    void read_withoutTheEndLine_throwsIllegalArgument() {
        List<String> lines = new ArrayList<>(CODEC.write(played()));
        lines.remove(lines.size() - 1);

        assertThatThrownBy(() -> CODEC.read(lines)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void read_withAnUnknownKind_throwsIllegalArgument() {
        List<String> lines = new ArrayList<>();
        for (String line : CODEC.write(played())) {
            lines.add(line.startsWith("enemy ") ? line.replace("goblin", "dragon") : line);
        }

        assertThatThrownBy(() -> CODEC.read(lines)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void length_countsTheLinesUpToEnd() {
        List<String> lines = new ArrayList<>(CODEC.write(played()));
        int length = lines.size();
        lines.add("dopo");

        assertThat(FloorCodec.length(lines)).isEqualTo(length);
        assertThat(Kind.of("goblin")).isEqualTo(FloorFixtures.goblin().kind());
    }
}
