package io.github.matteocardarelli.simpleroguelike.event;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.ArrayList;
import java.util.List;

/**
 * Un colpo a distanza: un coltello lanciato o una freccia. Il proiettile parte da {@code from} e
 * attraversa le celle di {@code path}, l'ultima delle quali è quella dove si ferma: un attore, o la
 * cella prima di un muro. Il danno, se c'è, è un {@link Damaged} separato; un colpo mancato è un
 * {@link Missed}.
 *
 * @param shooterKind il tipo di chi colpisce, per il messaggio
 * @param projectile il tipo del proiettile, per il messaggio e l'animazione
 */
public record Fired(
        EntityId shooter, Kind shooterKind, Kind projectile, Position from, List<Position> path)
        implements GameEvent {

    public Fired {
        path = List.copyOf(path);
    }

    @Override
    public List<Position> where() {
        List<Position> cells = new ArrayList<>(path.size() + 1);
        cells.add(from);
        cells.addAll(path);
        return List.copyOf(cells);
    }
}
