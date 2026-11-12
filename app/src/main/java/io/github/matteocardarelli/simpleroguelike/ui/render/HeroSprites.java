package io.github.matteocardarelli.simpleroguelike.ui.render;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import io.github.matteocardarelli.simpleroguelike.R;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Le immagini pixel art dell'eroe: ferma nelle quattro direzioni e il ciclo di camminata di sei
 * fotogrammi per direzione. Ogni immagine si carica una volta, la prima che serve, senza
 * ridimensionarla: la scala la sceglie chi disegna.
 */
final class HeroSprites {

    private static final Map<Direction, Integer> IDLE = new EnumMap<>(Direction.class);
    private static final Map<Direction, int[]> WALK = new EnumMap<>(Direction.class);

    static {
        IDLE.put(Direction.UP, R.drawable.hero_idle_north);
        IDLE.put(Direction.DOWN, R.drawable.hero_idle_south);
        IDLE.put(Direction.LEFT, R.drawable.hero_idle_west);
        IDLE.put(Direction.RIGHT, R.drawable.hero_idle_east);
        WALK.put(
                Direction.UP,
                new int[] {
                    R.drawable.hero_walk_north_0,
                    R.drawable.hero_walk_north_1,
                    R.drawable.hero_walk_north_2,
                    R.drawable.hero_walk_north_3,
                    R.drawable.hero_walk_north_4,
                    R.drawable.hero_walk_north_5
                });
        WALK.put(
                Direction.DOWN,
                new int[] {
                    R.drawable.hero_walk_south_0,
                    R.drawable.hero_walk_south_1,
                    R.drawable.hero_walk_south_2,
                    R.drawable.hero_walk_south_3,
                    R.drawable.hero_walk_south_4,
                    R.drawable.hero_walk_south_5
                });
        WALK.put(
                Direction.LEFT,
                new int[] {
                    R.drawable.hero_walk_west_0,
                    R.drawable.hero_walk_west_1,
                    R.drawable.hero_walk_west_2,
                    R.drawable.hero_walk_west_3,
                    R.drawable.hero_walk_west_4,
                    R.drawable.hero_walk_west_5
                });
        WALK.put(
                Direction.RIGHT,
                new int[] {
                    R.drawable.hero_walk_east_0,
                    R.drawable.hero_walk_east_1,
                    R.drawable.hero_walk_east_2,
                    R.drawable.hero_walk_east_3,
                    R.drawable.hero_walk_east_4,
                    R.drawable.hero_walk_east_5
                });
    }

    private final Resources resources;
    private final Map<Integer, Bitmap> cache = new HashMap<>();

    HeroSprites(Context context) {
        this.resources = context.getResources();
    }

    /** L'immagine da disegnare: ferma se {@code walkFrame} è -1, altrimenti quel fotogramma. */
    Bitmap bitmap(Direction facing, int walkFrame) {
        int id = walkFrame < 0 ? IDLE.get(facing) : WALK.get(facing)[walkFrame];
        @Nullable Bitmap bitmap = cache.get(id);
        if (bitmap == null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            bitmap = BitmapFactory.decodeResource(resources, id, options);
            cache.put(id, bitmap);
        }
        return bitmap;
    }
}
