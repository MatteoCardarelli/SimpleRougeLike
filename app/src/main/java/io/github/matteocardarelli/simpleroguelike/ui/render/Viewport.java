package io.github.matteocardarelli.simpleroguelike.ui.render;

/**
 * La parte di mappa che si vede: lo spostamento da applicare al disegno, in pixel, e le celle da
 * disegnare, estremi compresi.
 */
public record Viewport(
        float offsetX, float offsetY, int firstX, int firstY, int lastX, int lastY) {}
