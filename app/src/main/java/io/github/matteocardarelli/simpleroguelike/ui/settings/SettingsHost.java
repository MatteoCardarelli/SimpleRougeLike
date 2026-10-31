package io.github.matteocardarelli.simpleroguelike.ui.settings;

import io.github.matteocardarelli.simpleroguelike.settings.Settings;

/** Chi riceve le scelte delle impostazioni: l'Activity che ospita {@link SettingsDialog}. */
public interface SettingsHost {

    /** Le impostazioni di adesso, da mostrare all'apertura. */
    Settings currentSettings();

    /** Il giocatore ha mostrato o nascosto il registro dei messaggi: vale subito. */
    void onLogVisibilityChanged(boolean visible);

    /** Il giocatore vuole scrivere una nota di collaudo; le impostazioni si sono già chiuse. */
    void onWriteNoteRequested();
}
