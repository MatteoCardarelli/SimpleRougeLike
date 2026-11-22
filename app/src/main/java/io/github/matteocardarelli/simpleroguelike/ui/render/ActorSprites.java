package io.github.matteocardarelli.simpleroguelike.ui.render;

import androidx.annotation.DrawableRes;
import io.github.matteocardarelli.simpleroguelike.R;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Le immagini pixel art di nemici, mercante e slime gigante: una per direzione, tranne lo slime che
 * ha una posa sola. Chi non ha un'immagine, come la freccia in volo, resta a 0.
 */
final class ActorSprites {

    private static final Map<String, Map<Direction, Integer>> BY_KIND = new HashMap<>();

    static {
        add(
                "goblin",
                R.drawable.enemy_goblin_north,
                R.drawable.enemy_goblin_south,
                R.drawable.enemy_goblin_west,
                R.drawable.enemy_goblin_east);
        add(
                "archer",
                R.drawable.enemy_archer_north,
                R.drawable.enemy_archer_south,
                R.drawable.enemy_archer_west,
                R.drawable.enemy_archer_east);
        add(
                "wolf",
                R.drawable.enemy_wolf_north,
                R.drawable.enemy_wolf_south,
                R.drawable.enemy_wolf_west,
                R.drawable.enemy_wolf_east);
        add(
                "spider",
                R.drawable.enemy_spider_north,
                R.drawable.enemy_spider_south,
                R.drawable.enemy_spider_west,
                R.drawable.enemy_spider_east);
        add(
                "merchant",
                R.drawable.enemy_merchant_north,
                R.drawable.enemy_merchant_south,
                R.drawable.enemy_merchant_west,
                R.drawable.enemy_merchant_east);
        add(
                "giant_slime",
                R.drawable.boss_giant_slime,
                R.drawable.boss_giant_slime,
                R.drawable.boss_giant_slime,
                R.drawable.boss_giant_slime);
    }

    private ActorSprites() {}

    private static void add(
            String kind,
            @DrawableRes int up,
            @DrawableRes int down,
            @DrawableRes int left,
            @DrawableRes int right) {
        Map<Direction, Integer> map = new EnumMap<>(Direction.class);
        map.put(Direction.UP, up);
        map.put(Direction.DOWN, down);
        map.put(Direction.LEFT, left);
        map.put(Direction.RIGHT, right);
        BY_KIND.put(kind, map);
    }

    /** L'immagine del {@code Kind} rivolto verso {@code facing}, o 0 se non ne ha. */
    @DrawableRes
    static int sprite(Kind kind, Direction facing) {
        Map<Direction, Integer> map = BY_KIND.get(kind.value());
        return map == null ? 0 : map.get(facing);
    }

    /** L'immagine di chi guarda verso il basso, per schede e liste; 0 se non ne ha. */
    @DrawableRes
    static int front(Kind kind) {
        return sprite(kind, Direction.DOWN);
    }
}
