package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Un negozio: il mercante, le celle della sua zona e il prezzo di ogni merce, a terra o già presa e
 * non ancora pagata. Cambia solo con i metodi di {@link Floor}.
 */
public final class Shop {

    private final EntityId merchant;
    private final Set<Position> zone;
    private final Map<EntityId, Integer> prices;
    private final Set<EntityId> unpaid = new LinkedHashSet<>();

    /**
     * @param zone le celle dove il player può tenere la merce senza pagarla: il negozio e il suo
     *     bordo
     * @param prices il prezzo di ogni merce, in monete
     */
    public Shop(EntityId merchant, Set<Position> zone, Map<EntityId, Integer> prices) {
        this.merchant = merchant;
        this.zone = Collections.unmodifiableSet(new LinkedHashSet<>(zone));
        this.prices = new LinkedHashMap<>(prices);
    }

    public EntityId merchant() {
        return merchant;
    }

    public Set<Position> zone() {
        return zone;
    }

    /** Il prezzo di una merce, se è in vendita o presa e non pagata. */
    public java.util.Optional<Integer> priceOf(EntityId item) {
        return java.util.Optional.ofNullable(prices.get(item));
    }

    /** Tutti i prezzi, per il salvataggio. */
    public Map<EntityId, Integer> prices() {
        return Collections.unmodifiableMap(prices);
    }

    /** Le merci prese e non pagate. */
    public Set<EntityId> unpaid() {
        return Collections.unmodifiableSet(unpaid);
    }

    /** Quanto costa tutto quello che il player ha preso senza pagare. */
    public int bill() {
        int total = 0;
        for (EntityId item : unpaid) {
            total = Math.addExact(total, prices.getOrDefault(item, 0));
        }
        return total;
    }

    void take(EntityId item) {
        unpaid.add(item);
    }

    /** Tutto pagato, o rubato: le merci prese non sono più in vendita. */
    void settle() {
        for (EntityId item : unpaid) {
            prices.remove(item);
        }
        unpaid.clear();
    }
}
