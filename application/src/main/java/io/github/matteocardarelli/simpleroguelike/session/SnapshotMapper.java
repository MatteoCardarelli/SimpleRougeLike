package io.github.matteocardarelli.simpleroguelike.session;

import io.github.matteocardarelli.simpleroguelike.dungeon.Enemy;
import io.github.matteocardarelli.simpleroguelike.dungeon.Equipment;
import io.github.matteocardarelli.simpleroguelike.dungeon.Fixture;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorView;
import io.github.matteocardarelli.simpleroguelike.dungeon.Item;
import io.github.matteocardarelli.simpleroguelike.dungeon.Player;
import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.model.CellSet;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
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
import io.github.matteocardarelli.simpleroguelike.snapshot.WareView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fotografa il piano per l'interfaccia, con le stesse celle visibili usate per filtrare gli eventi:
 * nemici e oggetti compaiono solo nelle celle visibili adesso.
 */
public final class SnapshotMapper {

    public GameSnapshot from(FloorView floor, Phase phase, long seed, int turn) {
        return from(floor, phase, seed, turn, 1, 1);
    }

    /** Come l'altro, per il piano {@code depth} di una partita di {@code floors} piani. */
    public GameSnapshot from(
            FloorView floor, Phase phase, long seed, int turn, int depth, int floors) {
        CellSet visible = floor.visible();
        CellSet explored = floor.explored();
        Grid<TileType> terrain = floor.terrain();
        Grid<CellView> cells =
                Grid.of(
                        terrain.width(),
                        terrain.height(),
                        cell ->
                                new CellView(
                                        terrain.get(cell) == TileType.WALL,
                                        visibility(cell, visible, explored)));

        List<ActorView> enemies = new ArrayList<>();
        List<Position> danger = new ArrayList<>();
        for (Enemy enemy : floor.enemies()) {
            boolean seen = false;
            for (Position cell : floor.cellsOf(enemy.id())) {
                seen = seen || visible.contains(cell);
            }
            if (seen) {
                enemies.add(
                        new ActorView(
                                enemy.id(),
                                enemy.kind(),
                                floor.positionOf(enemy.id()),
                                enemy.health(),
                                enemy.stats().maxHealth(),
                                enemy.footprint().size(),
                                enemy.isBoss()));
                if (enemy.pendingJump().isPresent()) {
                    danger.addAll(enemy.footprint().at(enemy.pendingJump().get()));
                }
            }
        }

        List<ItemView> items = new ArrayList<>();
        for (Map.Entry<Position, Item> entry : floor.itemsOnGround().entrySet()) {
            if (visible.contains(entry.getKey())) {
                Item item = entry.getValue();
                items.add(new ItemView(item.id(), item.kind(), entry.getKey()));
            }
        }

        Player player = floor.player();
        List<InventoryEntry> inventory = new ArrayList<>();
        for (Item item : player.inventory().items()) {
            inventory.add(
                    new InventoryEntry(
                            item.id(),
                            item.kind(),
                            item.equipment().map(Equipment::slot),
                            player.isEquipped(item.id()),
                            item.throwing().isPresent(),
                            item.count(),
                            item.definition().usable()));
        }
        PlayerView playerView =
                new PlayerView(
                        player.id(),
                        floor.positionOf(player.id()),
                        player.health(),
                        player.stats().maxHealth(),
                        player.level(),
                        player.experience(),
                        player.experienceToNextLevel(),
                        player.stats().attack(),
                        player.stats().defense(),
                        player.stats().accuracy(),
                        player.stats().evasion(),
                        player.coins());

        List<FixtureView> fixtures = new ArrayList<>();
        for (Map.Entry<Position, Fixture> entry : floor.fixtures().entrySet()) {
            if (explored.contains(entry.getKey())) {
                Fixture fixture = entry.getValue();
                fixtures.add(new FixtureView(entry.getKey(), fixture.kind(), fixture.isOpen()));
            }
        }

        return new GameSnapshot(
                seed,
                turn,
                phase,
                cells,
                playerView,
                enemies,
                items,
                inventory,
                fixtures,
                depth,
                floors,
                danger,
                ware(floor, playerView.position()));
    }

    private static Optional<WareView> ware(FloorView floor, Position at) {
        Optional<Item> item = floor.wareAt(at);
        if (!item.isPresent()) {
            return Optional.empty();
        }
        int price = floor.shop().flatMap(shop -> shop.priceOf(item.get().id())).orElse(0);
        return Optional.of(new WareView(item.get().id(), item.get().kind(), price));
    }

    private static Visibility visibility(Position cell, CellSet visible, CellSet explored) {
        if (visible.contains(cell)) {
            return Visibility.VISIBLE;
        }
        return explored.contains(cell) ? Visibility.EXPLORED : Visibility.UNEXPLORED;
    }
}
