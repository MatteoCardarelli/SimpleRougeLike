package io.github.matteocardarelli.simpleroguelike.ui.hud;

import android.content.Context;
import android.view.View;
import io.github.matteocardarelli.simpleroguelike.R;
import io.github.matteocardarelli.simpleroguelike.databinding.ViewPlayerBarsBinding;
import io.github.matteocardarelli.simpleroguelike.snapshot.PlayerView;

/**
 * Riempie le barre di salute ed esperienza di {@code view_player_bars}: un solo posto per il
 * formato dei valori, sulla mappa come nell'inventario. Sulla mappa accanto alle barre c'è solo il
 * livello; i valori si leggono nell'inventario, dove si sceglie una pozione. TalkBack legge i
 * valori in tutti e due i posti.
 */
public final class PlayerBars {

    private PlayerBars() {}

    /** Sulla mappa: le barre e il livello, senza numeri. */
    public static void showOnMap(ViewPlayerBarsBinding bars, PlayerView player) {
        Context context = bars.getRoot().getContext();
        fill(bars, player);
        bars.healthValue.setVisibility(View.GONE);
        bars.statsValue.setVisibility(View.GONE);
        bars.xpValue.setText(context.getString(R.string.hud_level, player.level()));
    }

    /**
     * Nell'inventario: le barre con salute ed esperienza scritte accanto, il livello e le
     * statistiche di combattimento.
     */
    public static void showWithValues(ViewPlayerBarsBinding bars, PlayerView player) {
        Context context = bars.getRoot().getContext();
        fill(bars, player);
        bars.healthValue.setVisibility(View.VISIBLE);
        bars.healthValue.setText(
                context.getString(R.string.hud_health_value, player.health(), player.maxHealth()));
        bars.xpValue.setText(
                context.getString(
                        R.string.hud_experience_value,
                        player.experience(),
                        player.experienceToNextLevel(),
                        player.level()));
        bars.statsValue.setVisibility(View.VISIBLE);
        bars.statsValue.setText(
                context.getString(
                        R.string.hud_stats,
                        player.attack(),
                        player.defense(),
                        player.accuracy(),
                        player.evasion()));
    }

    /** Le barre e le loro descrizioni per TalkBack, uguali nei due posti. */
    private static void fill(ViewPlayerBarsBinding bars, PlayerView player) {
        Context context = bars.getRoot().getContext();
        bars.healthBar.setMax(player.maxHealth());
        bars.healthBar.setProgress(player.health());
        bars.xpBar.setMax(player.experienceToNextLevel());
        bars.xpBar.setProgress(player.experience());
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
