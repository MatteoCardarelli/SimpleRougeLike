package io.github.matteocardarelli.simpleroguelike.ui.playback;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;

/**
 * Un attore come lo disegna la UI: coordinate in celle, con la virgola, perché un'animazione lo può
 * mostrare a metà strada tra due celle.
 */
public final class Sprite {

    private final EntityId id;
    private final Kind kind;
    private float x;
    private float y;
    private float alpha = 1f;
    private final int size;

    public Sprite(EntityId id, Kind kind, float x, float y) {
        this(id, kind, x, y, 1);
    }

    /**
     * Uno sprite grande {@code size} celle per lato, con {@code (x, y)} l'angolo in alto a
     * sinistra.
     */
    public Sprite(EntityId id, Kind kind, float x, float y, int size) {
        this.id = id;
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.size = size;
    }

    /** Il lato in celle: 1 per un attore normale. */
    public int size() {
        return size;
    }

    public EntityId id() {
        return id;
    }

    public Kind kind() {
        return kind;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float alpha() {
        return alpha;
    }

    public void moveTo(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }
}
