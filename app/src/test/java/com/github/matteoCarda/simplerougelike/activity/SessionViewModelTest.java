package com.github.matteoCarda.simplerougelike.activity;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.command.MoveCommand;
import io.github.matteocardarelli.simpleroguelike.command.UseItemCommand;
import io.github.matteocardarelli.simpleroguelike.di.GameConfig;
import io.github.matteocardarelli.simpleroguelike.di.GameSessionFactory;
import io.github.matteocardarelli.simpleroguelike.feedback.FeedbackEntry;
import io.github.matteocardarelli.simpleroguelike.feedback.FeedbackReplay;
import io.github.matteocardarelli.simpleroguelike.feedback.FeedbackStore;
import io.github.matteocardarelli.simpleroguelike.generation.FixedFloorSource;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.session.TurnResult;
import io.github.matteocardarelli.simpleroguelike.snapshot.Phase;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SessionViewModelTest {

    private final AtomicLong seeds = new AtomicLong(100);

    private SessionViewModel viewModel() {
        return new SessionViewModel(
                GameSessionFactory.create(GameConfig.standard()), seeds::getAndIncrement);
    }

    @Test
    void constructor_startsGameWithFirstSeed() {
        SessionViewModel viewModel = viewModel();

        TurnResult result = viewModel.current();

        assertThat(result.snapshot().seed()).isEqualTo(100);
        assertThat(result.snapshot().turn()).isEqualTo(1);
    }

    @Test
    void current_whenCalledAgain_returnsSameResultWithoutStartingNewGame() {
        SessionViewModel viewModel = viewModel();
        TurnResult played = viewModel.handle(new MoveCommand(Direction.UP));

        TurnResult again = viewModel.current();

        assertThat(again).isSameAs(played);
    }

    @Test
    void handle_afterCommands_keepsSameGame() {
        SessionViewModel viewModel = viewModel();

        for (Direction direction : Direction.values()) {
            viewModel.handle(new MoveCommand(direction));
        }

        assertThat(viewModel.current().snapshot().seed()).isEqualTo(100);
        assertThat(viewModel.current().revision()).isEqualTo(1 + Direction.values().length);
    }

    @Test
    void newGame_usesNextSeed() {
        SessionViewModel viewModel = viewModel();

        TurnResult result = viewModel.newGame();

        assertThat(result.snapshot().seed()).isEqualTo(101);
        assertThat(result.snapshot().turn()).isEqualTo(1);
    }

    @Test
    void toggleInventory_opensThenCloses() {
        SessionViewModel viewModel = viewModel();

        viewModel.toggleInventory();
        boolean afterFirst = viewModel.isInventoryOpen();
        viewModel.toggleInventory();

        assertThat(afterFirst).isTrue();
        assertThat(viewModel.isInventoryOpen()).isFalse();
    }

    /** L'inventario resta aperto dopo l'uso di un oggetto, con la lista aggiornata. */
    @Test
    void handle_usingAPotion_keepsTheInventoryOpen() {
        // Senza nemici la partita sarebbe già vinta: il goblin la tiene aperta.
        SessionViewModel viewModel = drawn("############", "#@!.......g#", "############");
        viewModel.handle(new MoveCommand(Direction.RIGHT));
        EntityId potion = viewModel.current().snapshot().inventory().get(0).id();
        viewModel.toggleInventory();

        TurnResult result = viewModel.handle(new UseItemCommand(potion));

        assertThat(viewModel.isInventoryOpen()).isTrue();
        assertThat(result.snapshot().inventory()).isEmpty();
    }

    /** Bug #16: lo stato aperto è del ViewModel, quindi una rotazione non lo perde. */
    @Test
    void isInventoryOpen_isKeptByTheViewModelAcrossReads() {
        SessionViewModel viewModel = viewModel();
        viewModel.toggleInventory();

        TurnResult again = viewModel.current();

        assertThat(viewModel.isInventoryOpen()).isTrue();
        assertThat(again.snapshot().phase()).isEqualTo(Phase.PLAYING);
    }

    @Test
    void handle_whenThePlayerDies_closesTheInventory() {
        SessionViewModel viewModel = drawn("#####", "#g@g#", "##g##", "#####");
        viewModel.toggleInventory();

        // Contro il muro il player aspetta, e i tre goblin attaccano a ogni turno.
        for (int i = 0; i < 50 && viewModel.current().snapshot().phase() == Phase.PLAYING; i++) {
            viewModel.handle(new MoveCommand(Direction.UP));
        }

        assertThat(viewModel.current().snapshot().phase()).isEqualTo(Phase.GAME_OVER);
        assertThat(viewModel.isInventoryOpen()).isFalse();
    }

    @Test
    void toggleInventory_afterTheGameEnded_staysClosed() {
        SessionViewModel viewModel = drawn("#####", "#g@g#", "##g##", "#####");
        for (int i = 0; i < 50 && viewModel.current().snapshot().phase() == Phase.PLAYING; i++) {
            viewModel.handle(new MoveCommand(Direction.UP));
        }

        viewModel.toggleInventory();

        assertThat(viewModel.isInventoryOpen()).isFalse();
    }

    @Test
    void newGame_closesTheInventory() {
        SessionViewModel viewModel = viewModel();
        viewModel.toggleInventory();

        viewModel.newGame();

        assertThat(viewModel.isInventoryOpen()).isFalse();
    }

    /** Una nota salvata e riletta basta a rigiocare la partita fino allo stesso stato. */
    @Test
    void feedback_savedAndReplayed_reachesTheSameState(@TempDir Path directory) throws IOException {
        SessionViewModel viewModel = viewModel();
        for (Direction direction :
                List.of(Direction.UP, Direction.UP, Direction.RIGHT, Direction.DOWN)) {
            viewModel.handle(new MoveCommand(direction));
        }
        viewModel.toggleInventory();

        FeedbackEntry note = viewModel.feedback("prova", "2026-10-29T10:15:03", "1.0", "abc");
        File file = new FeedbackStore(directory.toFile()).save(note);
        FeedbackReplay.Result replay = FeedbackReplay.play(FeedbackStore.read(file));

        assertThat(note.seed()).isEqualTo(100);
        assertThat(note.commands())
                .containsExactly("move up", "move up", "move right", "move down");
        assertThat(note.inventoryOpen()).isTrue();
        assertThat(FeedbackReplay.differences(note, replay.last().snapshot())).isEmpty();
    }

    @Test
    void feedback_afterNewGame_hasOnlyTheNewGame() {
        SessionViewModel viewModel = viewModel();
        viewModel.handle(new MoveCommand(Direction.UP));

        viewModel.newGame();
        FeedbackEntry note = viewModel.feedback("prova", "t", "1.0", "abc");

        assertThat(note.seed()).isEqualTo(101);
        assertThat(note.commands()).isEmpty();
        assertThat(note.turn()).isEqualTo(1);
    }

    private SessionViewModel drawn(String... rows) {
        return new SessionViewModel(
                GameSessionFactory.create(GameConfig.standard(), new FixedFloorSource(rows)),
                seeds::getAndIncrement);
    }
}
