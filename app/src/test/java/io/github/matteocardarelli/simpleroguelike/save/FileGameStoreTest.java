package io.github.matteocardarelli.simpleroguelike.save;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileGameStoreTest {

    @TempDir Path directory;

    /** Le scritture subito, sul thread del test. */
    private FileGameStore store() {
        return new FileGameStore(new File(directory.toFile(), FileGameStore.FILE), Runnable::run);
    }

    private static SavedGame game(long seed) {
        return new SavedGame(seed, List.of("move up"), List.of("format 1", "seed " + seed));
    }

    @Test
    void load_withoutAFile_isEmpty() {
        assertThat(store().load()).isEmpty();
    }

    @Test
    void save_thenLoad_givesTheLastGame_alsoFromAnotherStore() {
        store().save(game(1));
        store().save(game(2));

        assertThat(store().load()).contains(game(2));
        assertThat(directory.toFile().list()).containsExactly(FileGameStore.FILE);
    }

    @Test
    void clear_deletesTheSave() {
        FileGameStore store = store();
        store.save(game(1));

        store.clear();

        assertThat(store.load()).isEmpty();
    }

    @Test
    void load_ofAnUnreadableFile_isEmpty() throws IOException {
        Files.write(
                directory.resolve(FileGameStore.FILE),
                "non un salvataggio\n".getBytes(StandardCharsets.UTF_8));

        assertThat(store().load()).isEmpty();
    }
}
