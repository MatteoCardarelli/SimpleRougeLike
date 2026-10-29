package io.github.matteocardarelli.simpleroguelike.session;

import io.github.matteocardarelli.simpleroguelike.dungeon.Enemy;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorView;
import io.github.matteocardarelli.simpleroguelike.dungeon.Item;
import io.github.matteocardarelli.simpleroguelike.dungeon.Player;
import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.model.CellSet;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.snapshot.ActorView;
import io.github.matteocardarelli.simpleroguelike.snapshot.CellView;
import io.github.matteocardarelli.simpleroguelike.snapshot.GameSnapshot;
import io.github.matteocardarelli.simpleroguelike.snapshot.InventoryEntry;
import io.github.matteocardarelli.simpleroguelike.snapshot.ItemView;
import io.github.matteocardarelli.simpleroguelike.snapshot.Phase;
import io.github.matteocardarelli.simpleroguelike.snapshot.PlayerView;
import io.github.matteocardarelli.simpleroguelike.snapshot.Visibility;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fotografa il piano per l'interfaccia, con le stesse celle visibili usate per filtrare gli eventi:
 * nemici e oggetti compaiono solo nelle celle visibili adesso.
 */
public final class SnapshotMapper {

    public GameSnapshot from(FloorView floor, Phase phase, long seed, int turn) {
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
        for (Enemy enemy : floor.enemies()) {
            Position at = floor.positionOf(enemy.id());
            if (visible.contains(at)) {
                enemies.add(
                        new ActorView(
                                enemy.id(),
                                enemy.kind(),
                                at,
                                enemy.health(),
                                enemy.stats().maxHealth()));
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
            inventory.add(new InventoryEntry(item.id(), item.kind()));
        }
        PlayerView playerView =
                new PlayerView(
                        player.id(),
                        floor.positionOf(player.id()),
                        player.health(),
                        player.stats().maxHealth(),
                        player.level(),
                        player.experience(),
                        player.experienceToNextLevel());

        return new GameSnapshot(seed, turn, phase, cells, playerView, enemies, items, inventory);
    }

    private static Visibility visibility(Position cell, CellSet visible, CellSet explored) {
        if (visible.contains(cell)) {
            return Visibility.VISIBLE;
        }
        return explored.contains(cell) ? Visibility.EXPLORED : Visibility.UNEXPLORED;
    }
}
