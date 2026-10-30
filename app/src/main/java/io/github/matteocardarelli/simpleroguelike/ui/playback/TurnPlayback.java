package io.github.matteocardarelli.simpleroguelike.ui.playback;

import io.github.matteocardarelli.simpleroguelike.session.TurnResult;

/**
 * Mostra i risultati dei turni sulla scena, uno alla volta. Un risultato con la stessa revisione di
 * quello già mostrato non tocca la scena: aprire l'inventario non interrompe un'animazione.
 *
 * <p>Se arriva una revisione nuova mentre una riproduzione è in corso, quella in corso salta alla
 * fine e poi si riproduce la nuova, senza avvisare in mezzo che la riproduzione è finita.
 */
public final class TurnPlayback {

    private final EventPlayer player;
    private final Scene scene;
    private long shownRevision = -1;
    private boolean busy;
    private long playing;
    private Runnable onIdle = () -> {};

    public TurnPlayback(EventPlayer player, Scene scene) {
        this.player = player;
        this.scene = scene;
    }

    /**
     * Mostra un risultato.
     *
     * @param animate falso per allineare subito la scena, per esempio dopo una rotazione, quando il
     *     risultato era già stato riprodotto
     */
    public void show(TurnResult result, boolean animate) {
        if (result.revision() == shownRevision) {
            return;
        }
        shownRevision = result.revision();
        if (busy) {
            // La riproduzione interrotta si chiude senza avvisare: ne parte subito un'altra.
            playing = -1;
            player.finish();
        }
        if (!animate) {
            busy = false;
            scene.syncWith(result.snapshot());
            return;
        }
        busy = true;
        long revision = result.revision();
        playing = revision;
        player.play(
                result.events(),
                scene,
                () -> {
                    scene.syncWith(result.snapshot());
                    if (playing == revision) {
                        busy = false;
                        onIdle.run();
                    }
                });
    }

    public boolean isBusy() {
        return busy;
    }

    /** Chi chiamare quando una riproduzione finisce: chi teneva in coda un'azione. */
    public void setOnIdle(Runnable onIdle) {
        this.onIdle = onIdle;
    }
}
