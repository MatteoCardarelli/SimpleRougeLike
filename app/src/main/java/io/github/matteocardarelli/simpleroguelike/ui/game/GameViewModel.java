package io.github.matteocardarelli.simpleroguelike.ui.game;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.matteocardarelli.simpleroguelike.command.Command;
import io.github.matteocardarelli.simpleroguelike.command.EquipCommand;
import io.github.matteocardarelli.simpleroguelike.command.MoveCommand;
import io.github.matteocardarelli.simpleroguelike.command.ThrowCommand;
import io.github.matteocardarelli.simpleroguelike.command.UnequipCommand;
import io.github.matteocardarelli.simpleroguelike.command.UseItemCommand;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.feedback.BuildInfo;
import io.github.matteocardarelli.simpleroguelike.feedback.CommandJournal;
import io.github.matteocardarelli.simpleroguelike.feedback.CommandLogFormat;
import io.github.matteocardarelli.simpleroguelike.feedback.FeedbackEntry;
import io.github.matteocardarelli.simpleroguelike.feedback.FeedbackStore;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.save.GameStore;
import io.github.matteocardarelli.simpleroguelike.save.SavedGame;
import io.github.matteocardarelli.simpleroguelike.session.GameSession;
import io.github.matteocardarelli.simpleroguelike.session.SaveCodec;
import io.github.matteocardarelli.simpleroguelike.session.TurnResult;
import io.github.matteocardarelli.simpleroguelike.settings.Settings;
import io.github.matteocardarelli.simpleroguelike.settings.SettingsStore;
import io.github.matteocardarelli.simpleroguelike.snapshot.GameSnapshot;
import io.github.matteocardarelli.simpleroguelike.snapshot.InventoryEntry;
import io.github.matteocardarelli.simpleroguelike.snapshot.Phase;
import io.github.matteocardarelli.simpleroguelike.ui.input.PlayerInput;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import org.jspecify.annotations.Nullable;

/**
 * La partita e lo stato dell'interfaccia: sopravvivono alla rotazione, a differenza dell'Activity.
 * Un solo flusso: le azioni arrivano come {@link PlayerInput}, lo stato esce come {@link
 * GameUiState}.
 *
 * <p>L'inventario aperto e l'oggetto selezionato sono stato dell'interfaccia, non della partita:
 * l'inventario resta aperto dopo l'uso di un oggetto e si chiude da solo a fine partita. Il
 * registro dei comandi serve alle note di collaudo.
 *
 * <p>Ogni risultato si riproduce una volta sola: {@link #claimPlayback} lo segna, così dopo una
 * rotazione la nuova Activity mostra lo stato finale senza ripetere le animazioni.
 *
 * <p>La partita si salva dopo ogni comando, finché è in corso, e alla creazione si riprende quella
 * salvata; a fine partita il salvataggio si cancella. Se Android chiude l'app e il salvataggio
 * manca o non si legge, alla ripartenza {@link SavedStateHandle} ha ancora il segno di una partita
 * in corso, la sessione è nuova, e il giocatore viene avvisato.
 *
 * <p>Le impostazioni del giocatore si leggono una volta, alla creazione, e si salvano a ogni
 * cambiamento: restano anche dopo la chiusura dell'app, e una partita nuova non le tocca.
 */
@HiltViewModel
public final class GameViewModel extends ViewModel implements PlayerInput {

    private static final String GAME_IN_PROGRESS = "game_in_progress";

    /** Quanti eventi tiene il registro: abbastanza per qualche turno, sempre pochi. */
    static final int LOG_SIZE = 50;

