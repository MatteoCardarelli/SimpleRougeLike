package io.github.matteocardarelli.simpleroguelike.settings;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Le impostazioni in un file di {@link SharedPreferences}, nella memoria privata dell'app. Il
 * salvataggio è asincrono ({@code apply}): non blocca il thread principale.
 */
public final class PreferencesSettingsStore implements SettingsStore {

    /** Il nome del file: i test strumentati lo svuotano prima di partire. */
    public static final String FILE = "settings";

    private static final String LOG_VISIBLE = "log_visible";

    private final SharedPreferences preferences;

    public PreferencesSettingsStore(Context context) {
        this.preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    @Override
    public Settings load() {
        return new Settings(preferences.getBoolean(LOG_VISIBLE, Settings.DEFAULT.logVisible()));
    }

    @Override
    public void save(Settings settings) {
        preferences.edit().putBoolean(LOG_VISIBLE, settings.logVisible()).apply();
    }
}
