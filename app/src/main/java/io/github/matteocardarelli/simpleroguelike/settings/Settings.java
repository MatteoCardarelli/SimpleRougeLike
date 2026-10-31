package io.github.matteocardarelli.simpleroguelike.settings;

/**
 * Le impostazioni del giocatore, che sopravvivono alla chiusura dell'app.
 *
 * @param logVisible il registro dei messaggi si vede in fondo allo schermo
 */
public record Settings(boolean logVisible) {

    /** Le impostazioni di chi apre l'app per la prima volta. */
    public static final Settings DEFAULT = new Settings(true);

    /** Le stesse impostazioni con il registro visibile o nascosto. */
    public Settings withLogVisible(boolean visible) {
        return new Settings(visible);
    }
}
