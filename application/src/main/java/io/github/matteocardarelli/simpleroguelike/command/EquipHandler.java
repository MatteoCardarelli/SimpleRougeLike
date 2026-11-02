package io.github.matteocardarelli.simpleroguelike.command;

import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.Item;
import io.github.matteocardarelli.simpleroguelike.run.Run;
import java.util.Optional;

/**
 * Equipaggia un oggetto dell'inventario. Il turno non passa: cambiare equipaggiamento è una scelta
 * di preparazione, non un'azione di combattimento. Un oggetto che non c'è più, o che non si
 * equipaggia, non fa niente: l'interfaccia può aver mostrato una lista vecchia.
 */
public final class EquipHandler implements CommandHandler<EquipCommand> {

    @Override
    public Class<EquipCommand> commandType() {
        return EquipCommand.class;
    }

    @Override
    public CommandOutcome handle(EquipCommand command, Run run) {
        Floor floor = run.floor();
        Optional<Item> item = floor.player().inventory().find(command.itemId());
        if (!item.isPresent() || !item.get().equipment().isPresent()) {
            return CommandOutcome.none();
        }
        return new CommandOutcome(floor.equip(floor.playerId(), command.itemId()), false);
    }
}
