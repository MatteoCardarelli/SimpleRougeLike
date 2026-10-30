package io.github.matteocardarelli.simpleroguelike.di;

import android.app.Application;
import android.os.StrictMode;
import dagger.hilt.android.HiltAndroidApp;
import io.github.matteocardarelli.simpleroguelike.BuildConfig;

/** L'applicazione: il punto da cui Hilt crea gli oggetti dei suoi moduli. */
@HiltAndroidApp
public final class SimpleRogueLikeApp extends Application {

    /**
     * Nelle build di debug StrictMode scrive nel log il lavoro su disco o in rete fatto sul thread
     * principale, e le risorse non chiuse: problemi che in release si vedrebbero solo come scatti.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                    new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
            StrictMode.setVmPolicy(
                    new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
        }
    }
}
