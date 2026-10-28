package io.github.matteocardarelli.simpleroguelike.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class KeysTest {

    @ParameterizedTest
    @ValueSource(strings = {"potion", "goblin", "chase_or_wander", "a1_b2"})
    void keys_snakeCase_areAccepted(String value) {
        assertThat(Kind.of(value).value()).isEqualTo(value);
        assertThat(BehaviorKey.of(value).value()).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"", "Potion", "chase-or-wander", "_potion", "potion_", "1potion", "a b"})
    void keys_notSnakeCase_areRejected(String value) {
        assertThatThrownBy(() -> Kind.of(value)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BehaviorKey.of(value))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void behaviorKey_compareTo_ordersByValue() {
        assertThat(BehaviorKey.of("a").compareTo(BehaviorKey.of("b"))).isNegative();
        assertThat(BehaviorKey.of("b").compareTo(BehaviorKey.of("a"))).isPositive();
        assertThat(BehaviorKey.of("a").compareTo(BehaviorKey.of("a"))).isZero();
    }
}
