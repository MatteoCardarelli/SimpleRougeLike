package io.github.matteocardarelli.simpleroguelike.ui.sound;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.event.Moved;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SoundEffectsTest {

    private static final EntityId PLAYER = new EntityId(1);
    private static final Moved PLAYER_STEP =
            new Moved(PLAYER, Kind.of("player"), new Position(1, 1), new Position(2, 1));

    private final List<Sound> played = new ArrayList<>();
    private final SoundEffects effects = new SoundEffects(new SoundMap(), played::add);

    @Test
    void onEventPlayed_playsTheSoundOfTheEvent() {
        effects.setPlayer(PLAYER);

        effects.onEventPlayed(PLAYER_STEP);

        assertThat(played).containsExactly(Sound.STEP);
    }

    @Test
    void onEventPlayed_whenSoundIsOff_playsNothing() {
        effects.setPlayer(PLAYER);
        effects.setEnabled(false);

        effects.onEventPlayed(PLAYER_STEP);
        effects.play(Sound.VICTORY);

        assertThat(played).isEmpty();
    }

    @Test
    void onEventPlayed_turnedBackOn_playsAgain() {
        effects.setPlayer(PLAYER);
        effects.setEnabled(false);
        effects.setEnabled(true);

        effects.onEventPlayed(PLAYER_STEP);

        assertThat(played).containsExactly(Sound.STEP);
    }

    @Test
    void onEventPlayed_beforeKnowingThePlayer_playsNothing() {
        effects.onEventPlayed(PLAYER_STEP);

        assertThat(played).isEmpty();
    }

    @Test
    void play_playsAnEffectThatIsNotAnEvent() {
        effects.play(Sound.VICTORY);

        assertThat(played).containsExactly(Sound.VICTORY);
    }
}
