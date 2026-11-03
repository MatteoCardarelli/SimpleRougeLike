package io.github.matteocardarelli.simpleroguelike.ui.game;

import static org.assertj.core.api.Assertions.assertThat;

import androidx.lifecycle.SavedStateHandle;
import io.github.matteocardarelli.simpleroguelike.command.MoveCommand;
import io.github.matteocardarelli.simpleroguelike.di.GameConfig;
import io.github.matteocardarelli.simpleroguelike.di.TestGraph;
import io.github.matteocardarelli.simpleroguelike.feedback.BuildInfo;
import io.github.matteocardarelli.simpleroguelike.feedback.FeedbackEntry;
import io.github.matteocardarelli.simpleroguelike.feedback.FeedbackReplay;
import io.github.matteocardarelli.simpleroguelike.feedback.FeedbackStore;
import io.github.matteocardarelli.simpleroguelike.generation.FixedFloorSource;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.save.GameStore;
import io.github.matteocardarelli.simpleroguelike.save.SavedGame;
import io.github.matteocardarelli.simpleroguelike.session.GameSession;
import io.github.matteocardarelli.simpleroguelike.settings.Settings;
import io.github.matteocardarelli.simpleroguelike.settings.SettingsStore;
import io.github.matteocardarelli.simpleroguelike.snapshot.Phase;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(InstantTaskExecutorExtension.class)
class GameViewModelTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-10-29T10:15:03Z"), ZoneOffset.UTC);

    @TempDir Path directory;

    private final AtomicLong seeds = new AtomicLong(100);
    private final MemorySettingsStore settings = new MemorySettingsStore();
    private final MemoryGameStore games = new MemoryGameStore();

    /** La partita salvata in memoria, come la terrebbe il file tra un avvio e l'altro. */
    private static final class MemoryGameStore implements GameStore {
        private @Nullable SavedGame saved;

        @Override
        public Optional<SavedGame> load() {
            return Optional.ofNullable(saved);
        }

        @Override
        public void save(SavedGame game) {
            saved = game;
        }

        @Override
        public void clear() {
            saved = null;
        }
    }

    /** Le impostazioni in memoria, come le terrebbe il telefono tra un avvio e l'altro. */
    private static final class MemorySettingsStore implements SettingsStore {
        private Settings saved = Settings.DEFAULT;

        @Override
        public Settings load() {
            return saved;
        }

        @Override
        public void save(Settings settings) {
            saved = settings;
        }
    }

    private GameViewModel viewModel() {
        return viewModel(TestGraph.session(GameConfig.standard()), new SavedStateHandle());
    }

    private GameViewModel drawn(String... rows) {
        return viewModel(
                TestGraph.session(GameConfig.standard(), new FixedFloorSource(rows)),
                new SavedStateHandle());
    }

    private GameViewModel viewModel(GameSession session, SavedStateHandle saved) {
        return new GameViewModel(
                session,
                saved,
                seeds::getAndIncrement,
                new FeedbackStore(directory.toFile()),
                new BuildInfo("1.0", "abc"),
                CLOCK,
                settings,
                games,
                TestGraph.saveCodec());
    }

    /** Sconfitta del player: contro il muro aspetta, e i tre goblin attaccano a ogni turno. */
    private static void loseTheGame(GameViewModel viewModel) {
        for (int i = 0; i < 50 && phase(viewModel) == Phase.PLAYING; i++) {
            viewModel.submit(new MoveCommand(Direction.UP));
        }
    }

    private static Phase phase(GameViewModel viewModel) {
        return viewModel.state().result().snapshot().phase();
    }

    @Test
    void constructor_startsAGameWithTheFirstSeed_andPublishesIt() {
        GameViewModel viewModel = viewModel();

        GameUiState state = viewModel.uiState().getValue();

        assertThat(state).isNotNull();
        assertThat(state.result().snapshot().seed()).isEqualTo(100);
        assertThat(state.result().snapshot().turn()).isEqualTo(1);
        assertThat(state.inventoryOpen()).isFalse();
        assertThat(state.previousGameLost()).isFalse();
    }

    @Test
    void submit_savesTheGame_andAnotherViewModelResumesIt() {
        GameViewModel first = viewModel();
        for (Direction direction : Direction.values()) {
            first.submit(new MoveCommand(direction));
        }

        GameViewModel second = viewModel();

        assertThat(second.state().result().snapshot()).isEqualTo(first.state().result().snapshot());
        assertThat(second.state().previousGameLost()).isFalse();
        assertThat(seeds.get()).as("nessun seed nuovo").isEqualTo(101);
    }

    /** Dopo la ripresa le note di collaudo hanno ancora tutti i comandi dall'inizio. */
    @Test
    void resume_keepsTheCommandJournal_forTheFeedbackNotes() throws IOException {
        GameViewModel first = viewModel();
        first.submit(new MoveCommand(Direction.LEFT));
        first.submit(new MoveCommand(Direction.UP));

        GameViewModel second = viewModel();
        second.submit(new MoveCommand(Direction.RIGHT));
        second.saveFeedback("dopo la ripresa");

        FeedbackEntry entry =
                FeedbackStore.read(new FeedbackStore(directory.toFile()).list().get(0));
        assertThat(entry.seed()).isEqualTo(100);
        assertThat(entry.commands()).hasSize(3);
    }

    @Test
    void gameOver_clearsTheSave_soTheNextStartIsANewGame() {
        GameViewModel viewModel = drawn("#####", "#g@g#", "##g##", "#####");
        loseTheGame(viewModel);
        assertThat(phase(viewModel)).isEqualTo(Phase.GAME_OVER);

        assertThat(games.load()).isEmpty();
        assertThat(viewModel().state().result().snapshot().turn()).isEqualTo(1);
    }

    @Test
    void newGame_replacesTheSave() {
        GameViewModel viewModel = viewModel();
        viewModel.submit(new MoveCommand(Direction.LEFT));

        viewModel.newGame();

        assertThat(games.load()).get().extracting(SavedGame::journalSeed).isEqualTo(101L);
        assertThat(games.load().orElseThrow().commands()).isEmpty();
    }

    @Test
    void anUnreadableSave_isCleared_andANewGameStarts() {
        games.save(new SavedGame(5, List.of(), List.of("format 99")));

        GameViewModel viewModel = viewModel();

        assertThat(viewModel.state().result().snapshot().seed()).isEqualTo(100);
        assertThat(games.load()).get().extracting(SavedGame::journalSeed).isEqualTo(100L);
    }

    @Test
    void submit_publishesEachTurn_ofTheSameGame() {
        GameViewModel viewModel = viewModel();

        for (Direction direction : Direction.values()) {
            viewModel.submit(new MoveCommand(direction));
        }

        assertThat(viewModel.state().result().snapshot().seed()).isEqualTo(100);
        assertThat(viewModel.state().result().revision()).isEqualTo(1 + Direction.values().length);
    }

    @Test
    void newGame_usesTheNextSeed() {
        GameViewModel viewModel = viewModel();

        viewModel.newGame();

        assertThat(viewModel.state().result().snapshot().seed()).isEqualTo(101);
        assertThat(viewModel.state().result().snapshot().turn()).isEqualTo(1);
    }

    @Test
    void toggleInventory_opensThenCloses_withoutPlayingATurn() {
        GameViewModel viewModel = viewModel();

        viewModel.toggleInventory();
        GameUiState open = viewModel.state();
        viewModel.toggleInventory();

        assertThat(open.inventoryOpen()).isTrue();
        assertThat(viewModel.state().inventoryOpen()).isFalse();
        assertThat(viewModel.state().result().snapshot().turn()).isEqualTo(1);
    }

    /** L'inventario resta aperto dopo l'uso di un oggetto, con la lista aggiornata. */
    @Test
    void chooseItem_usesThePotion_andKeepsTheInventoryOpen() {
        // Senza nemici la partita sarebbe già vinta: il goblin la tiene aperta.
        GameViewModel viewModel = drawn("############", "#@!.......g#", "############");
        viewModel.submit(new MoveCommand(Direction.RIGHT));
        EntityId potion = viewModel.state().result().snapshot().inventory().get(0).id();
        viewModel.toggleInventory();

        viewModel.chooseItem(potion);

        assertThat(viewModel.state().inventoryOpen()).isTrue();
        assertThat(viewModel.state().result().snapshot().inventory()).isEmpty();
    }

    /**
     * Scegliere un'arma la equipaggia senza far passare il turno; sceglierla di nuovo la toglie.
     */
    @Test
    void chooseItem_onAWeapon_equipsAndUnequipsIt_withoutPlayingATurn() {
        GameViewModel viewModel = drawn("############", "#@/.......g#", "############");
        viewModel.submit(new MoveCommand(Direction.RIGHT));
        EntityId sword = viewModel.state().result().snapshot().inventory().get(0).id();
        int turn = viewModel.state().result().snapshot().turn();
        int attack = viewModel.state().result().snapshot().player().attack();

        viewModel.chooseItem(sword);

        assertThat(viewModel.state().result().snapshot().inventory().get(0).equipped()).isTrue();
        assertThat(viewModel.state().result().snapshot().player().attack()).isEqualTo(attack + 4);
        assertThat(viewModel.state().result().snapshot().turn()).isEqualTo(turn);

        viewModel.chooseItem(sword);

        assertThat(viewModel.state().result().snapshot().inventory().get(0).equipped()).isFalse();
        assertThat(viewModel.state().result().snapshot().player().attack()).isEqualTo(attack);
    }

    /**
     * Scegliere i coltelli chiude l'inventario e fa mirare; lo swipe lancia un coltello, che lascia
     * la pila con uno in meno.
     */
    @Test
    void chooseItem_onKnives_aims_andTheSwipeThrowsOne() {
        GameViewModel viewModel =
                drawn("############", "#@--.......#", "#.........g#", "############");
        viewModel.submit(new MoveCommand(Direction.RIGHT));
        viewModel.submit(new MoveCommand(Direction.RIGHT));
        EntityId knives = viewModel.state().result().snapshot().inventory().get(0).id();
        viewModel.toggleInventory();
        int turn = viewModel.state().result().snapshot().turn();

        viewModel.chooseItem(knives);

        assertThat(viewModel.state().aimingItem()).contains(knives);
        assertThat(viewModel.state().inventoryOpen()).isFalse();

        viewModel.swipe(Direction.RIGHT);

        assertThat(viewModel.state().aimingItem()).isEmpty();
        assertThat(viewModel.state().result().snapshot().turn()).isEqualTo(turn + 1);
        assertThat(viewModel.state().result().snapshot().inventory().get(0).count()).isEqualTo(1);
        assertThat(viewModel.state().result().snapshot().player().position())
                .isEqualTo(new Position(3, 1));
    }

    /** Il doppio tap mentre si mira annulla la mira, senza lanciare e senza aprire l'inventario. */
    @Test
    void toggleInventory_whileAiming_cancelsTheAim() {
        GameViewModel viewModel =
                drawn("############", "#@-........#", "#.........g#", "############");
        viewModel.submit(new MoveCommand(Direction.RIGHT));
        EntityId knives = viewModel.state().result().snapshot().inventory().get(0).id();
        viewModel.chooseItem(knives);

        viewModel.toggleInventory();

        assertThat(viewModel.state().aimingItem()).isEmpty();
        assertThat(viewModel.state().inventoryOpen()).isFalse();
        assertThat(viewModel.state().result().snapshot().inventory().get(0).count()).isEqualTo(1);
    }

    /** Un tocco seleziona e mostra la scheda; "Usa" consuma l'oggetto, che esce dalla selezione. */
    @Test
    void selectItem_thenChooseIt_usesThePotion_andClearsTheSelection() {
        GameViewModel viewModel = drawn("############", "#@!.......g#", "############");
        viewModel.submit(new MoveCommand(Direction.RIGHT));
        EntityId potion = viewModel.state().result().snapshot().inventory().get(0).id();
        viewModel.toggleInventory();

        viewModel.selectItem(potion);
        GameUiState selected = viewModel.state();
        viewModel.chooseItem(potion);

        assertThat(selected.selectedItem()).contains(potion);
        assertThat(selected.result().snapshot().turn()).isEqualTo(2);
        assertThat(viewModel.state().selectedItem()).isEmpty();
        assertThat(viewModel.state().inventoryOpen()).isTrue();
    }

    @Test
    void selectItem_notInTheInventory_isIgnored() {
        GameViewModel viewModel = viewModel();

        viewModel.selectItem(new EntityId(999));

        assertThat(viewModel.state().selectedItem()).isEmpty();
    }

    /** Dopo una rotazione la nuova Activity non ripete le animazioni già mostrate. */
    @Test
    void claimPlayback_isTrueOnlyTheFirstTimeForARevision() {
        GameViewModel viewModel = viewModel();
        long revision = viewModel.state().result().revision();

        boolean first = viewModel.claimPlayback(revision);
        boolean again = viewModel.claimPlayback(revision);
        viewModel.submit(new MoveCommand(Direction.UP));
        boolean next = viewModel.claimPlayback(viewModel.state().result().revision());

        assertThat(first).isTrue();
        assertThat(again).isFalse();
        assertThat(next).isTrue();
    }

    /**
     * Il registro tiene gli eventi visibili dei turni, sopravvive alla rotazione con il ViewModel.
     */
    @Test
    void submit_addsTheVisibleEventsToTheLog_andNewGameClearsIt() {
        GameViewModel viewModel = viewModel();

        for (Direction direction : Direction.values()) {
            viewModel.submit(new MoveCommand(direction));
        }
        int afterMoves = viewModel.state().log().size();
        viewModel.newGame();

        assertThat(afterMoves).isPositive();
        assertThat(viewModel.state().log()).isEmpty();
    }

    @Test
    void log_keepsOnlyTheLastEvents() {
        GameViewModel viewModel = viewModel();

        for (int i = 0; i < GameViewModel.LOG_SIZE; i++) {
            viewModel.submit(new MoveCommand(i % 2 == 0 ? Direction.LEFT : Direction.RIGHT));
        }

        assertThat(viewModel.state().log()).hasSizeLessThanOrEqualTo(GameViewModel.LOG_SIZE);
    }

    @Test
    void closeInventory_closesIt() {
        GameViewModel viewModel = viewModel();
        viewModel.toggleInventory();

        viewModel.closeInventory();

        assertThat(viewModel.state().inventoryOpen()).isFalse();
    }

    @Test
    void submit_whenThePlayerDies_closesTheInventory() {
        GameViewModel viewModel = drawn("#####", "#g@g#", "##g##", "#####");
        viewModel.toggleInventory();

        loseTheGame(viewModel);

        assertThat(phase(viewModel)).isEqualTo(Phase.GAME_OVER);
        assertThat(viewModel.state().inventoryOpen()).isFalse();
    }

    @Test
    void toggleInventory_afterTheGameEnded_staysClosed() {
        GameViewModel viewModel = drawn("#####", "#g@g#", "##g##", "#####");
        loseTheGame(viewModel);

        viewModel.toggleInventory();

        assertThat(viewModel.state().inventoryOpen()).isFalse();
    }

    @Test
    void newGame_closesTheInventory() {
        GameViewModel viewModel = viewModel();
        viewModel.toggleInventory();

        viewModel.newGame();

        assertThat(viewModel.state().inventoryOpen()).isFalse();
    }

    /** Dopo la chiusura del processo il segno di una partita in corso è ancora salvato. */
    @Test
    void constructor_afterTheProcessWasKilled_warnsThatTheGameWasLost() {
        SavedStateHandle restored = new SavedStateHandle(Map.of("game_in_progress", true));

        GameViewModel viewModel = viewModel(TestGraph.session(GameConfig.standard()), restored);

        assertThat(viewModel.state().previousGameLost()).isTrue();
        assertThat(viewModel.state().result().snapshot().turn()).isEqualTo(1);
    }

    @Test
    void acknowledgeLostGame_removesTheWarning() {
        SavedStateHandle restored = new SavedStateHandle(Map.of("game_in_progress", true));
        GameViewModel viewModel = viewModel(TestGraph.session(GameConfig.standard()), restored);

        viewModel.acknowledgeLostGame();

        assertThat(viewModel.state().previousGameLost()).isFalse();
    }

    @Test
    void constructor_marksTheGameAsInProgress() {
        SavedStateHandle saved = new SavedStateHandle();

        viewModel(TestGraph.session(GameConfig.standard()), saved);

        assertThat(saved.<Boolean>get("game_in_progress")).isTrue();
    }

    /** Una nota salvata e riletta basta a rigiocare la partita fino allo stesso stato. */
    @Test
    void saveFeedback_writesANoteThatReplaysToTheSameState() throws IOException {
        GameViewModel viewModel = viewModel();
        for (Direction direction :
                List.of(Direction.UP, Direction.UP, Direction.RIGHT, Direction.DOWN)) {
            viewModel.submit(new MoveCommand(direction));
        }
        viewModel.toggleInventory();

        boolean saved = viewModel.saveFeedback("  prova  ");

        List<File> notes = new FeedbackStore(directory.toFile()).list();
        FeedbackEntry note = FeedbackStore.read(notes.get(0));
        FeedbackReplay.Result replay = FeedbackReplay.play(note);
        assertThat(saved).isTrue();
        assertThat(notes).extracting(File::getName).containsExactly("2026-10-29T10-15-03.txt");
        assertThat(note.text()).isEqualTo("prova");
        assertThat(note.seed()).isEqualTo(100);
        assertThat(note.commit()).isEqualTo("abc");
        assertThat(note.commands())
                .containsExactly("move up", "move up", "move right", "move down");
        assertThat(note.inventoryOpen()).isTrue();
        assertThat(FeedbackReplay.differences(note, replay.last().snapshot())).isEmpty();
    }

    @Test
    void constructor_withoutSavedSettings_showsTheLog() {
        GameViewModel viewModel = viewModel();

        assertThat(viewModel.state().logVisible()).isTrue();
    }

    @Test
    void setLogVisible_publishesAndSavesTheChoice() {
        GameViewModel viewModel = viewModel();

        viewModel.setLogVisible(false);

        assertThat(viewModel.state().logVisible()).isFalse();
        assertThat(settings.load().logVisible()).isFalse();
    }

    @Test
    void constructor_readsTheSavedSettings_asAfterARestartOfTheApp() {
        viewModel().setLogVisible(false);

        GameViewModel restarted = viewModel();

        assertThat(restarted.state().logVisible()).isFalse();
        assertThat(restarted.settings().logVisible()).isFalse();
    }

    @Test
    void newGame_keepsTheSettings() {
        GameViewModel viewModel = viewModel();
        viewModel.setLogVisible(false);

        viewModel.newGame();

        assertThat(viewModel.state().logVisible()).isFalse();
    }

    @Test
    void saveFeedback_withAnEmptyText_savesNothing() throws IOException {
        GameViewModel viewModel = viewModel();

        boolean saved = viewModel.saveFeedback("   ");

        assertThat(saved).isFalse();
        assertThat(new FeedbackStore(directory.toFile()).list()).isEmpty();
    }

    @Test
    void saveFeedback_afterNewGame_hasOnlyTheNewGame() throws IOException {
        GameViewModel viewModel = viewModel();
        viewModel.submit(new MoveCommand(Direction.UP));
        viewModel.newGame();

        viewModel.saveFeedback("prova");

        FeedbackEntry note =
                FeedbackStore.read(new FeedbackStore(directory.toFile()).list().get(0));
        assertThat(note.seed()).isEqualTo(101);
        assertThat(note.commands()).isEmpty();
        assertThat(note.turn()).isEqualTo(1);
    }
}
