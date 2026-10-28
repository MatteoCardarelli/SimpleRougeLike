package io.github.matteocardarelli.simpleroguelike.squidlib;

import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Position;

/**
 * Conversioni tra le griglie del dominio e gli array di SquidLib, che si indicizzano {@code
 * [x][y]}.
 */
final class SquidGrids {

    private SquidGrids() {}

    /** Da una mappa di SquidLib: {@code '.'} è pavimento, ogni altro carattere è muro. */
    static Grid<TileType> toTerrain(char[][] map) {
        int width = map.length;
        int height = map[0].length;
        return Grid.of(
                width,
                height,
                cell -> map[cell.x()][cell.y()] == '.' ? TileType.FLOOR : TileType.WALL);
    }

    /** La resistenza alla vista di ogni cella: 1 per chi blocca lo sguardo, 0 per chi no. */
    static double[][] toResistance(Grid<TileType> terrain) {
        double[][] resistance = new double[terrain.width()][terrain.height()];
        for (Position cell : terrain.positions()) {
            resistance[cell.x()][cell.y()] = terrain.get(cell).isTransparent() ? 0.0 : 1.0;
        }
        return resistance;
    }
}
