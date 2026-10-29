package io.github.matteocardarelli.simpleroguelike.snapshot;

/** Una cella come la vede il player. */
public record CellView(boolean wall, Visibility visibility) {}
