package io.github.matteocardarelli.simpleroguelike.ui.input;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.model.Direction;
import org.junit.jupiter.api.Test;

class SwipeInputMapperTest {

    private static final float DISTANCE = 100;
    private static final float FAST = 1000;

    @Test
    void direction_followsTheLongerMovement() {
        assertThat(SwipeInputMapper.direction(150, 20, FAST, FAST, DISTANCE))
                .hasValue(Direction.RIGHT);
        assertThat(SwipeInputMapper.direction(-150, 20, FAST, FAST, DISTANCE))
                .hasValue(Direction.LEFT);
        assertThat(SwipeInputMapper.direction(20, 150, FAST, FAST, DISTANCE))
                .hasValue(Direction.DOWN);
        assertThat(SwipeInputMapper.direction(20, -150, FAST, FAST, DISTANCE))
                .hasValue(Direction.UP);
    }

    @Test
    void direction_ofATooShortOrTooSlowGesture_isEmpty() {
        assertThat(SwipeInputMapper.direction(50, 0, FAST, FAST, DISTANCE)).isEmpty();
        assertThat(SwipeInputMapper.direction(150, 0, 10, 10, DISTANCE)).isEmpty();
    }
}
