package io.github.matteocardarelli.simpleroguelike.ui.sound;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import java.util.EnumMap;
import java.util.Map;

/**
 * Suona con un {@link SoundPool}: i 20 effetti si caricano una volta sola all'avvio, e si rilascia
 * tutto con {@link #release()} quando l'Activity si chiude. Segue il volume dei contenuti
 * multimediali del telefono.
 */
public final class SoundPoolOutput implements SoundOutput {

    private static final int MAX_STREAMS = 6;

    private final SoundPool pool;
    private final Map<Sound, Integer> ids = new EnumMap<>(Sound.class);

    public SoundPoolOutput(Context context) {
        AudioAttributes attributes =
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
        pool =
                new SoundPool.Builder()
                        .setMaxStreams(MAX_STREAMS)
                        .setAudioAttributes(attributes)
                        .build();
        for (Sound sound : Sound.values()) {
            ids.put(sound, pool.load(context, sound.resource(), 1));
        }
    }

    @Override
    public void play(Sound sound) {
        Integer id = ids.get(sound);
        if (id != null) {
            float volume = sound.volume();
            pool.play(id, volume, volume, 1, 0, 1f);
        }
    }

    /** Libera la memoria dei suoni. */
    public void release() {
        pool.release();
    }
}
