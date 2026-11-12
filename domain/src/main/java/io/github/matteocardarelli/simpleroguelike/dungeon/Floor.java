package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.event.ChestOpened;
import io.github.matteocardarelli.simpleroguelike.event.CoinsPickedUp;
import io.github.matteocardarelli.simpleroguelike.event.Damaged;
import io.github.matteocardarelli.simpleroguelike.event.Died;
import io.github.matteocardarelli.simpleroguelike.event.DoorLocked;
import io.github.matteocardarelli.simpleroguelike.event.DoorOpened;
import io.github.matteocardarelli.simpleroguelike.event.DoorUnlocked;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.event.Healed;
import io.github.matteocardarelli.simpleroguelike.event.InventoryFull;
import io.github.matteocardarelli.simpleroguelike.event.ItemEquipped;
import io.github.matteocardarelli.simpleroguelike.event.ItemPickedUp;
import io.github.matteocardarelli.simpleroguelike.event.ItemUnequipped;
import io.github.matteocardarelli.simpleroguelike.event.ItemUsed;
import io.github.matteocardarelli.simpleroguelike.event.JumpAnnounced;
import io.github.matteocardarelli.simpleroguelike.event.JumpCharged;
import io.github.matteocardarelli.simpleroguelike.event.Landed;
import io.github.matteocardarelli.simpleroguelike.event.LeverPulled;
import io.github.matteocardarelli.simpleroguelike.event.MerchantAngered;
import io.github.matteocardarelli.simpleroguelike.event.MerchantGreeted;
import io.github.matteocardarelli.simpleroguelike.event.Moved;
import io.github.matteocardarelli.simpleroguelike.event.NotEnoughCoins;
import io.github.matteocardarelli.simpleroguelike.event.WareTaken;
import io.github.matteocardarelli.simpleroguelike.event.WaresPaid;
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
import org.jspecify.annotations.Nullable;

