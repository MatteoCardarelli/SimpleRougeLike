package io.github.matteocardarelli.simpleroguelike.command;

import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.Item;
import io.github.matteocardarelli.simpleroguelike.dungeon.Throwing;
import io.github.matteocardarelli.simpleroguelike.rules.RangedRules;
import io.github.matteocardarelli.simpleroguelike.run.Run;
import java.util.Optional;

/**
 * Lancia un pezzo di un oggetto, come un coltello, e il turno passa. Il pezzo lanciato si consuma.
 * Un oggetto che non c'è più, o che non si lancia, non fa niente: l'interfaccia può aver mostrato
 * una lista vecchia.
 */
public final class ThrowHandler implements CommandHandler<ThrowCommand> {

    private final RangedRules ranged;

    public ThrowHandler(RangedRules ranged) {
        this.ranged = ranged;
    }

    @Override
    public Class<ThrowCommand> commandType() {
        return ThrowCommand.class;
    }

    @Override
    public CommandOutcome handle(ThrowCommand command, Run run) {
        Floor floor = run.floor();
        Optional<Item> item = floor.player().inventory().find(command.itemId());
        if (!item.isPresent() || !item.get().throwing().isPresent()) {
            return CommandOutcome.none();
        }
        Throwing throwing = floor.spendThrowable(floor.playerId(), command.itemId());
        return new CommandOutcome(
                ranged.shoot(
                        floor,
                        floor.playerId(),
                        command.direction(),
                        throwing.range(),
                        item.get().kind(),
                        throwing.damage(),
                        run.gameplay()),
                true);
    }
}
