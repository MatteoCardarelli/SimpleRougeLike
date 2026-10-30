package io.github.matteocardarelli.simpleroguelike.feedback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.matteocardarelli.simpleroguelike.BuildConfig;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

/**
 * Rigioca una nota copiata dal telefono: si esegue con {@code ./gradlew :app:replayFeedback
 * -Pnote=<file>}, negli altri casi si salta. Scrive il registro della partita accanto alla nota, in
 * {@code app/build/feedback}, e controlla che la partita arrivi allo stato salvato.
 */
class FeedbackReplayTest {

    @Test
    void replay_reachesTheStateSavedInTheNote() throws IOException {
        String path = System.getProperty("srl.feedbackNote");
        assumeTrue(
                path != null && !path.isEmpty(),
                "Si esegue con ./gradlew :app:replayFeedback -Pnote=<file>");
        File noteFile = new File(path);
        assertThat(noteFile).as("nota da rigiocare").exists();
        FeedbackEntry note = FeedbackStore.read(noteFile);

        FeedbackReplay.Result replay = FeedbackReplay.play(note);

        List<String> differences = FeedbackReplay.differences(note, replay.last().snapshot());
        File log =
                new File(
                        Objects.requireNonNull(System.getProperty("srl.feedbackDir")),
                        noteFile.getName().replace(".txt", "") + "-replay.txt");
        List<String> lines = new ArrayList<>(replay.log());
        lines.addAll(differences.isEmpty() ? List.of("stato uguale alla nota") : differences);
        Files.createDirectories(log.getParentFile().toPath());
        Files.write(
                log.toPath(), (String.join("\n", lines) + "\n").getBytes(StandardCharsets.UTF_8));
        System.out.println("Registro della partita rigiocata: " + log);

        String commit =
                note.commit().equals(BuildConfig.GIT_COMMIT)
                        ? ""
                        : " La nota è del commit "
                                + note.commit()
                                + ", il codice è "
                                + BuildConfig.GIT_COMMIT
                                + ": rigiocarla su quel commit, per esempio con git worktree.";
        assertThat(differences)
                .as("stato della nota rispetto alla partita rigiocata." + commit)
                .isEmpty();
    }
}
