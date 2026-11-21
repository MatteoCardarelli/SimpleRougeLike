package io.github.matteocardarelli.simpleroguelike.session;

import io.github.matteocardarelli.simpleroguelike.model.Kind;

/**
 * Quando il boss {@code boss} atterra, a volte dal soffitto cadono nemici di tipo {@code
 * reinforcement} nella sua stanza.
 */
public record CeilingFall(Kind boss, Kind reinforcement) {}
