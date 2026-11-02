package io.github.matteocardarelli.simpleroguelike.ui.render;

import io.github.matteocardarelli.simpleroguelike.model.Position;

/** Segue il player: la sua cella sta al centro della vista. Solo calcoli, senza Android. */
public final class Camera {

    private final float cellSizePx;

    public Camera(float cellSizePx) {
        if (cellSizePx <= 0) {
            throw new IllegalArgumentException("Cella non valida: " + cellSizePx);
        }
        this.cellSizePx = cellSizePx;
    }

    public float cellSizePx() {
        return cellSizePx;
    }

    /**
     * La parte di mappa da disegnare con {@code target} al centro di una vista grande {@code
     * viewWidth} per {@code viewHeight} pixel, su una griglia di {@code gridWidth} per {@code
     * gridHeight} celle. Le celle sono solo quelle della griglia, anche vicino ai bordi.
     */
    public Viewport centerOn(
            Position target, int viewWidth, int viewHeight, int gridWidth, int gridHeight) {
        return centerOn(target.x(), target.y(), viewWidth, viewHeight, gridWidth, gridHeight);
    }

    /**
     * Come {@link #centerOn(Position, int, int, int, int)}, con il centro anche tra due celle:
     * durante un passo la telecamera segue lo sprite del player mentre scivola.
     */
    public Viewport centerOn(
            float targetX,
            float targetY,
            int viewWidth,
            int viewHeight,
            int gridWidth,
            int gridHeight) {
        float offsetX = targetX * cellSizePx - viewWidth / 2f + cellSizePx / 2f;
        float offsetY = targetY * cellSizePx - viewHeight / 2f + cellSizePx / 2f;
        int firstX = Math.max(0, (int) Math.floor(offsetX / cellSizePx));
        int firstY = Math.max(0, (int) Math.floor(offsetY / cellSizePx));
        int lastX = Math.min(gridWidth - 1, (int) Math.floor((offsetX + viewWidth) / cellSizePx));
        int lastY = Math.min(gridHeight - 1, (int) Math.floor((offsetY + viewHeight) / cellSizePx));
        return new Viewport(offsetX, offsetY, firstX, firstY, lastX, lastY);
    }
}