/**
 * Il piano in cui si gioca: terreno, oggetti fissi, attori, oggetti a terra, celle visibili ed
 * esplorate. È la radice dell'aggregate: ogni modifica a ciò che contiene passa dai suoi metodi
 * pubblici, che garantiscono le invarianti.
 *
 * <ul>
 *   <li>Mai due attori vivi nella stessa cella, mai un attore su una cella che non si può
 *       attraversare. Un attore grande occupa tutte le celle della sua impronta, a partire dalla
 *       cella di riferimento che il piano tiene come sua posizione. Un nemico in aria non occupa
 *       celle: la sua posizione è la cella di riferimento della sua ombra.
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
    private final LinkedHashMap<Position, Fixture> fixtures;
    private @Nullable Grid<TileType> movementGrid;
    private @Nullable Grid<TileType> sightGrid;
    private @Nullable EntityId defeatedBoss;
    private @Nullable Shop shop;
    private final CellSet bossRoom;
    private CellSet visible;
    private CellSet explored;

    /** Copia le mappe: chi le passa può continuare a usarle senza toccare il piano. */
    Floor(
            Grid<TileType> terrain,
            Player player,
            Map<EntityId, Enemy> enemies,
            Map<EntityId, Position> positions,
            Map<Position, Item> groundItems,
            Map<Position, Fixture> fixtures,
            CellSet bossRoom) {
        this.terrain = terrain;
        this.bossRoom = bossRoom;
        this.player = player;
        this.enemies = new TreeMap<>(enemies);
        this.positions = new TreeMap<>(positions);
        this.groundItems = new LinkedHashMap<>(groundItems);
        this.fixtures = new LinkedHashMap<>(fixtures);
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
            Actor actor = requireActor(entry.getKey());
            if (actor.isAlive()
                    && !isAirborne(entry.getKey())
                    && actor.footprint().at(entry.getValue()).contains(position)) {
                return Optional.of(actor);
            }
        }
        return Optional.empty();
    }

    private boolean isAirborne(EntityId id) {
        Enemy enemy = enemies.get(id);
        return enemy != null && enemy.isAirborne();
    }

    @Override
    public CellSet bossRoom() {
        return bossRoom;
    }

    @Override
    public List<Position> cellsOf(EntityId id) {
        return requireActor(id).footprint().at(positionOf(id));
    }

    @Override
    public boolean canOccupy(EntityId id, Position anchor) {
        return fits(id, anchor, false);
    }

    @Override
    public boolean canLand(EntityId id, Position anchor) {
        return fits(id, anchor, true);
    }

    /**
     * Ogni cella dell'impronta attraversabile e libera, salvo l'attore stesso e forse il player.
     */
    private boolean fits(EntityId id, Position anchor, boolean ignoringPlayer) {
        for (Position cell : requireActor(id).footprint().at(anchor)) {
            if (!isWalkable(cell)) {
                return false;
            }
            Optional<Actor> occupant = actorAt(cell);
            if (occupant.isPresent()
                    && !occupant.get().id().equals(id)
                    && !(ignoringPlayer && occupant.get().id().equals(player.id()))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Optional<Position> contactCell(EntityId attacker, EntityId defender) {
        List<Position> defenderCells = cellsOf(defender);
        for (Position cell : cellsOf(attacker)) {
            for (Direction direction : Direction.values()) {
                Position next = cell.translate(direction);
                if (defenderCells.contains(next)) {
                    return Optional.of(next);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<EntityId> bossId() {
        for (Enemy enemy : enemies.values()) {
            if (enemy.isBoss()) {
                return Optional.of(enemy.id());
            }
        }
        return Optional.ofNullable(defeatedBoss);
    }

    @Override
    public boolean isBossDefeated() {
        Optional<EntityId> boss = bossId();
        return boss.isPresent() && enemy(boss.get()).map(e -> !e.isAlive()).orElse(true);
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
        if (!terrain.isInside(position) || !terrain.get(position).isWalkable()) {
            return false;
        }
        Fixture fixture = fixtures.get(position);
        return fixture == null || !fixture.blocksMovement();
    }

    @Override
    public Optional<Fixture> fixtureAt(Position position) {
        return Optional.ofNullable(fixtures.get(position));
    }

    @Override
    public Map<Position, Fixture> fixtures() {
        return Collections.unmodifiableMap(fixtures);
    }

    @Override
    public Grid<TileType> movementGrid() {
        Grid<TileType> grid = movementGrid;
        if (grid == null) {
            grid =
                    Grid.of(
                            terrain.width(),
                            terrain.height(),
                            p -> isWalkable(p) ? TileType.FLOOR : TileType.WALL);
            movementGrid = grid;
        }
        return grid;
    }

    @Override
    public Grid<TileType> sightGrid() {
        Grid<TileType> grid = sightGrid;
        if (grid == null) {
            grid =
                    Grid.of(
                            terrain.width(),
                            terrain.height(),
                            p -> {
                                Fixture fixture = fixtures.get(p);
                                return fixture != null && fixture.blocksSight()
                                        ? TileType.WALL
                                        : terrain.get(p);
                            });
            sightGrid = grid;
        }
        return grid;
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
        if (!canOccupy(id, to)) {
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
        int coins = item.definition().coinValue();
        if (coins > 0) {
            groundItems.remove(at);
            player.addCoins(coins);
            return Optional.of(new CoinsPickedUp(coins, at));
        }
        if (wareAt(at).isPresent()) {
            // La merce non si raccoglie da sola: il player sceglie se prenderla.
            return Optional.empty();
        }
        Optional<KeyType> key = item.definition().key();
        if (key.isPresent()) {
            // Le chiavi non occupano posto: vanno nel loro contatore, come le monete.
            groundItems.remove(at);
            player.addKeys(key.get(), item.count());
            return Optional.of(new ItemPickedUp(item.id(), item.kind(), at));
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

    /**
     * Prende un pezzo di un oggetto da lanciare: l'inventario ne ha uno in meno, e la pila sparisce
     * con l'ultimo. Il colpo lo risolve chi chiama.
     *
     * @return danno e portata dell'oggetto
     * @throws IllegalArgumentException se {@code user} non è il player, l'oggetto non è nel suo
     *     inventario o non si lancia
     * @throws IllegalStateException se il player è morto
     */
    public Throwing spendThrowable(EntityId user, EntityId itemId) {
        Item item = requireInInventory(user, itemId);
        Throwing throwing =
                item.throwing()
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                item.kind().value() + " non si lancia"));
        player.inventory().removeOne(itemId);
        return throwing;
    }

    private Item requireInInventory(EntityId user, EntityId itemId) {
        requirePlayer(user);
        requireAlive(user);
        return player.inventory()
                .find(itemId)
                .orElseThrow(() -> new IllegalArgumentException(itemId + " non è nell'inventario"));
    }

    /**
     * Il player usa l'oggetto fisso nella cella accanto: apre una porta o uno scrigno, tira una
     * leva. Un cancello si apre solo con la sua leva, e un oggetto già aperto non fa niente.
     *
     * @return gli eventi di quello che è successo; vuoto se non è successo niente
     * @throws IllegalArgumentException se {@code user} non è il player o la cella non è accanto
     * @throws IllegalStateException se il player è morto
     */
    public List<GameEvent> interact(EntityId user, Position at) {
        requirePlayer(user);
        requireAlive(user);
        if (!positionOf(user).isAdjacentTo(at)) {
            throw new IllegalArgumentException(at + " non è accanto al player");
        }
        Fixture fixture = fixtures.get(at);
        if (fixture == null || fixture.isOpen()) {
            return List.of();
        }
        return switch (fixture.type()) {
            case DOOR -> {
                fixture.open();
                changed();
                yield List.of(new DoorOpened(at));
            }
            case LOCKED_DOOR, BOSS_DOOR -> {
                KeyType needed =
                        fixture.type() == FixtureType.BOSS_DOOR ? KeyType.BOSS : KeyType.COMMON;
                if (!player.useKey(needed)) {
                    yield List.of(new DoorLocked(at));
                }
                fixture.open();
                changed();
                yield List.of(new DoorUnlocked(at));
            }
            case LEVER -> {
                fixture.open();
                for (Position gate : fixture.linked()) {
                    Fixture linked = fixtures.get(gate);
                    if (linked != null) {
                        linked.open();
                    }
                }
                changed();
                yield List.of(new LeverPulled(at, fixture.linked()));
            }
            case CHEST -> {
                Optional<Item> item = fixture.open();
                item.ifPresent(found -> groundItems.put(at, found));
                changed();
                yield List.of(new ChestOpened(at, item.map(Item::kind)));
            }
            case GATE, STAIRS -> List.of();
        };
    }

    /**
     * Mette un oggetto a terra, per esempio il bottino di un nemico morto.
     *
     * @return falso, senza fare niente, se la cella non si attraversa, ha già un oggetto o un
     *     oggetto fisso
     */
    public boolean dropItem(Position at, Item item) {
        if (!isWalkable(at) || groundItems.containsKey(at) || fixtures.containsKey(at)) {
            return false;
        }
        groundItems.put(at, item);
        return true;
    }

    /**
     * I nemici morti che non sono ancora stati tolti dal piano, cioè quelli morti in questo turno,
     * in ordine di id.
     */
    public List<Enemy> fallen() {
        List<Enemy> dead = new ArrayList<>();
        for (Enemy enemy : enemies.values()) {
            if (!enemy.isAlive()) {
                dead.add(enemy);
            }
        }
        return List.copyOf(dead);
    }

    /** Il negozio del piano, se c'è. */
    @Override
    public Optional<Shop> shop() {
        return Optional.ofNullable(shop);
    }

    /** Mette il negozio sul piano; lo fa la generazione. */
    public void openShop(Shop shop) {
        if (this.shop != null) {
            throw new IllegalStateException("Il piano ha già un negozio");
        }
        this.shop = shop;
    }

    /** La merce in vendita a terra nella cella, se c'è. */
    @Override
    public Optional<Item> wareAt(Position at) {
        Item item = groundItems.get(at);
        Shop current = shop;
        if (item == null || current == null || !current.priceOf(item.id()).isPresent()) {
            return Optional.empty();
        }
        return current.unpaid().contains(item.id()) ? Optional.empty() : Optional.of(item);
    }

    /**
     * Il player prende la merce su cui sta: va nell'inventario, da pagare.
     *
     * @return {@code WareTaken}, {@code InventoryFull} o vuoto se lì non c'è merce
     */
    public Optional<GameEvent> takeWare(EntityId user) {
        requirePlayer(user);
        requireAlive(user);
        Position at = positionOf(user);
        Optional<Item> ware = wareAt(at);
        Shop current = shop;
        if (!ware.isPresent() || current == null) {
            return Optional.empty();
        }
        Item item = ware.get();
        if (!player.inventory().add(item)) {
            return Optional.of(new InventoryFull(item.id(), item.kind(), at));
        }
        groundItems.remove(at);
        current.take(item.id());
        return Optional.of(
                new WareTaken(item.id(), item.kind(), current.priceOf(item.id()).orElse(0), at));
    }

    /**
     * Il player urta il mercante: paga tutto quello che ha preso, se le monete bastano.
     *
     * @return {@code WaresPaid}, {@code NotEnoughCoins} o {@code MerchantGreeted}
     */
    public GameEvent trade(EntityId user) {
        requirePlayer(user);
        Shop current = shop;
        Position at = positionOf(user);
        if (current == null || current.bill() == 0) {
            return new MerchantGreeted(at);
        }
        int bill = current.bill();
        if (player.coins() < bill) {
            return new NotEnoughCoins(bill, player.coins(), at);
        }
        player.spendCoins(bill);
        current.settle();
        return new WaresPaid(bill, at);
    }

    /**
     * Dopo un passo del player: se esce dalla zona del negozio con merce non pagata, il mercante si
     * arrabbia, e la merce presa resta al player.
     *
     * @return {@code MerchantAngered}, o vuoto
     */
    public Optional<GameEvent> checkTheft() {
        Shop current = shop;
        if (current == null || current.unpaid().isEmpty()) {
            return Optional.empty();
        }
        if (current.zone().contains(positionOf(player.id()))) {
            return Optional.empty();
        }
        Optional<Enemy> merchant = enemy(current.merchant());
        current.settle();
        if (!merchant.isPresent() || !merchant.get().isAlive()) {
            return Optional.empty();
        }
        merchant.get().anger();
        return Optional.of(new MerchantAngered(current.merchant(), positionOf(current.merchant())));
    }

    /** Gli oggetti fissi sono cambiati: le griglie combinate vanno ricalcolate. */
    private void changed() {
        movementGrid = null;
        sightGrid = null;
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
     * Il nemico che salta resta fermo un turno: dopo, sarà pronto a caricarsi.
     *
     * @throws IllegalStateException se il nemico è morto
     */
    public void rest(EntityId jumper) {
        requireLivingEnemy(jumper).rested();
    }

    /**
     * Il nemico si carica per saltare, restando a terra.
     *
     * @throws IllegalStateException se il nemico è morto o in aria
     */
    public JumpCharged chargeJump(EntityId jumper) {
        Enemy enemy = requireLivingEnemy(jumper);
        if (enemy.isAirborne()) {
            throw new IllegalStateException(jumper + " è in aria");
        }
        enemy.charged();
        return new JumpCharged(jumper, enemy.kind(), cellsOf(jumper));
    }

    /**
     * Il nemico si alza in aria sopra la sua ombra, con la cella di riferimento in {@code anchor}:
     * lascia le sue celle, non si può colpire, e al prossimo turno atterrerà lì.
     *
     * @throws IllegalStateException se il nemico è morto, già in aria, o non può atterrare lì
     */
    public JumpAnnounced announceJump(EntityId jumper, Position anchor) {
        Enemy enemy = requireLivingEnemy(jumper);
        if (enemy.isAirborne()) {
            throw new IllegalStateException(jumper + " è già in aria");
        }
        if (!canLand(jumper, anchor)) {
            throw new IllegalStateException(jumper + " non può atterrare in " + anchor);
        }
        enemy.liftedOff(anchor);
        positions.put(jumper, anchor);
        return new JumpAnnounced(jumper, enemy.footprint().at(anchor));
    }

    /**
     * Il nemico in aria atterra sulla sua ombra: chi era sotto viene spinto nella cella libera più
     * vicina fuori dall'impronta. Se un altro nemico occupa l'ombra, o non c'è una cella dove
     * spingere il player, resta in aria e ci riprova al turno dopo.
     *
     * @return {@code Landed} seguito da un {@code Moved} per ogni attore spinto via; vuoto se non
     *     era in aria o non è potuto atterrare
     */
    public List<GameEvent> land(EntityId jumper) {
        Enemy enemy = requireLivingEnemy(jumper);
        Optional<Position> target = enemy.pendingJump();
        if (!target.isPresent() || !canLand(jumper, target.get())) {
            return List.of();
        }
        List<Position> cells = enemy.footprint().at(target.get());
        Position playerAt = positionOf(player.id());
        boolean crushed = player.isAlive() && cells.contains(playerAt);
        Optional<Position> out = Optional.empty();
        if (crushed) {
            out = nearestFreeOutside(playerAt, cells);
            if (!out.isPresent()) {
                return List.of();
            }
        }
        List<GameEvent> events = new ArrayList<>();
        enemy.landed();
        events.add(new Landed(jumper, enemy.kind(), target.get(), target.get()));
        if (out.isPresent()) {
            positions.put(player.id(), out.get());
            events.add(new Moved(player.id(), player.kind(), playerAt, out.get()));
        }
        return List.copyOf(events);
    }

    /** Ricorda se l'ultima azione del nemico è stata allontanarsi dal player. */
    public void recordRetreat(EntityId enemy, boolean retreated) {
        requireLivingEnemy(enemy).retreated(retreated);
    }

    /** La cella libera più vicina a {@code from}, fuori da {@code forbidden}. */
    private Optional<Position> nearestFreeOutside(Position from, List<Position> forbidden) {
        java.util.ArrayDeque<Position> queue = new java.util.ArrayDeque<>();
        java.util.LinkedHashSet<Position> seen = new java.util.LinkedHashSet<>();
        queue.add(from);
        seen.add(from);
        while (!queue.isEmpty()) {
            Position cell = queue.poll();
            for (Direction direction : Direction.values()) {
                Position next = cell.translate(direction);
                if (!terrain.isInside(next) || !isWalkable(next) || !seen.add(next)) {
                    continue;
                }
                if (!forbidden.contains(next) && !actorAt(next).isPresent()) {
                    return Optional.of(next);
                }
                queue.add(next);
            }
        }
        return Optional.empty();
    }

    private Enemy requireLivingEnemy(EntityId id) {
        Enemy enemy =
                enemy(id).orElseThrow(() -> new IllegalArgumentException("Nessun nemico " + id));
        if (!enemy.isAlive()) {
            throw new IllegalStateException(id + " è morto");
        }
        return enemy;
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
                if (enemy.isBoss()) {
                    defeatedBoss = enemy.id();
                }
                iterator.remove();
                positions.remove(enemy.id());
                removed.add(enemy);
            }
        }
        return List.copyOf(removed);
    }

    /**
     * Rimette le celle visibili ed esplorate e il boss sconfitto salvati; solo per riprendere una
     * partita, senza ricalcolare il campo visivo.
     */
    void restoreMemory(CellSet visible, CellSet explored, Optional<EntityId> defeatedBoss) {
        if (visible.width() != terrain.width() || visible.height() != terrain.height()) {
            throw new IllegalArgumentException(
                    "Le celle visibili devono avere le dimensioni del piano");
        }
        this.visible = visible;
        this.explored = explored.union(visible);
        this.defeatedBoss = defeatedBoss.orElse(null);
    }

    /** Il boss già sconfitto e tolto dal piano, se c'è: per il salvataggio. */
    Optional<EntityId> defeatedBoss() {
        return Optional.ofNullable(defeatedBoss);
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
