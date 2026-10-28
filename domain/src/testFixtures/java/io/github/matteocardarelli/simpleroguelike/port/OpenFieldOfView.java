package io.github.matteocardarelli.simpleroguelike.port;

import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.model.CellSet;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.ArrayList;
import java.util.List;

/**
 * Un {@link FieldOfView} finto: vede tutto il quadrato di lato {@code 2 * radius + 1} intorno
 * all'origine, attraverso i muri. Passa lo stesso contratto dell'adapter vero.
 */
public final class OpenFieldOfView implements FieldOfView {

    @Override
    public CellSet visibleFrom(Grid<TileType> terrain, Position origin, int radius) {
        List<Position> visible = new ArrayList<>();
        for (Position cell : terrain.positions()) {
            if (Math.abs(cell.x() - origin.x()) <= radius
                    && Math.abs(cell.y() - origin.y()) <= radius) {
                visible.add(cell);
            }
        }
        return CellSet.of(terrain.width(), terrain.height(), visible);
    }
}
