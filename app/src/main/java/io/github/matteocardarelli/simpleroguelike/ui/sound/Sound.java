package io.github.matteocardarelli.simpleroguelike.ui.sound;

import io.github.matteocardarelli.simpleroguelike.R;

/** Gli effetti sonori del gioco: la risorsa in {@code res/raw} e il volume relativo. */
public enum Sound {
    STEP(R.raw.sfx_step, 0.4f),
    HIT(R.raw.sfx_hit),
    HURT(R.raw.sfx_hurt),
    DEATH(R.raw.sfx_death),
    DOOR(R.raw.sfx_door),
    LEVER(R.raw.sfx_lever),
    CHEST(R.raw.sfx_chest),
    COINS(R.raw.sfx_coins),
    PICKUP(R.raw.sfx_pickup),
    POTION(R.raw.sfx_potion),
    THROW(R.raw.sfx_throw),
    CHARGE(R.raw.sfx_charge),
    JUMP(R.raw.sfx_jump),
    LAND(R.raw.sfx_land),
    FALL(R.raw.sfx_fall),
    STAIRS(R.raw.sfx_stairs),
    TRAP(R.raw.sfx_trap),
    POISON(R.raw.sfx_poison),
    VICTORY(R.raw.sfx_victory),
    DEFEAT(R.raw.sfx_defeat);

    private final int resource;
    private final float volume;

    Sound(int resource) {
        this(resource, 1f);
    }

    Sound(int resource, float volume) {
        this.resource = resource;
        this.volume = volume;
    }

    /** L'id della risorsa in {@code res/raw}. */
    public int resource() {
        return resource;
    }

    /** Il volume da 0 a 1, relativo al volume dei contenuti multimediali del telefono. */
    public float volume() {
        return volume;
    }
}
