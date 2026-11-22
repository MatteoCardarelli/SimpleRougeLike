package io.github.matteocardarelli.simpleroguelike.ui.sound;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.event.Attacked;
import io.github.matteocardarelli.simpleroguelike.event.ChestOpened;
import io.github.matteocardarelli.simpleroguelike.event.CoinsPickedUp;
import io.github.matteocardarelli.simpleroguelike.event.Damaged;
import io.github.matteocardarelli.simpleroguelike.event.Died;
import io.github.matteocardarelli.simpleroguelike.event.DoorOpened;
import io.github.matteocardarelli.simpleroguelike.event.DoorUnlocked;
import io.github.matteocardarelli.simpleroguelike.event.EnemyFell;
import io.github.matteocardarelli.simpleroguelike.event.Fired;
import io.github.matteocardarelli.simpleroguelike.event.FloorChanged;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.event.ItemPickedUp;
import io.github.matteocardarelli.simpleroguelike.event.ItemUsed;
import io.github.matteocardarelli.simpleroguelike.event.JumpAnnounced;
import io.github.matteocardarelli.simpleroguelike.event.JumpCharged;
import io.github.matteocardarelli.simpleroguelike.event.Landed;
import io.github.matteocardarelli.simpleroguelike.event.LeverPulled;
import io.github.matteocardarelli.simpleroguelike.event.Missed;
import io.github.matteocardarelli.simpleroguelike.event.Moved;
import io.github.matteocardarelli.simpleroguelike.event.Poisoned;
import io.github.matteocardarelli.simpleroguelike.event.TrapTriggered;
import io.github.matteocardarelli.simpleroguelike.event.WareTaken;
import io.github.matteocardarelli.simpleroguelike.event.WaresPaid;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SoundMapTest {

    private static final EntityId PLAYER = new EntityId(1);
    private static final EntityId GOBLIN = new EntityId(2);
    private static final EntityId ITEM = new EntityId(9);
    private static final Position AT = new Position(3, 4);
    private static final Position NEXT = new Position(4, 4);
    private static final Kind PLAYER_KIND = Kind.of("player");
    private static final Kind GOBLIN_KIND = Kind.of("goblin");

    private final SoundMap map = new SoundMap();

    private Optional<Sound> sound(GameEvent event) {
        return map.soundFor(event, PLAYER);
    }

    @Test
    void moved_ofThePlayer_isAStep() {
        assertThat(sound(new Moved(PLAYER, PLAYER_KIND, AT, NEXT))).contains(Sound.STEP);
    }

    @Test
    void moved_ofAnEnemy_isSilent() {
        assertThat(sound(new Moved(GOBLIN, GOBLIN_KIND, AT, NEXT))).isEmpty();
    }

    @Test
    void attacked_byThePlayer_isAHit_butByAnEnemyIsSilent() {
        assertThat(sound(new Attacked(PLAYER, GOBLIN, AT, NEXT))).contains(Sound.HIT);
        assertThat(sound(new Attacked(GOBLIN, PLAYER, NEXT, AT))).isEmpty();
    }

    @Test
    void damaged_isHurtOnlyForThePlayer() {
        assertThat(sound(new Damaged(PLAYER, AT, 3, 7))).contains(Sound.HURT);
        assertThat(sound(new Damaged(GOBLIN, AT, 3, 7))).isEmpty();
    }

    @Test
    void died_isDefeatForThePlayer_andDeathForAnEnemy() {
        assertThat(sound(new Died(PLAYER, PLAYER_KIND, AT))).contains(Sound.DEFEAT);
        assertThat(sound(new Died(GOBLIN, GOBLIN_KIND, AT))).contains(Sound.DEATH);
    }

    @Test
    void doors_leversAndChests() {
        assertThat(sound(new DoorOpened(AT))).contains(Sound.DOOR);
        assertThat(sound(new DoorUnlocked(AT))).contains(Sound.DOOR);
        assertThat(sound(new LeverPulled(AT, List.of(NEXT)))).contains(Sound.LEVER);
        assertThat(sound(new ChestOpened(AT, Optional.empty()))).contains(Sound.CHEST);
    }

    @Test
    void coinsAndPickups() {
        assertThat(sound(new CoinsPickedUp(5, AT))).contains(Sound.COINS);
        assertThat(sound(new WaresPaid(5, AT))).contains(Sound.COINS);
        assertThat(sound(new ItemPickedUp(ITEM, Kind.of("sword"), AT))).contains(Sound.PICKUP);
        assertThat(sound(new WareTaken(ITEM, Kind.of("sword"), 4, AT))).contains(Sound.PICKUP);
    }

    @Test
    void itemUsed_isAPotionSoundOnlyForPotionsAndAntidotes() {
        for (String kind : List.of("potion", "greater_potion", "antidote")) {
            assertThat(sound(new ItemUsed(ITEM, Kind.of(kind), AT))).contains(Sound.POTION);
        }
        assertThat(sound(new ItemUsed(ITEM, Kind.of("key"), AT))).isEmpty();
    }

    @Test
    void fired_isAThrow() {
        assertThat(sound(new Fired(PLAYER, PLAYER_KIND, Kind.of("arrow"), AT, List.of(NEXT))))
                .contains(Sound.THROW);
    }

    @Test
    void slimeJump_andFall() {
        assertThat(sound(new JumpCharged(GOBLIN, GOBLIN_KIND, List.of(AT)))).contains(Sound.CHARGE);
        assertThat(sound(new JumpAnnounced(GOBLIN, List.of(AT)))).contains(Sound.JUMP);
        assertThat(sound(new Landed(GOBLIN, GOBLIN_KIND, AT, NEXT))).contains(Sound.LAND);
        assertThat(sound(new EnemyFell(GOBLIN, GOBLIN_KIND, AT))).contains(Sound.FALL);
    }

    @Test
    void floorTrapAndPoison() {
        assertThat(sound(new FloorChanged(2))).contains(Sound.STAIRS);
        assertThat(sound(new TrapTriggered(Kind.of("trap_spikes"), AT, 3))).contains(Sound.TRAP);
        assertThat(sound(new Poisoned(PLAYER, AT, 4))).contains(Sound.POISON);
    }

    @Test
    void otherEvents_areSilent() {
        assertThat(sound(new Missed(PLAYER, GOBLIN, AT, NEXT))).isEmpty();
    }
}
