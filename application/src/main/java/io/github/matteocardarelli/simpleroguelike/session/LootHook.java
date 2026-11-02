package io.github.matteocardarelli.simpleroguelike.session;

import io.github.matteocardarelli.simpleroguelike.dungeon.Enemy;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.Item;
import io.github.matteocardarelli.simpleroguelike.dungeon.ItemCatalog;
import io.github.matteocardarelli.simpleroguelike.dungeon.ItemDefinition;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.event.ItemDropped;
import io.github.matteocardarelli.simpleroguelike.generation.WeightedPicker;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.run.Run;
import java.util.ArrayList;
import java.util.List;

/**
 * Il bottino: a fine turno ogni nemico morto in quel turno lascia cadere un oggetto nella sua
 * cella, con la probabilità della sua definizione. L'oggetto si sceglie dal catalogo in proporzione
 * al peso, come sul piano; il caso viene dal flusso di gameplay, gli id dalla sequenza della
 * partita. Una cella che ha già un oggetto o un oggetto fisso non riceve niente.
 */
public final class LootHook implements TurnHook {

    /** Dopo le regole di fine turno che potrebbero uccidere qualcuno. */
    public static final int ORDER = 100;

    private final ItemCatalog items;

    public LootHook(ItemCatalog items) {
        this.items = items;
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public List<GameEvent> afterTurn(Run run) {
        Floor floor = run.floor();
        List<GameEvent> events = new ArrayList<>();
        for (Enemy enemy : floor.fallen()) {
            int chance = enemy.definition().dropChance();
            if (chance == 0 || run.gameplay().nextInt(100) >= chance) {
                continue;
            }
            ItemDefinition type =
                    WeightedPicker.pick(items.all(), ItemDefinition::spawnWeight, run.gameplay());
            Item item = new Item(run.ids().next(), type);
            Position at = floor.positionOf(enemy.id());
            if (floor.dropItem(at, item)) {
                events.add(new ItemDropped(item.id(), item.kind(), at));
            }
        }
        return List.copyOf(events);
    }
}
