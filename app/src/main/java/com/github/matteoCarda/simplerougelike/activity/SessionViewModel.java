package com.github.matteoCarda.simplerougelike.activity;

import androidx.lifecycle.ViewModel;
import io.github.matteocardarelli.simpleroguelike.command.Command;
import io.github.matteocardarelli.simpleroguelike.di.GameConfig;
import io.github.matteocardarelli.simpleroguelike.di.GameSessionFactory;
import io.github.matteocardarelli.simpleroguelike.session.GameSession;
import io.github.matteocardarelli.simpleroguelike.session.TurnResult;
import io.github.matteocardarelli.simpleroguelike.snapshot.Phase;
import java.util.Random;
import java.util.function.LongSupplier;

/**
 * Tiene la sessione di gioco. Sopravvive alla rotazione, a differenza dell'Activity: girare il
 * telefono non fa più partire una partita nuova. Al posto del Singleton di prima, la partita sta
 * qui e nessun oggetto statico tiene un riferimento all'Activity, che può essere liberata.
 *
 * <p>Tiene anche l'inventario aperto, che è stato dell'interfaccia e non della partita: così resta
 * aperto dopo una rotazione.
 *
 * <p>È il ViewModel minimo che serve a portare l'app di oggi sul nuovo motore: l'osservazione con
 * LiveData arriverà con il ViewModel definitivo.
 */
public final class SessionViewModel extends ViewModel {

    private final GameSession session;
    private final LongSupplier seeds;
    private boolean inventoryOpen;

    public SessionViewModel() {
        this(GameConfig.standard());
    }

    private SessionViewModel(GameConfig config) {
        this(GameSessionFactory.create(config), seedsFrom(config));
    }

    /** Inizia subito una partita con il primo seed. */
    SessionViewModel(GameSession session, LongSupplier seeds) {
        this.session = session;
        this.seeds = seeds;
        session.newGame(seeds.getAsLong());
    }

    /** L'ultimo risultato, senza giocare niente. */
    public TurnResult current() {
        return session.current();
    }

    /**
     * Esegue un comando del player: se la partita è finita non succede niente. L'inventario aperto
     * resta aperto, anche dopo aver usato un oggetto, finché la partita non finisce.
     */
    public TurnResult handle(Command command) {
        TurnResult result = session.handle(command);
        if (result.snapshot().phase() != Phase.PLAYING) {
            inventoryOpen = false;
        }
        return result;
    }

    /** Inizia una partita nuova con il seed successivo, con l'inventario chiuso. */
    public TurnResult newGame() {
        inventoryOpen = false;
        return session.newGame(seeds.getAsLong());
    }

    public boolean isInventoryOpen() {
        return inventoryOpen;
    }

    /** Apre l'inventario se è chiuso e lo chiude se è aperto; a partita finita resta chiuso. */
    public void toggleInventory() {
        inventoryOpen = !inventoryOpen && session.current().snapshot().phase() == Phase.PLAYING;
    }

    /** Chiude l'inventario senza toccare la partita. */
    public void closeInventory() {
        inventoryOpen = false;
    }

    /** Il seed lo sceglie l'app: il dominio non usa mai sorgenti non deterministiche. */
    private static LongSupplier seedsFrom(GameConfig config) {
        if (config.fixedSeed().isPresent()) {
            long seed = config.fixedSeed().getAsLong();
            return () -> seed;
        }
        Random random = new Random();
        return random::nextLong;
    }
}
