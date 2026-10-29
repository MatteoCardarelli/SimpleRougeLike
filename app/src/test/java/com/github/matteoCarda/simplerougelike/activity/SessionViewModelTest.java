package com.github.matteoCarda.simplerougelike.activity;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.command.MoveCommand;
import io.github.matteocardarelli.simpleroguelike.di.GameConfig;
import io.github.matteocardarelli.simpleroguelike.di.GameSessionFactory;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.session.TurnResult;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

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
}
