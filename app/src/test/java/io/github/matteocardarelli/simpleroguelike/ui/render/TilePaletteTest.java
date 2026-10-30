package io.github.matteocardarelli.simpleroguelike.ui.render;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.snapshot.CellView;
import io.github.matteocardarelli.simpleroguelike.snapshot.Visibility;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class TilePaletteTest {

    /** Il contrasto minimo tra visibile ed esplorato: leggibile anche senza percepire i colori. */
    private static final double MINIMUM_CONTRAST = 1.5;

    private final TilePalette palette = new TilePalette(1, 2, 3, 4);

    @Test
    void cellColor_dependsOnVisibilityAndWall() {
        assertThat(palette.cellColor(new CellView(false, Visibility.VISIBLE))).hasValue(1);
        assertThat(palette.cellColor(new CellView(true, Visibility.VISIBLE))).hasValue(2);
        assertThat(palette.cellColor(new CellView(false, Visibility.EXPLORED))).hasValue(3);
        assertThat(palette.cellColor(new CellView(true, Visibility.EXPLORED))).hasValue(4);
        assertThat(palette.cellColor(new CellView(true, Visibility.UNEXPLORED))).isEmpty();
    }

    @Test
    void contrast_ofBlackAndWhite_isTheMaximum() {
        assertThat(TilePalette.contrast(0xFF000000, 0xFFFFFFFF)).isCloseTo(21, within());
        assertThat(TilePalette.contrast(0xFF777777, 0xFF777777)).isCloseTo(1, within());
    }

    /** I colori veri, letti dalle risorse dell'app: un colore cambiato male fa fallire il test. */
    @Test
    void colorsOfTheApp_distinguishVisibleFromExploredByLuminance() throws IOException {
        // I test di :app compilano contro le API di Android, che non hanno Files.readString.
        String colors =
                new String(
                        Files.readAllBytes(Path.of("src/main/res/values/colors.xml")),
                        StandardCharsets.UTF_8);
        TilePalette app =
                new TilePalette(
                        color(colors, "tile_visible_floor"),
                        color(colors, "tile_visible_wall"),
                        color(colors, "tile_explored_floor"),
                        color(colors, "tile_explored_wall"));

        assertThat(app.minimumVisibilityContrast()).isGreaterThanOrEqualTo(MINIMUM_CONTRAST);
    }

    private static int color(String resources, String name) {
        Matcher matcher =
                Pattern.compile("name=\"" + name + "\">#([0-9A-Fa-f]{8})<").matcher(resources);
        assertThat(matcher.find()).as("colore " + name).isTrue();
        return (int) Long.parseLong(matcher.group(1), 16);
    }

    private static org.assertj.core.data.Offset<Double> within() {
        return org.assertj.core.data.Offset.offset(0.01);
    }
}
