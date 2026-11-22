package io.github.matteocardarelli.simpleroguelike.ui.sound;

/** Chi suona davvero un effetto: sul telefono {@code SoundPoolOutput}, nei test un finto. */
public interface SoundOutput {

    void play(Sound sound);
}
