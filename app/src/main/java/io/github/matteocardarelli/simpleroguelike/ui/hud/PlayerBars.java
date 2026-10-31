package io.github.matteocardarelli.simpleroguelike.ui.hud;

import android.content.Context;
import io.github.matteocardarelli.simpleroguelike.R;
import io.github.matteocardarelli.simpleroguelike.databinding.ViewPlayerBarsBinding;
import io.github.matteocardarelli.simpleroguelike.snapshot.PlayerView;

/**
 * Riempie le barre di salute ed esperienza di {@code view_player_bars}: un solo posto per il
 * formato dei valori, sulla mappa come nell'inventario.
 */
public final class PlayerBars {

    private PlayerBars() {}

    /** Mostra salute ed esperienza del player, con i valori e le descrizioni per TalkBack. */
    public static void show(ViewPlayerBarsBinding bars, PlayerView player) {
        Context context = bars.getRoot().getContext();
        bars.healthBar.setMax(player.maxHealth());
        bars.healthBar.setProgress(player.health());
        bars.xpBar.setMax(player.experienceToNextLevel());
        bars.xpBar.setProgress(player.experience());
        bars.healthValue.setText(
                context.getString(R.string.hud_health_value, player.health(), player.maxHealth()));
        bars.xpValue.setText(
                context.getString(
                        R.string.hud_experience_value,
                        player.experience(),
                        player.experienceToNextLevel(),
                        player.level()));
        // TalkBack legge il nome della barra e il suo valore, non una percentuale.
        bars.healthBar.setStateDescription(
                context.getString(R.string.a11y_health_value, player.health(), player.maxHealth()));
        bars.xpBar.setStateDescription(
                context.getString(
                        R.string.a11y_experience_value,
                        player.experience(),
                        player.experienceToNextLevel(),
                        player.level()));
    }
}
