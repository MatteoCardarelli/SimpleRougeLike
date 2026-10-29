package io.github.matteocardarelli.simpleroguelike.feedback;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Le note di collaudo in una cartella, un file di testo UTF-8 per nota, con il nome dalla data e
 * ora. Sul telefono la cartella sta nella memoria privata dell'app: nessun permesso, e le note
 * spariscono disinstallando l'app.
 */
public final class FeedbackStore {

    private final File directory;

    public FeedbackStore(File directory) {
        this.directory = directory;
    }

    /**
     * Salva la nota in un file nuovo: due note nello stesso secondo non si sovrascrivono.
     *
     * @return il file scritto
     */
    public File save(FeedbackEntry entry) throws IOException {
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Impossibile creare la cartella " + directory);
        }
        String name = entry.createdAt().replace(':', '-');
        File file = new File(directory, name + ".txt");
        for (int copy = 2; file.exists(); copy++) {
            file = new File(directory, name + "-" + copy + ".txt");
        }
        String content = String.join("\n", entry.toLines()) + "\n";
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    /** Le note salvate, in ordine di nome, cioè di data. */
    public List<File> list() {
        File[] files = directory.listFiles((parent, name) -> name.endsWith(".txt"));
        if (files == null) {
            return List.of();
        }
        List<File> sorted = new ArrayList<>(Arrays.asList(files));
        sorted.sort(Comparator.comparing(File::getName));
        return List.copyOf(sorted);
    }

    /** Rilegge una nota salvata. */
    public static FeedbackEntry read(File file) throws IOException {
        return FeedbackEntry.parse(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
    }
}
