package io.github.matteocardarelli.simpleroguelike.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FactionTest {

    @Test
    void faction_isHostileTo_onlyOtherFactions() {
        assertThat(Faction.PLAYER.isHostileTo(Faction.MONSTERS)).isTrue();
        assertThat(Faction.MONSTERS.isHostileTo(Faction.PLAYER)).isTrue();
        assertThat(Faction.MONSTERS.isHostileTo(Faction.MONSTERS)).isFalse();
    }
}
