package io.github.matteocardarelli.simpleroguelike.ui.render;

import androidx.annotation.DrawableRes;
import io.github.matteocardarelli.simpleroguelike.R;

/** Quale immagine mostra un oggetto fisso, per tipo e per stato (aperto o tirato). */
final class FixtureSprites {

    private FixtureSprites() {}

    /**
     * @return l'immagine, o 0 se il tipo non ne ha
     */
    @DrawableRes
    static int sprite(String kind, boolean open) {
        return switch (kind) {
            case "door" -> open ? R.drawable.fixture_door_open : R.drawable.fixture_door;
            case "locked_door" ->
                    open ? R.drawable.fixture_door_open : R.drawable.fixture_locked_door;
            case "boss_door" ->
                    open ? R.drawable.fixture_boss_door_open : R.drawable.fixture_boss_door;
            case "gate" -> open ? R.drawable.fixture_gate_open : R.drawable.fixture_gate;
            case "lever" -> open ? R.drawable.fixture_lever_open : R.drawable.fixture_lever;
            case "chest" -> open ? R.drawable.fixture_chest_open : R.drawable.fixture_chest;
            case "stairs" -> R.drawable.fixture_stairs;
            // Una trappola scattata resta com'è: si vede dove ha colpito.
            case "trap_spikes" -> R.drawable.trap_spikes;
            case "trap_dart" -> R.drawable.trap_dart;
            default -> 0;
        };
    }
}
