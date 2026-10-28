package io.github.matteocardarelli.simpleroguelike.dungeon;

/** Il tipo di una cella del terreno. */
public enum TileType {
    FLOOR(true, true),
    WALL(false, false);

    private final boolean walkable;
    private final boolean transparent;

    TileType(boolean walkable, boolean transparent) {
        this.walkable = walkable;
        this.transparent = transparent;
    }

    public boolean isWalkable() {
        return walkable;
    }

    /** Se lo sguardo attraversa la cella. */
    public boolean isTransparent() {
        return transparent;
    }
}
