package io.github.matteocardarelli.simpleroguelike.ui.game;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.matteocardarelli.simpleroguelike.command.Command;
import io.github.matteocardarelli.simpleroguelike.command.UseItemCommand;
import io.github.matteocardarelli.simpleroguelike.feedback.BuildInfo;
import io.github.matteocardarelli.simpleroguelike.feedback.CommandJournal;
import io.github.matteocardarelli.simpleroguelike.feedback.CommandLogFormat;
import io.github.matteocardarelli.simpleroguelike.feedback.FeedbackEntry;
import io.github.matteocardarelli.simpleroguelike.feedback.FeedbackStore;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.session.GameSession;
import io.github.matteocardarelli.simpleroguelike.session.TurnResult;
import io.github.matteocardarelli.simpleroguelike.snapshot.GameSnapshot;
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
import javax.inject.Inject;

/**
 * La partita e lo stato dell'interfaccia: sopravvivono alla rotazione, a differenza dell'Activity.
 * Un solo flusso: le azioni arrivano come {@link PlayerInput}, lo stato esce come {@link
 * GameUiState}.
 *
 * <p>L'inventario aperto è stato dell'interfaccia, non della partita: resta aperto dopo l'uso di un
 * oggetto e si chiude da solo a fine partita. Il registro dei comandi serve alle note di collaudo.
 *
 * <p>Se Android chiude l'app la partita non si salva: alla ripartenza {@link SavedStateHandle} ha
 * ancora il segno di una partita in corso, la sessione è nuova, e il giocatore viene avvisato.
 */
@HiltViewModel
public final class GameViewModel extends ViewModel implements PlayerInput {

    private static final String GAME_IN_PROGRESS = "game_in_progress";
    private static final DateTimeFormatter NOTE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT);

    private final GameSession session;
    private final SavedStateHandle saved;
    private final SeedSource seeds;
    private final FeedbackStore feedback;
    private final BuildInfo build;
    private final Clock clock;
    private final CommandJournal journal = new CommandJournal();
    private final CommandLogFormat format = new CommandLogFormat();
    private final MutableLiveData<GameUiState> uiState = new MutableLiveData<>();
    private boolean inventoryOpen;
    private boolean previousGameLost;

    /** Inizia subito una partita, e ricorda se Android aveva chiuso l'app durante quella prima. */
    @Inject
    public GameViewModel(
            GameSession session,
            SavedStateHandle saved,
            SeedSource seeds,
            FeedbackStore feedback,
            BuildInfo build,
            Clock clock) {
        this.session = session;
        this.saved = saved;
        this.seeds = seeds;
        this.feedback = feedback;
        this.build = build;
        this.clock = clock;
        this.previousGameLost = Boolean.TRUE.equals(saved.get(GAME_IN_PROGRESS));
        start();
    }

    public LiveData<GameUiState> uiState() {
        return uiState;
    }

    /** Lo stato pubblicato per ultimo. */
    public GameUiState state() {
        return Objects.requireNonNull(uiState.getValue());
    }

    /** Esegue un comando: se la partita è finita non succede niente. */
    @Override
    public void submit(Command command) {
        journal.record(command);
        TurnResult result = session.handle(command);
        if (result.snapshot().phase() != Phase.PLAYING) {
            inventoryOpen = false;
        }
        publish(result);
    }

    /** Apre o chiude l'inventario; a partita finita resta chiuso. */
    @Override
    public void toggleInventory() {
        inventoryOpen = !inventoryOpen && session.current().snapshot().phase() == Phase.PLAYING;
        publish(session.current());
    }

    /** Usa un oggetto dell'inventario, che resta aperto con la lista aggiornata. */
    @Override
    public void chooseItem(EntityId item) {
        submit(new UseItemCommand(item));
    }

    /** Chiude l'inventario senza toccare la partita. */
    public void closeInventory() {
        inventoryOpen = false;
        publish(session.current());
    }

    /** Inizia una partita nuova con il seed successivo, con l'inventario chiuso. */
    public void newGame() {
        start();
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
        TurnResult result = session.newGame(seed);
        saved.set(GAME_IN_PROGRESS, true);
        publish(result);
    }

    private void publish(TurnResult result) {
        uiState.setValue(new GameUiState(result, inventoryOpen, previousGameLost));
    }
}
