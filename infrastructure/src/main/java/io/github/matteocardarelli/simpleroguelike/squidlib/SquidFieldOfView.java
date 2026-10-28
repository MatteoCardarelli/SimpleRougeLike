package io.github.matteocardarelli.simpleroguelike.squidlib;

import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.model.CellSet;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.port.FieldOfView;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import squidpony.squidgrid.FOV;
import squidpony.squidgrid.Radius;

/**
 * Il campo visivo con lo shadowcasting di SquidLib, in un cerchio di raggio {@code radius}
 * compreso: i muri fermano lo sguardo e sono visibili. Il calcolo di SquidLib non usa numeri
 * casuali.
 *
 * <p>La resistenza del terreno si calcola una volta per terreno: il terreno è immutabile, quindi
 * basta confrontare l'istanza. Il risultato dipende comunque solo dagli argomenti. Non è
 * thread-safe.
 */
public final class SquidFieldOfView implements FieldOfView {

    private @Nullable Grid<TileType> cachedTerrain;
    private double[][] resistance = new double[0][0];
    private double[][] light = new double[0][0];

    @Override
    public CellSet visibleFrom(Grid<TileType> terrain, Position origin, int radius) {
        // Confronto per istanza: il terreno è immutabile, e un terreno nuovo è un'istanza nuova.
        @SuppressWarnings("ReferenceEquality")
        boolean newTerrain = terrain != cachedTerrain;
        if (newTerrain) {
            cachedTerrain = terrain;
            resistance = SquidGrids.toResistance(terrain);
            light = new double[terrain.width()][terrain.height()];
        }
        // SquidLib dà luce 1 - d / raggio: a distanza uguale al raggio la luce è 0. Con radius + 1
        // si vede fino a radius compreso.
        FOV.reuseFOV(resistance, light, origin.x(), origin.y(), radius + 1, Radius.CIRCLE);
        List<Position> visible = new ArrayList<>();
        for (Position cell : terrain.positions()) {
            if (light[cell.x()][cell.y()] > 0) {
                visible.add(cell);
            }
        }
        return CellSet.of(terrain.width(), terrain.height(), visible);
    }
}
