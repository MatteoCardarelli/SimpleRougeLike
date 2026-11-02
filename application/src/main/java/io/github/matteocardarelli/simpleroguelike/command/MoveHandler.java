package io.github.matteocardarelli.simpleroguelike.command;

import io.github.matteocardarelli.simpleroguelike.rules.ActionResult;
import io.github.matteocardarelli.simpleroguelike.rules.ActionRules;
import io.github.matteocardarelli.simpleroguelike.run.Run;

/**
 * Un passo del player, con la stessa regola dei nemici. Il turno passa sempre, anche se il passo è
 * bloccato da un muro: il player resta fermo e i nemici agiscono, così si può aspettare un turno
 * spingendo contro un muro.
 */
public final class MoveHandler implements CommandHandler<MoveCommand> {

    private final ActionRules actions;

    public MoveHandler(ActionRules actions) {
        this.actions = actions;
    }

    @Override
    public Class<MoveCommand> commandType() {
        return MoveCommand.class;
    }

    @Override
    public CommandOutcome handle(MoveCommand command, Run run) {
        ActionResult result =
                actions.step(
                        run.floor(), run.floor().playerId(), command.direction(), run.gameplay());
        return new CommandOutcome(result.events(), true);
    }
}
