package io.github.matteocardarelli.simpleroguelike.ui.render;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.matteocardarelli.simpleroguelike.model.Position;
import org.junit.jupiter.api.Test;

class CameraTest {

    private final Camera camera = new Camera(10);

    @Test
    void centerOn_putsTheTargetCellInTheMiddleOfTheView() {
        Viewport viewport = camera.centerOn(new Position(20, 30), 100, 60, 50, 50);

        // Il centro della cella (205, 305) cade al centro della vista (50, 30).
        assertThat(viewport.offsetX()).isEqualTo(155f);
        assertThat(viewport.offsetY()).isEqualTo(275f);
        assertThat(viewport.firstX()).isEqualTo(15);
        assertThat(viewport.lastX()).isEqualTo(25);
        assertThat(viewport.firstY()).isEqualTo(27);
        assertThat(viewport.lastY()).isEqualTo(33);
    }

    @Test
    void centerOn_nearTheBorders_drawsOnlyCellsOfTheGrid() {
        Viewport viewport = camera.centerOn(new Position(0, 49), 100, 100, 50, 50);

        assertThat(viewport.firstX()).isZero();
        assertThat(viewport.lastY()).isEqualTo(49);
    }

    @Test
    void constructor_withoutACellSize_throws() {
        assertThatThrownBy(() -> new Camera(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
