package io.github.matteocardarelli.simpleroguelike.di;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Il test golden sul dispositivo: la stessa partita giocata su Android deve dare lo stesso registro
 * della JVM, cioè il file atteso del test JVM, impacchettato tra gli asset del test.
 */
@RunWith(AndroidJUnit4.class)
public class GoldenGameDeviceTest {

    @Test
    public void standardGame_onTheDevice_matchesTheGoldenLogOfTheJvm() throws IOException {
        List<String> expected = new ArrayList<>();
        try (BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                InstrumentationRegistry.getInstrumentation()
                                        .getContext()
                                        .getAssets()
                                        .open(GoldenGame.FILE),
                                StandardCharsets.UTF_8))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                expected.add(line);
            }
        }

        assertEquals(expected, GoldenGame.play());
    }
}
