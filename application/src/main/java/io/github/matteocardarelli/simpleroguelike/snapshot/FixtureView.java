package io.github.matteocardarelli.simpleroguelike.snapshot;

import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;

/**
 * Un oggetto fisso in una cella che il player ha visto: una porta, un cancello, una leva o uno
 * scrigno, aperto o chiuso.
 */
public record FixtureView(Position position, Kind kind, boolean open) {}
