package io.github.matteocardarelli.simpleroguelike.di;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

/**
 * La partita di {@link GoldenGame} deve dare sempre lo stesso registro: quello salvato in {@code
 * src/test/resources/golden}. Se cambia una regola del gioco il registro cambia di proposito, e si
 * rigenera con {@code ./gradlew :app:updateGolden}; se cambia senza motivo, la partita non è più
 * deterministica. Lo stesso file lo confronta il test sul dispositivo.
 */
class GoldenGameTest {

    @Test
    void standardGame_matchesGoldenLog() throws IOException {
        List<String> log = GoldenGame.play();
        Path golden =
                Path.of(
                        Objects.requireNonNull(
                                System.getProperty("srl.goldenDir"),
                                "La cartella del golden la passa Gradle: eseguire con Gradle"),
                        GoldenGame.FILE);

        if (Boolean.getBoolean("srl.updateGolden")) {
            Files.createDirectories(golden.getParent());
            // Fine riga \n su ogni sistema: i test di :app compilano contro le API di Android,
            // che non hanno Files.writeString.
            Files.write(golden, (String.join("\n", log) + "\n").getBytes(StandardCharsets.UTF_8));
            return;
        }

        assertThat(golden).as("file atteso, da creare con ./gradlew :app:updateGolden").exists();
        assertThat(log).containsExactlyElementsOf(Files.readAllLines(golden));
    }

    @Test
    void standardGame_whenPlayedTwice_givesSameLog() {
        assertThat(GoldenGame.play()).isEqualTo(GoldenGame.play());
    }
}
