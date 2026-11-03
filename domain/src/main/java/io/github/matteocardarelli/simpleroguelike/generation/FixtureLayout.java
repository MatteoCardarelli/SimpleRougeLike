package io.github.matteocardarelli.simpleroguelike.generation;

import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Dove stanno gli oggetti fissi di un piano, scelti prima di attori e oggetti.
 *
 * <ul>
 *   <li>Le porte stanno nei corridoi: celle con il muro ai due lati e il pavimento davanti e
 *       dietro.
 *   <li>Un cancello sta in un corridoio che, chiuso, separa dal resto del piano una zona piccola:
 *       dentro c'è uno scrigno, la leva sta in un muro fuori dalla zona, e il player nasce fuori.
 *   <li>Gli scrigni sparsi e le scale stanno fuori dai corridoi, per non chiudere passaggi.
 * </ul>
 *
 * Tutte le scelte vengono dal flusso casuale di generazione, nello stesso ordine: lo stesso seed dà
 * lo stesso piano.
 *
 * @param gates i cancelli, ognuno con la sua leva e la sua zona
 * @param doors le celle delle porte
 * @param chests le celle degli scrigni, compresi quelli nelle zone chiuse
 * @param closed le celle delle zone chiuse, dove il player non nasce
 * @param stairs la cella delle scale, fuori dai corridoi e dalle zone chiuse, se il piano le ha
 * @param lockedDoors le celle delle porte chiuse a chiave, scelte tra le porte
 */
