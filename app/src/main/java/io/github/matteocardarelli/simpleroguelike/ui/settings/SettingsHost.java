package io.github.matteocardarelli.simpleroguelike.ui.settings;

import io.github.matteocardarelli.simpleroguelike.settings.Settings;

/** Chi riceve le scelte delle impostazioni: l'Activity che ospita {@link SettingsDialog}. */
public interface SettingsHost {

    /** Le impostazioni di adesso, da mostrare all'apertura. */
    Settings currentSettings();

    /** Il seed della partita in corso: non è un'impostazione, ma si legge e si copia da qui. */
    long currentSeed();

    /**
     * Vero finché la partita non è vinta o persa: solo allora una partita nuova chiede conferma.
     */
    boolean gameInProgress();

    /** Il giocatore ha mostrato o nascosto il registro dei messaggi: vale subito. */
    void onLogVisibilityChanged(boolean visible);

    /** Il giocatore vuole una partita nuova, già confermata; le impostazioni si sono già chiuse. */
    void onNewGameRequested();

    /** Il giocatore vuole scrivere una nota di collaudo; le impostazioni si sono già chiuse. */
    void onWriteNoteRequested();
}
