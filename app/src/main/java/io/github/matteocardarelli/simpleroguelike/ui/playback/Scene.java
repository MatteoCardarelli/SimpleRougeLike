package io.github.matteocardarelli.simpleroguelike.ui.playback;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.snapshot.ActorView;
import io.github.matteocardarelli.simpleroguelike.snapshot.GameSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Gli attori che la UI disegna, uno sprite per id. A riposo coincide con lo snapshot; durante
 * un'animazione gli sprite si muovono tra la posizione di prima e quella di dopo. Vive quanto
 * l'Activity: dopo una rotazione si ricrea dallo snapshot.
 */
public final class Scene {

    private final Kind playerKind;
    private final Map<EntityId, Sprite> sprites = new LinkedHashMap<>();

    /**
     * @param playerKind il {@code Kind} dello sprite del player, che lo snapshot non porta
     */
    public Scene(Kind playerKind) {
        this.playerKind = playerKind;
    }

    public Optional<Sprite> sprite(EntityId id) {
        return Optional.ofNullable(sprites.get(id));
    }

    /**
     * Aggiunge uno sprite, per esempio un nemico che entra nel campo visivo durante un'animazione.
     */
    public Sprite spawn(EntityId id, Kind kind, Position at) {
        Sprite sprite = new Sprite(id, kind, at.x(), at.y());
        sprites.put(id, sprite);
        return sprite;
    }

    /** Gli sprite, nell'ordine in cui disegnarli: il player per ultimo, sopra a tutti. */
    public List<Sprite> sprites() {
        return Collections.unmodifiableList(new ArrayList<>(sprites.values()));
    }

    /**
     * Allinea la scena allo snapshot: il player e i nemici visibili, ognuno nella sua cella. Gli
     * sprite di chi non si vede più spariscono.
     */
    public void syncWith(GameSnapshot snapshot) {
        sprites.clear();
        for (ActorView enemy : snapshot.visibleEnemies()) {
            spawn(enemy.id(), enemy.kind(), enemy.position());
        }
        spawn(snapshot.player().id(), playerKind, snapshot.player().position());
    }
}
