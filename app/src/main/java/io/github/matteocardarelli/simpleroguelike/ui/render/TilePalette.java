package io.github.matteocardarelli.simpleroguelike.ui.render;

import android.content.Context;
import io.github.matteocardarelli.simpleroguelike.R;
import io.github.matteocardarelli.simpleroguelike.snapshot.CellView;
import java.util.OptionalInt;

/**
 * I colori delle celle. Visibile ed esplorata si distinguono per luminosità, non solo per tinta: la
 * differenza resta leggibile anche senza percepire i colori.
 */
public final class TilePalette {

    private final int visibleFloor;
    private final int visibleWall;
    private final int exploredFloor;
    private final int exploredWall;

    public TilePalette(int visibleFloor, int visibleWall, int exploredFloor, int exploredWall) {
        this.visibleFloor = visibleFloor;
        this.visibleWall = visibleWall;
        this.exploredFloor = exploredFloor;
        this.exploredWall = exploredWall;
    }

    /** I colori delle risorse, scelti per significato. */
    public static TilePalette from(Context context) {
        return new TilePalette(
                context.getColor(R.color.tile_visible_floor),
                context.getColor(R.color.tile_visible_wall),
                context.getColor(R.color.tile_explored_floor),
                context.getColor(R.color.tile_explored_wall));
    }

    /** Il colore della cella; vuoto se non è mai stata vista, e allora non si disegna. */
    public OptionalInt cellColor(CellView cell) {
        return switch (cell.visibility()) {
            case VISIBLE -> OptionalInt.of(cell.wall() ? visibleWall : visibleFloor);
            case EXPLORED -> OptionalInt.of(cell.wall() ? exploredWall : exploredFloor);
            case UNEXPLORED -> OptionalInt.empty();
        };
    }

    /**
     * Il contrasto più basso tra una cella visibile e la stessa cella solo esplorata, come rapporto
     * di luminanza relativa (da 1 a 21).
     */
    public double minimumVisibilityContrast() {
        return Math.min(contrast(visibleFloor, exploredFloor), contrast(visibleWall, exploredWall));
    }

    /**
     * Il rapporto di contrasto tra due colori ARGB, calcolato a mano per non dipendere da Android.
     */
    static double contrast(int first, int second) {
        double a = luminance(first);
        double b = luminance(second);
        return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
    }

    private static double luminance(int color) {
        return 0.2126 * channel((color >> 16) & 0xFF)
                + 0.7152 * channel((color >> 8) & 0xFF)
                + 0.0722 * channel(color & 0xFF);
    }

    private static double channel(int value) {
        double c = value / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}