    private static final DateTimeFormatter NOTE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT);

    private final GameSession session;
    private final SavedStateHandle saved;
    private final SeedSource seeds;
    private final FeedbackStore feedback;
    private final BuildInfo build;
    private final Clock clock;
    private final SettingsStore settingsStore;
    private final GameStore games;
    private final SaveCodec codec;
    private final CommandJournal journal = new CommandJournal();
    private final CommandLogFormat format = new CommandLogFormat();
    private final MutableLiveData<GameUiState> uiState = new MutableLiveData<>();
    private final List<GameEvent> log = new ArrayList<>();
    private boolean inventoryOpen;
    private @Nullable EntityId selectedItem;
    private @Nullable EntityId aimingItem;
    private boolean previousGameLost;
    private Settings settings;
    private long lastClaimedRevision;

    /**
     * Riprende la partita salvata o ne inizia una nuova, e ricorda se Android aveva chiuso l'app
     * durante quella prima senza che si potesse riprendere.
     */
    @Inject
    public GameViewModel(
            GameSession session,
            SavedStateHandle saved,
            SeedSource seeds,
            FeedbackStore feedback,
            BuildInfo build,
            Clock clock,
            SettingsStore settingsStore,
            GameStore games,
            SaveCodec codec) {
        this.session = session;
        this.saved = saved;
        this.seeds = seeds;
        this.feedback = feedback;
        this.build = build;
        this.clock = clock;
        this.settingsStore = settingsStore;
        this.games = games;
        this.codec = codec;
        this.settings = settingsStore.load();
        boolean resumed = resume();
        this.previousGameLost = !resumed && Boolean.TRUE.equals(saved.get(GAME_IN_PROGRESS));
        if (!resumed) {
            start();
        }
    }

    public LiveData<GameUiState> uiState() {
        return uiState;
    }

    /** Lo stato pubblicato per ultimo. */
    public GameUiState state() {
        return Objects.requireNonNull(uiState.getValue());
    }

    /**
     * Segna che un risultato sta per essere riprodotto.
     *
     * @return vero solo la prima volta per quella revisione: le volte dopo, per esempio dopo una
     *     rotazione, la scena si allinea senza animazioni
     */
    public boolean claimPlayback(long revision) {
        if (revision <= lastClaimedRevision) {
            return false;
        }
        lastClaimedRevision = revision;
        return true;
    }

    /** Esegue un comando: se la partita è finita non succede niente. */
    @Override
    public void submit(Command command) {
        journal.record(command);
        TurnResult result = session.handle(command);
        if (result.snapshot().phase() != Phase.PLAYING) {
            inventoryOpen = false;
        }
        if (!inInventory(result, selectedItem)) {
            selectedItem = null;
        }
        log.addAll(result.events());
        if (log.size() > LOG_SIZE) {
            log.subList(0, log.size() - LOG_SIZE).clear();
        }
        persist(result);
        publish(result);
    }

    /** Uno swipe: il lancio dell'oggetto scelto, se si sta mirando, altrimenti un passo. */
    @Override
    public void swipe(Direction direction) {
        EntityId item = aimingItem;
        aimingItem = null;
        submit(item != null ? new ThrowCommand(item, direction) : new MoveCommand(direction));
    }

    /**
     * Apre o chiude l'inventario; a partita finita resta chiuso. Mentre si mira, annulla la mira.
     */
    @Override
    public void toggleInventory() {
        if (aimingItem != null) {
            aimingItem = null;
            publish(session.current());
            return;
        }
        inventoryOpen = !inventoryOpen && session.current().snapshot().phase() == Phase.PLAYING;
        if (!inventoryOpen) {
            selectedItem = null;
        }
        publish(session.current());
    }

    /** Seleziona un oggetto dell'inventario e ne mostra la scheda, senza usarlo. */
    public void selectItem(EntityId item) {
        if (inInventory(session.current(), item)) {
            selectedItem = item;
            publish(session.current());
        }
    }

    /**
     * Usa un oggetto dell'inventario, che resta aperto con la lista aggiornata. Un oggetto da
     * equipaggiare si equipaggia, o si toglie se il player lo porta già.
     */
    @Override
    public void chooseItem(EntityId item) {
        Optional<InventoryEntry> entry = entry(session.current(), item);
        if (entry.isPresent() && entry.get().throwable()) {
            aimingItem = item;
            inventoryOpen = false;
            selectedItem = null;
            publish(session.current());
        } else if (entry.isPresent() && entry.get().slot().isPresent()) {
            submit(entry.get().equipped() ? new UnequipCommand(item) : new EquipCommand(item));
        } else {
            submit(new UseItemCommand(item));
        }
    }

    private static Optional<InventoryEntry> entry(TurnResult result, EntityId item) {
        for (InventoryEntry entry : result.snapshot().inventory()) {
            if (entry.id().equals(item)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    /** Chiude l'inventario senza toccare la partita. */
    public void closeInventory() {
        inventoryOpen = false;
        selectedItem = null;
        publish(session.current());
    }

    /** Inizia una partita nuova con il seed successivo, con l'inventario chiuso. */
    public void newGame() {
        start();
    }

    /** Le impostazioni di adesso. */
    public Settings settings() {
        return settings;
    }

    /** Mostra o nasconde il registro dei messaggi, e lo ricorda anche dopo la chiusura dell'app. */
    public void setLogVisible(boolean visible) {
        if (settings.logVisible() != visible) {
            settings = settings.withLogVisible(visible);
            settingsStore.save(settings);
            publish(session.current());
        }
    }

    /** Il giocatore ha letto l'avviso della partita persa. */
    public void acknowledgeLostGame() {
        previousGameLost = false;
        publish(session.current());
    }

    /**
     * Salva una nota di collaudo con la partita di adesso: seed, comandi giocati dall'inizio, stato
     * del player e dell'inventario.
     *
     * @return falso se il testo è vuoto e non c'era niente da salvare
     * @throws IOException se la nota non si può scrivere
     */
    public boolean saveFeedback(String text) throws IOException {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        feedback.save(feedbackEntry(trimmed));
        return true;
    }

    private FeedbackEntry feedbackEntry(String text) {
        TurnResult current = session.current();
        GameSnapshot snapshot = current.snapshot();
        List<String> commands = new ArrayList<>();
        for (Command command : journal.commands()) {
            commands.add(format.line(command));
        }
        return new FeedbackEntry(
                LocalDateTime.now(clock).format(NOTE_TIME),
                build.appVersion(),
                build.commit(),
                journal.seed(),
                snapshot.turn(),
                current.revision(),
                snapshot.phase(),
                snapshot.player(),
                inventoryOpen,
                text,
                commands);
    }

    private void start() {
        long seed = seeds.nextSeed();
        journal.start(seed);
        inventoryOpen = false;
        selectedItem = null;
        aimingItem = null;
        log.clear();
        TurnResult result = session.newGame(seed);
        saved.set(GAME_IN_PROGRESS, true);
        persist(result);
        publish(result);
    }

    /**
     * Riprende la partita salvata, se c'è: il registro dei comandi continua da dove era, quello dei
     * messaggi riparte vuoto.
     *
     * @return falso se non c'è un salvataggio o non si legge; uno illeggibile si cancella
     */
    private boolean resume() {
        Optional<SavedGame> game = games.load();
        if (!game.isPresent()) {
            return false;
        }
        try {
            List<Command> commands = new ArrayList<>();
            for (String line : game.get().commands()) {
                commands.add(format.parse(line));
            }
            TurnResult result = session.resume(codec, game.get().session());
            journal.start(game.get().journalSeed());
            for (Command command : commands) {
                journal.record(command);
            }
            saved.set(GAME_IN_PROGRESS, true);
            publish(result);
            return true;
        } catch (IllegalArgumentException e) {
            games.clear();
            return false;
        }
    }

    /** Salva la partita in corso, o cancella il salvataggio se è finita: la morte è definitiva. */
    private void persist(TurnResult result) {
        if (result.snapshot().phase() != Phase.PLAYING) {
            games.clear();
            return;
        }
        List<String> commands = new ArrayList<>();
        for (Command command : journal.commands()) {
            commands.add(format.line(command));
        }
        games.save(new SavedGame(journal.seed(), commands, session.save(codec)));
    }

    private void publish(TurnResult result) {
        uiState.setValue(
                new GameUiState(
                        result,
                        inventoryOpen,
                        Optional.ofNullable(selectedItem),
                        log,
                        previousGameLost,
                        settings.logVisible(),
                        Optional.ofNullable(aimingItem)));
    }

    private static boolean inInventory(TurnResult result, @Nullable EntityId item) {
        if (item == null) {
            return false;
        }
        for (InventoryEntry entry : result.snapshot().inventory()) {
            if (entry.id().equals(item)) {
                return true;
            }
        }
        return false;
    }
}
