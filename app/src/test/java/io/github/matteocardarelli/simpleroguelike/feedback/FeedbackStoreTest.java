package io.github.matteocardarelli.simpleroguelike.feedback;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FeedbackStoreTest {

    @TempDir Path directory;

    @Test
    void save_writesOneFileNamedAfterTheTime_andReadGivesTheSameNote() throws IOException {
        FeedbackStore store = new FeedbackStore(directory.resolve("feedback").toFile());
        FeedbackEntry entry = FeedbackEntryTest.entry("Il goblin attraversa il muro");

        File file = store.save(entry);

        assertThat(file.getName()).isEqualTo("2026-10-29T10-15-03.txt");
        assertThat(FeedbackStore.read(file)).isEqualTo(entry);
    }

    @Test
    void save_inTheSameSecond_doesNotOverwrite() throws IOException {
        FeedbackStore store = new FeedbackStore(directory.toFile());

        store.save(FeedbackEntryTest.entry("prima"));
        store.save(FeedbackEntryTest.entry("seconda"));

        assertThat(store.list())
                .extracting(File::getName)
                .containsExactly("2026-10-29T10-15-03-2.txt", "2026-10-29T10-15-03.txt");
        assertThat(FeedbackStore.read(store.list().get(0)).text()).isEqualTo("seconda");
    }

    @Test
    void list_withoutTheDirectory_isEmpty() {
        FeedbackStore store = new FeedbackStore(directory.resolve("missing").toFile());

        assertThat(store.list()).isEmpty();
    }
}
