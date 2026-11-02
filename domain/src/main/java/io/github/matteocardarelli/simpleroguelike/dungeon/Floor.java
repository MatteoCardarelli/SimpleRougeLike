package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.event.Damaged;
import io.github.matteocardarelli.simpleroguelike.event.Died;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.event.Healed;
import io.github.matteocardarelli.simpleroguelike.event.InventoryFull;
import io.github.matteocardarelli.simpleroguelike.event.ItemEquipped;
import io.github.matteocardarelli.simpleroguelike.event.ItemPickedUp;
import io.github.matteocardarelli.simpleroguelike.event.ItemUnequipped;
import io.github.matteocardarelli.simpleroguelike.event.ItemUsed;
import io.github.matteocardarelli.simpleroguelike.event.Moved;
import io.github.matteocardarelli.simpleroguelike.model.CellSet;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Il piano in cui si gioca: terreno, attori, oggetti a terra, celle visibili ed esplorate. È la
 * radice dell'aggregate: ogni modifica a ciò che contiene passa dai suoi metodi pubblici, che
 * garantiscono le invarianti.
 *
 * <ul>
 *   <li>Mai due attori vivi nella stessa cella, mai un attore su una cella che non si può
 *       attraversare.
 *   <li>Al massimo un oggetto a terra per cella.
 *   <li>La morte è definitiva: un attore morto non si cura e non si muove.
 *   <li>Le celle esplorate contengono sempre quelle visibili.
 * </ul>
 *
 * <p>Si crea con {@link FloorBuilder}. Non è thread-safe.
 */
public final class Floor implements FloorView {

    private final Grid<TileType> terrain;
    private final Player player;
    private final TreeMap<EntityId, Enemy> enemies;
    private final TreeMap<EntityId, Position> positions;
    private final LinkedHashMap<Position, Item> groundItems;
    private CellSet visible;
    private CellSet explored;

    /** Copia le mappe: chi le passa può continuare a usarle senza toccare il piano. */
    Floor(
            Grid<TileType> terrain,
            Player player,
            Map<EntityId, Enemy> enemies,
            Map<EntityId, Position> positions,
            Map<Position, Item> groundItems) {
        this.terrain = terrain;
        this.player = player;
        this.enemies = new TreeMap<>(enemies);
        this.positions = new TreeMap<>(positions);
        this.groundItems = new LinkedHashMap<>(groundItems);
        this.visible = CellSet.empty(terrain.width(), terrain.height());
        this.explored = visible;
    }

    @Override
    public Grid<TileType> terrain() {
        return terrain;
    }

    @Override
    public EntityId playerId() {
        return player.id();
    }

    @Override
    public Player player() {
        return player;
    }

    @Override
    public List<Enemy> enemies() {
        List<Enemy> alive = new ArrayList<>();
        for (Enemy enemy : enemies.values()) {
            if (enemy.isAlive()) {
                alive.add(enemy);
            }
        }
        return List.copyOf(alive);
    }

    @Override
    public Optional<Actor> actor(EntityId id) {
        if (id.equals(player.id())) {
            return Optional.of(player);
        }
        return Optional.ofNullable(enemies.get(id));
    }

    @Override
    public Optional<Enemy> enemy(EntityId id) {
        return Optional.ofNullable(enemies.get(id));
    }

    @Override
    public Position positionOf(EntityId id) {
        Position position = positions.get(id);
        if (position == null) {
            throw new IllegalArgumentException("Nessun attore " + id + " sul piano");
        }
        return position;
    }

