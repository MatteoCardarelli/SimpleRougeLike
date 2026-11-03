package io.github.matteocardarelli.simpleroguelike.save;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * La partita salvata in un file di testo UTF-8, nella memoria privata dell'app. Scrittura e
 * cancellazione vanno su {@code writer}, nell'ordine in cui arrivano, così il thread principale non
 * aspetta il disco; si scrive in un file accanto e poi lo si rinomina, così un'app chiusa a metà
 * scrittura lascia il salvataggio di prima.
 *
 * <p>Anche la lettura passa da {@code writer}, e la si aspetta: legge sempre l'ultima partita
 * salvata, anche se l'app è stata riaperta mentre una scrittura era ancora in coda. Si legge una
 * volta, all'avvio.
 */
public final class FileGameStore implements GameStore {

    /** Il nome del file, nella cartella dei file dell'app: i test strumentati lo cancellano. */
    public static final String FILE = "game.sav";

    private static final Logger LOG = Logger.getLogger(FileGameStore.class.getName());

    /** Un thread solo per tutto il processo: due store sullo stesso file restano in ordine. */
    private static final Executor DISK = Executors.newSingleThreadExecutor();

    private final File file;
    private final File temporary;
    private final Executor writer;

    /** Lo store dell'app, con le scritture sul thread del disco condiviso. */
    public FileGameStore(File file) {
        this(file, DISK);
    }

    /**
     * @param writer esegue letture e scritture una alla volta e nell'ordine, per esempio un
     *     executor con un solo thread
     */
    public FileGameStore(File file, Executor writer) {
        this.file = file;
        this.temporary = new File(file.getPath() + ".tmp");
        this.writer = writer;
    }

    @Override
    public Optional<SavedGame> load() {
        FutureTask<Optional<SavedGame>> read = new FutureTask<>(this::read);
        writer.execute(read);
        try {
            return read.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (ExecutionException e) {
            LOG.log(Level.WARNING, "Salvataggio non letto", e);
            return Optional.empty();
        }
    }

    private Optional<SavedGame> read() {
        if (!file.isFile()) {
            return Optional.empty();
        }
        try {
            return Optional.of(
                    SavedGame.parse(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)));
        } catch (IOException | IllegalArgumentException e) {
            LOG.log(Level.WARNING, "Salvataggio non leggibile: si inizia una partita nuova", e);
            return Optional.empty();
        }
    }

    @Override
    public void save(SavedGame game) {
        byte[] content = (String.join("\n", game.lines()) + "\n").getBytes(StandardCharsets.UTF_8);
        writer.execute(
                () -> {
                    try {
                        Files.write(temporary.toPath(), content);
                        Files.move(
                                temporary.toPath(),
                                file.toPath(),
                                StandardCopyOption.REPLACE_EXISTING,
                                StandardCopyOption.ATOMIC_MOVE);
                    } catch (IOException e) {
                        // Resta il salvataggio del turno prima; il prossimo turno riprova.
                        LOG.log(Level.WARNING, "Salvataggio non riuscito", e);
                    }
                });
    }

    @Override
    public void clear() {
        writer.execute(
                () -> {
                    try {
                        Files.deleteIfExists(file.toPath());
                        Files.deleteIfExists(temporary.toPath());
                    } catch (IOException e) {
                        LOG.log(Level.WARNING, "Salvataggio non cancellato", e);
                    }
                });
    }
}
