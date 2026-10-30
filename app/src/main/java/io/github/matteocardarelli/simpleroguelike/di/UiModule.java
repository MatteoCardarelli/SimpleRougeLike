package io.github.matteocardarelli.simpleroguelike.di;

import android.content.Context;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ActivityComponent;
import dagger.hilt.android.qualifiers.ActivityContext;
import dagger.multibindings.Multibinds;
import io.github.matteocardarelli.simpleroguelike.dungeon.Player;
import io.github.matteocardarelli.simpleroguelike.ui.content.ContentPresentation;
import io.github.matteocardarelli.simpleroguelike.ui.log.EventMessageFormatter;
import io.github.matteocardarelli.simpleroguelike.ui.log.Texts;
import io.github.matteocardarelli.simpleroguelike.ui.playback.CompositeEventListener;
import io.github.matteocardarelli.simpleroguelike.ui.playback.EventListener;
import io.github.matteocardarelli.simpleroguelike.ui.playback.EventPlayer;
import io.github.matteocardarelli.simpleroguelike.ui.playback.InstantEventPlayer;
import io.github.matteocardarelli.simpleroguelike.ui.playback.Scene;
import java.util.Set;

/**
 * La riproduzione dei turni: vive quanto l'Activity, e dopo una rotazione si ricrea. Un suono o una
 * vibrazione si aggiungono come {@link EventListener} con {@code @IntoSet}; le animazioni
 * sostituiranno il riproduttore istantaneo in un metodo solo.
 */
@Module
@InstallIn(ActivityComponent.class)
public abstract class UiModule {

    private UiModule() {}

    /** Chi ascolta gli eventi mostrati: oggi nessuno, e l'insieme vuoto è ammesso. */
    @Multibinds
    abstract Set<EventListener> listeners();

    @Provides
    static EventListener eventListener(Set<EventListener> listeners) {
        return new CompositeEventListener(listeners);
    }

    @Provides
    static EventPlayer eventPlayer(EventListener listener) {
        return new InstantEventPlayer(listener);
    }

    @Provides
    static Scene scene() {
        return new Scene(Player.KIND);
    }

    /** I testi del registro, dalle risorse dell'Activity: nella lingua del telefono. */
    @Provides
    static EventMessageFormatter eventMessageFormatter(
            @ActivityContext Context context, ContentPresentation content) {
        Texts texts =
                new Texts() {
                    @Override
                    public String text(int id, Object... args) {
                        return context.getString(id, args);
                    }

                    @Override
                    public String quantity(int id, int count, Object... args) {
                        return context.getResources().getQuantityString(id, count, args);
                    }
                };
        return new EventMessageFormatter(texts, content);
    }
}
