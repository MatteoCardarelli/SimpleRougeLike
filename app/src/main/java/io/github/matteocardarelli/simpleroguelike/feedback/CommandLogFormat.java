package io.github.matteocardarelli.simpleroguelike.feedback;

import io.github.matteocardarelli.simpleroguelike.command.Command;
import io.github.matteocardarelli.simpleroguelike.command.DropItemCommand;
import io.github.matteocardarelli.simpleroguelike.command.EquipCommand;
import io.github.matteocardarelli.simpleroguelike.command.MoveCommand;
import io.github.matteocardarelli.simpleroguelike.command.TakeWareCommand;
import io.github.matteocardarelli.simpleroguelike.command.ThrowCommand;
import io.github.matteocardarelli.simpleroguelike.command.UnequipCommand;
import io.github.matteocardarelli.simpleroguelike.command.UseItemCommand;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Scrive un comando come una riga di testo e la rilegge: {@code move up}, {@code use item=12}. È un
 * registro per classe di comando, non una catena di {@code instanceof}: un comando nuovo si
 * registra qui con la sua parola chiave. Le righe sono scritte a mano, senza {@code toString()} né
 * nomi di classi o costanti, che R8 può cambiare sul telefono.
 */
public final class CommandLogFormat {

    private final Map<Class<? extends Command>, Function<Command, String>> writers =
            new LinkedHashMap<>();
    private final Map<String, Function<String, Command>> readers = new LinkedHashMap<>();

    public CommandLogFormat() {
        register(
                MoveCommand.class,
                "move",
                command -> name(command.direction()),
                arguments -> new MoveCommand(direction(arguments)));
        register(
                UseItemCommand.class,
                "use",
                command -> "item=" + command.itemId().value(),
                arguments -> new UseItemCommand(new EntityId(itemId(arguments))));
        register(
                ThrowCommand.class,
                "throw",
                command -> "item=" + command.itemId().value() + " " + name(command.direction()),
                arguments -> {
                    String[] parts = arguments.split(" ");
                    if (parts.length != 2) {
                        throw new IllegalArgumentException("Lancio senza direzione: " + arguments);
                    }
                    return new ThrowCommand(new EntityId(itemId(parts[0])), direction(parts[1]));
                });
        register(
                TakeWareCommand.class,
                "take",
                command -> "item=" + command.itemId().value(),
                arguments -> new TakeWareCommand(new EntityId(itemId(arguments))));
        register(
                EquipCommand.class,
                "equip",
                command -> "item=" + command.itemId().value(),
                arguments -> new EquipCommand(new EntityId(itemId(arguments))));
        register(
                UnequipCommand.class,
                "unequip",
                command -> "item=" + command.itemId().value(),
                arguments -> new UnequipCommand(new EntityId(itemId(arguments))));
        register(
                DropItemCommand.class,
                "drop",
                command -> "item=" + command.itemId().value(),
                arguments -> new DropItemCommand(new EntityId(itemId(arguments))));
    }

    /**
     * La riga di un comando.
     *
     * @throws IllegalArgumentException se il comando non ha un formato
     */
    public String line(Command command) {
        Function<Command, String> writer = writers.get(command.getClass());
        if (writer == null) {
            throw new IllegalArgumentException(
                    "Nessun formato per " + command.getClass().getName());
        }
        return writer.apply(command);
    }

    /**
     * Il comando scritto in una riga da {@link #line}.
     *
     * @throws IllegalArgumentException se la riga non è un comando conosciuto
     */
    public Command parse(String line) {
        String trimmed = line.trim();
        int space = trimmed.indexOf(' ');
        String keyword = space < 0 ? trimmed : trimmed.substring(0, space);
        String arguments = space < 0 ? "" : trimmed.substring(space + 1).trim();
        Function<String, Command> reader = readers.get(keyword);
        if (reader == null) {
            throw new IllegalArgumentException("Comando sconosciuto: " + line);
        }
        return reader.apply(arguments);
    }

    /** Le classi di comando che hanno un formato, nell'ordine di registrazione. */
    public List<Class<? extends Command>> formattedTypes() {
        return List.copyOf(writers.keySet());
    }

    private <C extends Command> void register(
            Class<C> type,
            String keyword,
            Function<C, String> arguments,
            Function<String, C> reader) {
        writers.put(type, command -> keyword + " " + arguments.apply(type.cast(command)));
        readers.put(keyword, reader::apply);
    }

    private static String name(Direction direction) {
        return switch (direction) {
            case UP -> "up";
            case DOWN -> "down";
            case LEFT -> "left";
            case RIGHT -> "right";
        };
    }

    private static Direction direction(String name) {
        return switch (name) {
            case "up" -> Direction.UP;
            case "down" -> Direction.DOWN;
            case "left" -> Direction.LEFT;
            case "right" -> Direction.RIGHT;
            default -> throw new IllegalArgumentException("Direzione sconosciuta: " + name);
        };
    }

    private static int itemId(String arguments) {
        if (!arguments.startsWith("item=")) {
            throw new IllegalArgumentException("Oggetto mancante: " + arguments);
        }
        return Integer.parseInt(arguments.substring("item=".length()));
    }
}
