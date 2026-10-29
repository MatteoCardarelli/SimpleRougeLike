package com.github.matteoCarda.simplerougelike.activity;

import androidx.lifecycle.ViewModel;
import io.github.matteocardarelli.simpleroguelike.command.Command;
import io.github.matteocardarelli.simpleroguelike.di.GameConfig;
import io.github.matteocardarelli.simpleroguelike.di.GameSessionFactory;
import io.github.matteocardarelli.simpleroguelike.feedback.CommandJournal;
import io.github.matteocardarelli.simpleroguelike.feedback.CommandLogFormat;
import io.github.matteocardarelli.simpleroguelike.feedback.FeedbackEntry;
import io.github.matteocardarelli.simpleroguelike.session.GameSession;
import io.github.matteocardarelli.simpleroguelike.session.TurnResult;
import io.github.matteocardarelli.simpleroguelike.snapshot.GameSnapshot;
import io.github.matteocardarelli.simpleroguelike.snapshot.Phase;
import java.util.ArrayList;
import java.util.List;
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
    private final CommandJournal journal = new CommandJournal();
    private final CommandLogFormat format = new CommandLogFormat();
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
        start();
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
        journal.record(command);
        TurnResult result = session.handle(command);
        if (result.snapshot().phase() != Phase.PLAYING) {
            inventoryOpen = false;
        }
        return result;
    }

    /** Inizia una partita nuova con il seed successivo, con l'inventario chiuso. */
    public TurnResult newGame() {
        inventoryOpen = false;
        return start();
    }

    /**
     * Una nota di collaudo con il testo del giocatore e la partita di adesso: seed, comandi giocati
     * dall'inizio, stato del player e dell'inventario.
     */
    public FeedbackEntry feedback(String text, String createdAt, String appVersion, String commit) {
        TurnResult current = session.current();
        GameSnapshot snapshot = current.snapshot();
        List<String> commands = new ArrayList<>();
        for (Command command : journal.commands()) {
            commands.add(format.line(command));
        }
        return new FeedbackEntry(
                createdAt,
                appVersion,
                commit,
                journal.seed(),
                snapshot.turn(),
                current.revision(),
                snapshot.phase(),
                snapshot.player(),
                inventoryOpen,
                text,
                commands);
    }

    private TurnResult start() {
        long seed = seeds.getAsLong();
        journal.start(seed);
        return session.newGame(seed);
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