    @Override
    public Optional<Actor> actorAt(Position position) {
        for (Map.Entry<EntityId, Position> entry : positions.entrySet()) {
            if (entry.getValue().equals(position)) {
                Actor actor = requireActor(entry.getKey());
                if (actor.isAlive()) {
                    return Optional.of(actor);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Item> itemAt(Position position) {
        return Optional.ofNullable(groundItems.get(position));
    }

    @Override
    public Map<Position, Item> itemsOnGround() {
        return Collections.unmodifiableMap(groundItems);
    }

    @Override
    public boolean isWalkable(Position position) {
        return terrain.isInside(position) && terrain.get(position).isWalkable();
    }

    @Override
    public boolean isFree(Position position) {
        return isWalkable(position) && !actorAt(position).isPresent();
    }

    @Override
    public CellSet visible() {
        return visible;
    }

    @Override
    public CellSet explored() {
        return explored;
    }

    @Override
    public boolean isExplored(Position position) {
        return explored.contains(position);
    }

    /**
     * Sposta un attore vivo di una cella.
     *
     * @throws IllegalStateException se l'attore è morto o la cella di arrivo non è libera: chi
     *     chiama controlla prima con {@link #isFree}
     */
    public Moved move(EntityId id, Direction direction) {
        Actor actor = requireAlive(id);
        Position from = positionOf(id);
        Position to = from.translate(direction);
        if (!isFree(to)) {
            throw new IllegalStateException(id + " non può entrare in " + to + ": non è libera");
        }
        positions.put(id, to);
        return new Moved(id, actor.kind(), from, to);
    }

    /**
     * L'unico modo di togliere salute a un attore. Su un attore già morto non fa niente.
     *
     * @throws IllegalArgumentException se {@code amount} è negativo
     */
    public DamageResult damage(EntityId target, int amount) {
        requireNotNegative(amount);
        Actor actor = requireActor(target);
        if (!actor.isAlive()) {
            return new DamageResult(0, 0, false, List.of());
        }
        int applied = actor.takeDamage(amount);
        Position at = positionOf(target);
        List<GameEvent> events = new ArrayList<>();
        if (applied > 0) {
            events.add(new Damaged(target, at, applied, actor.health()));
        }
        boolean killed = !actor.isAlive();
        if (killed) {
            events.add(new Died(target, actor.kind(), at));
        }
        return new DamageResult(applied, actor.health(), killed, events);
    }

    /**
     * Cura un attore vivo, senza superare la sua salute massima.
     *
     * @return vuoto se l'attore è morto o era già al massimo
     */
    public Optional<Healed> heal(EntityId target, int amount) {
        requireNotNegative(amount);
        Actor actor = requireActor(target);
        int restored = actor.restoreHealth(amount);
        if (restored == 0) {
            return Optional.empty();
        }
        return Optional.of(new Healed(target, positionOf(target), restored));
    }

    /**
     * Raccoglie l'oggetto nella cella del player. Con l'inventario pieno l'oggetto resta a terra.
     *
     * @return {@code ItemPickedUp}, {@code InventoryFull}, o vuoto se a terra non c'è niente
     */
    public Optional<GameEvent> pickUp(EntityId playerId) {
        requirePlayer(playerId);
        requireAlive(playerId);
        Position at = positionOf(playerId);
        Item item = groundItems.get(at);
        if (item == null) {
            return Optional.empty();
        }
        if (!player.inventory().add(item)) {
            return Optional.of(new InventoryFull(item.id(), item.kind(), at));
        }
        groundItems.remove(at);
        return Optional.of(new ItemPickedUp(item.id(), item.kind(), at));
    }

    /**
     * Usa un oggetto dell'inventario: applica il suo effetto e, se è consumabile, lo toglie.
     *
     * @return {@code ItemUsed} seguito dagli eventi dell'effetto
     * @throws IllegalArgumentException se {@code user} non è il player o l'oggetto non è nel suo
     *     inventario
     * @throws IllegalStateException se il player è morto
     */
    public List<GameEvent> useItem(EntityId user, EntityId itemId, RandomSource random) {
        requirePlayer(user);
        requireAlive(user);
        Item item =
                player.inventory()
                        .find(itemId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                itemId + " non è nell'inventario"));
        List<GameEvent> events = new ArrayList<>();
        events.add(new ItemUsed(itemId, item.kind(), positionOf(user)));
        events.addAll(item.definition().effect().apply(new EffectContext(user, this, random)));
        if (item.isConsumable()) {
            player.inventory().remove(itemId);
        }
        return List.copyOf(events);
    }

    /**
     * Equipaggia un oggetto dell'inventario. Se lo spazio era occupato, l'oggetto di prima torna
     * solo nell'inventario. Non consuma niente.
     *
     * @return {@code ItemUnequipped} per l'oggetto sostituito, se c'era, e {@code ItemEquipped};
     *     vuoto se l'oggetto era già equipaggiato
     * @throws IllegalArgumentException se {@code user} non è il player, l'oggetto non è nel suo
     *     inventario o non si equipaggia
     * @throws IllegalStateException se il player è morto
     */
    public List<GameEvent> equip(EntityId user, EntityId itemId) {
        Item item = requireInInventory(user, itemId);
        if (!item.equipment().isPresent()) {
            throw new IllegalArgumentException(item.kind().value() + " non si equipaggia");
        }
        if (player.isEquipped(itemId)) {
            return List.of();
        }
        Position at = positionOf(user);
        List<GameEvent> events = new ArrayList<>();
        player.equip(item)
                .ifPresent(old -> events.add(new ItemUnequipped(old.id(), old.kind(), at)));
        events.add(new ItemEquipped(itemId, item.kind(), at));
        return List.copyOf(events);
    }

    /**
     * Toglie un oggetto equipaggiato, che resta nell'inventario.
     *
     * @return {@code ItemUnequipped}, o vuoto se l'oggetto non era equipaggiato
     * @throws IllegalArgumentException se {@code user} non è il player o l'oggetto non è nel suo
     *     inventario
     * @throws IllegalStateException se il player è morto
     */
    public List<GameEvent> unequip(EntityId user, EntityId itemId) {
        Item item = requireInInventory(user, itemId);
        if (!player.isEquipped(itemId)) {
            return List.of();
        }
        player.unequip(item);
        return List.of(new ItemUnequipped(itemId, item.kind(), positionOf(user)));
    }

    private Item requireInInventory(EntityId user, EntityId itemId) {
        requirePlayer(user);
        requireAlive(user);
        return player.inventory()
                .find(itemId)
                .orElseThrow(() -> new IllegalArgumentException(itemId + " non è nell'inventario"));
    }

    /**
     * Dà esperienza al player, con tutti i livelli che ne derivano. Un player morto non ne
     * guadagna.
     *
     * @return quanti livelli ha guadagnato
     */
    public int grantExperience(int amount, LevelingPolicy policy) {
        requireNotNegative(amount);
        if (!player.isAlive()) {
            return 0;
        }
        return player.gainExperience(amount, policy);
    }

    /**
     * Toglie dal piano i nemici morti. Si chiama a fine turno.
     *
     * @return i nemici rimossi, in ordine di id
     */
    public List<Enemy> removeDead() {
        List<Enemy> removed = new ArrayList<>();
        Iterator<Enemy> iterator = enemies.values().iterator();
        while (iterator.hasNext()) {
            Enemy enemy = iterator.next();
            if (!enemy.isAlive()) {
                iterator.remove();
                positions.remove(enemy.id());
                removed.add(enemy);
            }
        }
        return List.copyOf(removed);
    }

    /**
     * Sostituisce le celle visibili e le aggiunge a quelle esplorate.
     *
     * @throws IllegalArgumentException se l'insieme non ha le dimensioni del piano
     */
    public void updateVisibility(CellSet visible) {
        if (visible.width() != terrain.width() || visible.height() != terrain.height()) {
            throw new IllegalArgumentException(
                    "Le celle visibili devono avere le dimensioni del piano");
        }
        this.visible = visible;
        this.explored = explored.union(visible);
    }

    private Actor requireActor(EntityId id) {
        return actor(id)
                .orElseThrow(
                        () -> new IllegalArgumentException("Nessun attore " + id + " sul piano"));
    }

    private Actor requireAlive(EntityId id) {
        Actor actor = requireActor(id);
        if (!actor.isAlive()) {
            throw new IllegalStateException(id + " è morto");
        }
        return actor;
    }

    private void requirePlayer(EntityId id) {
        if (!id.equals(player.id())) {
            throw new IllegalArgumentException(id + " non è il player");
        }
    }

    private static void requireNotNegative(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Quantità negativa: " + amount);
        }
    }
}
