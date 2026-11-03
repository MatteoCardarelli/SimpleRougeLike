package io.github.matteocardarelli.simpleroguelike.session;

import io.github.matteocardarelli.simpleroguelike.dungeon.EnemyCatalog;
import io.github.matteocardarelli.simpleroguelike.dungeon.EntityIdSequence;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorCodec;
import io.github.matteocardarelli.simpleroguelike.dungeon.ItemCatalog;
import io.github.matteocardarelli.simpleroguelike.random.SeededRandomSource;
import io.github.matteocardarelli.simpleroguelike.run.Run;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Il formato del salvataggio: righe di testo, con il seed della partita, il piano e il turno, il
 * prossimo id, lo stato dei due flussi casuali e il piano intero di {@link FloorCodec}. La prima
 * riga dice la versione del formato: un salvataggio di un formato diverso non si legge.
 */
public final class SaveCodec {

    /** La versione del formato; cambia quando un salvataggio vecchio non si può più leggere. */
    public static final int FORMAT = 1;

    private final FloorCodec floors;

    public SaveCodec(ItemCatalog items, EnemyCatalog enemies) {
        this.floors = new FloorCodec(items, enemies);
    }

    List<String> write(Run run, long seed) {
        List<String> lines = new ArrayList<>();
        lines.add("format " + FORMAT);
        lines.add("seed " + seed);
        lines.add("depth " + run.depth());
        lines.add("turn " + run.turn());
        lines.add("ids " + run.ids().peek());
        lines.add(random("generation", run.generationState()));
        lines.add(random("gameplay", run.gameplayState()));
        lines.addAll(floors.write(run.floor()));
        return Collections.unmodifiableList(lines);
    }

    /**
     * Rilegge un salvataggio scritto da {@link #write}.
     *
     * @throws IllegalArgumentException se le righe non sono un salvataggio valido di questo formato
     */
    Saved read(List<String> lines) {
        if (lines.size() < 8) {
            throw new IllegalArgumentException("Salvataggio troppo corto");
        }
        int format = Integer.parseInt(value(lines.get(0), "format")[0]);
        if (format != FORMAT) {
            throw new IllegalArgumentException("Formato del salvataggio non supportato: " + format);
        }
        long seed = Long.parseLong(value(lines.get(1), "seed")[0]);
        int depth = Integer.parseInt(value(lines.get(2), "depth")[0]);
        int turn = Integer.parseInt(value(lines.get(3), "turn")[0]);
        int nextId = Integer.parseInt(value(lines.get(4), "ids")[0]);
        SeededRandomSource.State generation = random(lines.get(5), "generation");
        SeededRandomSource.State gameplay = random(lines.get(6), "gameplay");
        List<String> rest = lines.subList(7, lines.size());
        if (FloorCodec.length(rest) != rest.size()) {
            throw new IllegalArgumentException("Righe in più dopo il piano salvato");
        }
        Floor floor = floors.read(rest);
        return new Saved(
                seed,
                Run.restore(
                        floor, new EntityIdSequence(nextId), generation, gameplay, depth, turn));
    }

    /** Una partita riletta: il seed con cui era iniziata e la run da riprendere. */
    record Saved(long seed, Run run) {}

    private static String random(String tag, SeededRandomSource.State state) {
        return tag + " " + state.seed() + " " + state.gamma();
    }

    private static SeededRandomSource.State random(String line, String tag) {
        String[] parts = value(line, tag);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Riga " + tag + " non valida");
        }
        return new SeededRandomSource.State(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
    }

    /** I campi dopo il tipo, controllando che la riga sia del tipo atteso. */
    private static String[] value(String line, String tag) {
        String[] parts = line.split(" ", -1);
        if (parts.length < 2 || !parts[0].equals(tag)) {
            throw new IllegalArgumentException("Attesa la riga " + tag + ", trovata: " + line);
        }
        String[] values = new String[parts.length - 1];
        System.arraycopy(parts, 1, values, 0, values.length);
        return values;
    }
}
