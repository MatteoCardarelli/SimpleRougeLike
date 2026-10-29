package io.github.matteocardarelli.simpleroguelike.feedback;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.snapshot.Phase;
import io.github.matteocardarelli.simpleroguelike.snapshot.PlayerView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Una nota di collaudo: il testo del giocatore e la partita nel momento in cui l'ha scritta. Con
 * seed e comandi la partita si rigioca identica, sullo stesso commit.
 *
 * <p>Nel file una chiave per riga, poi il testo tra due righe {@code ---}, poi un comando per riga,
 * scritto da {@link CommandLogFormat}. Il formato è scritto a mano, come il registro degli eventi.
 *
 * @param createdAt data e ora della nota, per esempio {@code 2026-10-29T10:15:03}
 * @param commit il commit da cui è compilata l'app, con {@code -dirty} se aveva modifiche
 * @param revision la revisione dell'ultimo risultato: cresce anche tra una partita e l'altra
 * @param commands i comandi della partita, dal suo inizio, come righe di {@link CommandLogFormat}
 */
public record FeedbackEntry(
        String createdAt,
        String appVersion,
        String commit,
        long seed,
        int turn,
        long revision,
        Phase phase,
        PlayerView player,
        boolean inventoryOpen,
        String text,
        List<String> commands) {

    private static final String SEPARATOR = "---";

    public FeedbackEntry {
        commands = List.copyOf(commands);
    }

    /** Le righe del file. */
    public List<String> toLines() {
        List<String> lines = new ArrayList<>();
        lines.add("created=" + createdAt);
        lines.add("version=" + appVersion);
        lines.add("commit=" + commit);
        lines.add("seed=" + seed);
        lines.add("turn=" + turn);
        lines.add("revision=" + revision);
        lines.add("phase=" + phaseName(phase));
        lines.add("player=" + player.id().value());
        lines.add("position=" + player.position().x() + "," + player.position().y());
        lines.add("health=" + player.health() + "/" + player.maxHealth());
        lines.add("level=" + player.level());
        lines.add("experience=" + player.experience() + "/" + player.experienceToNextLevel());
        lines.add("inventory_open=" + inventoryOpen);
        lines.add(SEPARATOR);
        // Né String.lines né Stream.toList esistono su Android 11.
        lines.addAll(Arrays.asList(text.split("\\R", -1)));
        lines.add(SEPARATOR);
        lines.addAll(commands);
        return List.copyOf(lines);
    }

    /**
     * Rilegge le righe scritte da {@link #toLines}. Il testo va dal primo all'ultimo separatore,
     * quindi può contenere anche righe {@code ---}.
     *
     * @throws IllegalArgumentException se manca una chiave o un separatore
     */
    public static FeedbackEntry parse(List<String> lines) {
        int first = lines.indexOf(SEPARATOR);
        int last = lines.lastIndexOf(SEPARATOR);
        if (first < 0 || first == last) {
            throw new IllegalArgumentException("Nota senza i due separatori " + SEPARATOR);
        }
        Map<String, String> keys = new LinkedHashMap<>();
        for (String line : lines.subList(0, first)) {
            int equals = line.indexOf('=');
            if (equals > 0) {
                keys.put(line.substring(0, equals), line.substring(equals + 1));
            }
        }
        String[] position = key(keys, "position").split(",");
        String[] health = key(keys, "health").split("/");
        String[] experience = key(keys, "experience").split("/");
        PlayerView player =
                new PlayerView(
                        new EntityId(Integer.parseInt(key(keys, "player"))),
                        new Position(Integer.parseInt(position[0]), Integer.parseInt(position[1])),
                        Integer.parseInt(health[0]),
                        Integer.parseInt(health[1]),
                        Integer.parseInt(key(keys, "level")),
                        Integer.parseInt(experience[0]),
                        Integer.parseInt(experience[1]));
        List<String> commands = new ArrayList<>();
        for (String line : lines.subList(last + 1, lines.size())) {
            if (!line.trim().isEmpty()) {
                commands.add(line.trim());
            }
        }
        return new FeedbackEntry(
                key(keys, "created"),
                key(keys, "version"),
                key(keys, "commit"),
                Long.parseLong(key(keys, "seed")),
                Integer.parseInt(key(keys, "turn")),
                Long.parseLong(key(keys, "revision")),
                phase(key(keys, "phase")),
                player,
                Boolean.parseBoolean(key(keys, "inventory_open")),
                String.join("\n", lines.subList(first + 1, last)),
                commands);
    }

    private static String key(Map<String, String> keys, String name) {
        String value = keys.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Nota senza " + name);
        }
        return value;
    }

    private static String phaseName(Phase phase) {
        return switch (phase) {
            case PLAYING -> "playing";
            case GAME_OVER -> "game_over";
            case VICTORY -> "victory";
        };
    }

    private static Phase phase(String name) {
        return switch (name) {
            case "playing" -> Phase.PLAYING;
            case "game_over" -> Phase.GAME_OVER;
            case "victory" -> Phase.VICTORY;
            default -> throw new IllegalArgumentException("Fase sconosciuta: " + name);
        };
    }
}