record FixtureLayout(
        List<Gate> gates,
        List<Position> doors,
        List<Position> chests,
        Set<Position> closed,
        Optional<Position> stairs,
        List<Position> lockedDoors) {

    /** Un cancello, la leva nel muro che lo apre e la zona che chiude. */
    record Gate(Position at, Position lever, Set<Position> zone) {}

    /** Quanti corridoi provare per un cancello prima di rinunciare. */
    private static final int GATE_ATTEMPTS = 30;

    /** La zona chiusa più piccola che valga uno scrigno. */
    private static final int MIN_ZONE = 6;

    FixtureLayout {
        gates = List.copyOf(gates);
        doors = List.copyOf(doors);
        lockedDoors = List.copyOf(lockedDoors);
        chests = List.copyOf(chests);
        closed = Collections.unmodifiableSet(new LinkedHashSet<>(closed));
    }

    /** Tutte le celle occupate da un oggetto fisso sul pavimento. */
    Set<Position> floorCells() {
        Set<Position> cells = new LinkedHashSet<>();
        for (Gate gate : gates) {
            cells.add(gate.at());
        }
        cells.addAll(doors);
        cells.addAll(lockedDoors);
        cells.addAll(chests);
        stairs.ifPresent(cells::add);
        return cells;
    }

    static FixtureLayout plan(Grid<TileType> terrain, FloorSpec spec, RandomSource random) {
        return plan(terrain, spec, random, Collections.emptySet());
    }

    /**
     * Come l'altro, lasciando stare le celle {@code reserved}, come l'arena del boss: niente
     * oggetti fissi lì, e il player non ci nasce.
     */
    static FixtureLayout plan(
            Grid<TileType> terrain, FloorSpec spec, RandomSource random, Set<Position> reserved) {
        List<Position> corridors = new ArrayList<>();
        List<Position> walkable = new ArrayList<>();
        for (Position cell : terrain.positions()) {
            if (walkable(terrain, cell) && !reserved.contains(cell)) {
                walkable.add(cell);
                if (isCorridor(terrain, cell)) {
                    corridors.add(cell);
                }
            }
        }
        Set<Position> used = new LinkedHashSet<>();
        Set<Position> closed = new LinkedHashSet<>(reserved);
        List<Gate> gates = new ArrayList<>();
        List<Position> chests = new ArrayList<>();
        List<Position> candidates = new ArrayList<>(corridors);
        for (int g = 0; g < spec.gateCount(); g++) {
            for (int attempt = 0; attempt < GATE_ATTEMPTS && !candidates.isEmpty(); attempt++) {
                Position at = candidates.remove(random.nextInt(candidates.size()));
                if (closed.contains(at) || touches(used, at)) {
                    continue;
                }
                Set<Position> zone = smallerSide(terrain, at, closed, walkable.size());
                if (zone.isEmpty()) {
                    continue;
                }
                List<Position> walls = leverWalls(terrain, at, zone, closed);
                List<Position> inside = new ArrayList<>(zone);
                inside.removeIf(cell -> isCorridor(terrain, cell));
                if (walls.isEmpty() || inside.isEmpty()) {
                    continue;
                }
                Position lever = walls.get(random.nextInt(walls.size()));
                Position chest = inside.get(random.nextInt(inside.size()));
                gates.add(new Gate(at, lever, zone));
                closed.addAll(zone);
                used.add(at);
                used.add(chest);
                chests.add(chest);
                break;
            }
        }
        List<Position> doors = new ArrayList<>();
        candidates = new ArrayList<>(corridors);
        candidates.removeIf(cell -> used.contains(cell) || closed.contains(cell));
        while (doors.size() < spec.doorCount() && !candidates.isEmpty()) {
            Position at = candidates.remove(random.nextInt(candidates.size()));
            if (!touches(used, at)) {
                doors.add(at);
                used.add(at);
            }
        }
        List<Position> open = new ArrayList<>(walkable);
        open.removeIf(
                cell -> used.contains(cell) || closed.contains(cell) || isCorridor(terrain, cell));
        Optional<Position> stairs = Optional.empty();
        if (spec.stairsCount() > 0 && !open.isEmpty()) {
            stairs = Optional.of(open.remove(random.nextInt(open.size())));
        }
        for (int c = 0; c < spec.chestCount() && !open.isEmpty(); c++) {
            Position at = open.remove(random.nextInt(open.size()));
            chests.add(at);
            used.add(at);
        }
        List<Position> locked =
                new ArrayList<>(doors.subList(0, Math.min(spec.lockedDoorCount(), doors.size())));
        List<Position> unlocked = new ArrayList<>(doors.subList(locked.size(), doors.size()));
        return new FixtureLayout(gates, unlocked, chests, closed, stairs, locked);
    }

    private static boolean walkable(Grid<TileType> terrain, Position cell) {
        return terrain.isInside(cell) && terrain.get(cell).isWalkable();
    }

    /** Muro a sinistra e a destra e pavimento sopra e sotto, o il contrario. */
    private static boolean isCorridor(Grid<TileType> terrain, Position cell) {
        boolean left = walkable(terrain, cell.translate(Direction.LEFT));
        boolean right = walkable(terrain, cell.translate(Direction.RIGHT));
        boolean up = walkable(terrain, cell.translate(Direction.UP));
        boolean down = walkable(terrain, cell.translate(Direction.DOWN));
        return (!left && !right && up && down) || (left && right && !up && !down);
    }

    /** Vero se la cella o una vicina ha già un oggetto fisso: due porte attaccate non servono. */
    private static boolean touches(Set<Position> used, Position cell) {
        if (used.contains(cell)) {
            return true;
        }
        for (Direction direction : Direction.values()) {
            if (used.contains(cell.translate(direction))) {
                return true;
            }
        }
        return false;
    }

    /**
     * La parte più piccola del piano che un cancello in {@code gate} separerebbe dal resto, se è
     * abbastanza grande da contenere uno scrigno e non più di un terzo del piano.
     *
     * @return vuota se il cancello non separa niente di adatto
     */
    private static Set<Position> smallerSide(
            Grid<TileType> terrain, Position gate, Set<Position> closed, int total) {
        List<Position> sides = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            Position side = gate.translate(direction);
            if (walkable(terrain, side)) {
                sides.add(side);
            }
        }
        if (sides.size() != 2) {
            return Collections.emptySet();
        }
        Set<Position> first = reachable(terrain, sides.get(0), gate, closed);
        if (first.contains(sides.get(1))) {
            return Collections.emptySet();
        }
        Set<Position> second = reachable(terrain, sides.get(1), gate, closed);
        Set<Position> zone = first.size() <= second.size() ? first : second;
        if (zone.size() < MIN_ZONE || zone.size() > total / 3) {
            return Collections.emptySet();
        }
        return zone;
    }

    /** Le celle attraversabili raggiungibili da {@code start} senza passare da {@code blocked}. */
    private static Set<Position> reachable(
            Grid<TileType> terrain, Position start, Position blocked, Set<Position> closed) {
        Set<Position> seen = new LinkedHashSet<>();
        Deque<Position> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            Position cell = queue.poll();
            for (Direction direction : Direction.values()) {
                Position next = cell.translate(direction);
                if (!next.equals(blocked)
                        && !closed.contains(next)
                        && walkable(terrain, next)
                        && seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        return seen;
    }

    /**
     * I muri dove può stare la leva: accanto a una cella di pavimento fuori dalla zona, che non sia
     * il cancello.
     */
    private static List<Position> leverWalls(
            Grid<TileType> terrain, Position gate, Set<Position> zone, Set<Position> closed) {
        Set<Position> walls = new LinkedHashSet<>();
        for (Position cell : terrain.positions()) {
            if (!walkable(terrain, cell)
                    || zone.contains(cell)
                    || closed.contains(cell)
                    || cell.equals(gate)) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                Position wall = cell.translate(direction);
                if (terrain.isInside(wall) && !walkable(terrain, wall)) {
                    walls.add(wall);
                }
            }
        }
        return new ArrayList<>(walls);
    }
}
