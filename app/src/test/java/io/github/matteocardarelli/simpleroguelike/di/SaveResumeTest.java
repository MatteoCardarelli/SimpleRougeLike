package io.github.matteocardarelli.simpleroguelike.di;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.session.GameSession;
import io.github.matteocardarelli.simpleroguelike.session.SaveCodec;
import org.junit.jupiter.api.Test;

/**
 * La prova completa del salvataggio: la partita del test golden, salvata e ripresa in una sessione
 * nuova dopo ogni comando, dà lo stesso registro della partita giocata di fila. Usa i piani
 * generati, i contenuti e le regole veri, quindi copre tutto quello che un piano può contenere.
 */
class SaveResumeTest {

    @Test
    void goldenGame_resumedAfterEveryCommand_givesTheSameLog() {
        SaveCodec codec = TestGraph.saveCodec();

        assertThat(
                        GoldenGame.play(
                                session -> {
                                    GameSession resumed = TestGraph.session(GameConfig.standard());
                                    resumed.resume(codec, session.save(codec));
                                    return resumed;
                                }))
                .isEqualTo(GoldenGame.play());
    }
}
