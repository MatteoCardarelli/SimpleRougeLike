package com.github.matteoCarda.simplerougelike.activity;

import androidx.lifecycle.ViewModel;
import io.github.matteocardarelli.simpleroguelike.command.Command;
import io.github.matteocardarelli.simpleroguelike.di.GameConfig;
import io.github.matteocardarelli.simpleroguelike.di.GameSessionFactory;
import io.github.matteocardarelli.simpleroguelike.session.GameSession;
import io.github.matteocardarelli.simpleroguelike.session.TurnResult;
import java.util.Random;
import java.util.function.LongSupplier;

/**
 * Tiene la sessione di gioco. Sopravvive alla rotazione, a differenza dell'Activity: girare il
 * telefono non fa più partire una partita nuova. Al posto del Singleton di prima, la partita sta
 * qui e nessun oggetto statico tiene un riferimento all'Activity, che può essere liberata.
 *
 * <p>È il ViewModel minimo che serve a portare l'app di oggi sul nuovo motore: lo stato
 * dell'inventario e l'osservazione con LiveData arriveranno con il ViewModel definitivo.
 */
public final class SessionViewModel extends ViewModel {

    private final GameSession session;
    private final LongSupplier seeds;

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

    /** Esegue un comando del player: se la partita è finita non succede niente. */
    public TurnResult handle(Command command) {
        return session.handle(command);
    }

    /** Inizia una partita nuova con il seed successivo. */
    public TurnResult newGame() {
        return session.newGame(seeds.getAsLong());
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
