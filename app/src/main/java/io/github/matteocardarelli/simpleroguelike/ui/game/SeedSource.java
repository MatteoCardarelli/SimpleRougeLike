package io.github.matteocardarelli.simpleroguelike.ui.game;

/**
 * Da dove arriva il seed di ogni partita nuova. Lo sceglie l'app, fisso o a caso: il dominio non
 * usa mai sorgenti non deterministiche.
 */
@FunctionalInterface
public interface SeedSource {

    long nextSeed();
}
