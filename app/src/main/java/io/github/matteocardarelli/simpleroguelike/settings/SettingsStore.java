package io.github.matteocardarelli.simpleroguelike.settings;

/** Dove stanno le impostazioni tra un avvio e l'altro dell'app. */
public interface SettingsStore {

    /** Le impostazioni salvate, o {@link Settings#DEFAULT} se non ne è mai stata salvata una. */
    Settings load();

    /** Salva le impostazioni: il prossimo {@link #load()} le restituisce. */
    void save(Settings settings);
}
