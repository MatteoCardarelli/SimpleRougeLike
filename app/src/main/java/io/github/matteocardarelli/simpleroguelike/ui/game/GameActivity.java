package io.github.matteocardarelli.simpleroguelike.ui.game;

import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.matteocardarelli.simpleroguelike.R;
import io.github.matteocardarelli.simpleroguelike.databinding.ActivityGameBinding;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.snapshot.GameSnapshot;
import io.github.matteocardarelli.simpleroguelike.snapshot.PlayerView;
import io.github.matteocardarelli.simpleroguelike.ui.content.ContentPresentation;
import io.github.matteocardarelli.simpleroguelike.ui.feedback.FeedbackDialog;
import io.github.matteocardarelli.simpleroguelike.ui.feedback.FeedbackHost;
import io.github.matteocardarelli.simpleroguelike.ui.input.SwipeInputMapper;
import io.github.matteocardarelli.simpleroguelike.ui.inventory.InventoryContent;
import io.github.matteocardarelli.simpleroguelike.ui.inventory.InventoryHost;
import io.github.matteocardarelli.simpleroguelike.ui.inventory.InventorySheet;
import io.github.matteocardarelli.simpleroguelike.ui.playback.EventPlayer;
import io.github.matteocardarelli.simpleroguelike.ui.playback.GatedPlayerInput;
import io.github.matteocardarelli.simpleroguelike.ui.playback.Scene;
import io.github.matteocardarelli.simpleroguelike.ui.playback.TurnPlayback;
import io.github.matteocardarelli.simpleroguelike.ui.render.Camera;
import io.github.matteocardarelli.simpleroguelike.ui.render.TilePalette;
import java.io.IOException;
import javax.inject.Inject;
import org.jspecify.annotations.Nullable;

/**
 * L'Activity del gioco: trasforma i gesti in azioni e disegna ogni {@link GameUiState} che riceve.
 * Non decide niente della partita: dopo una rotazione la nuova Activity riceve lo stesso stato, lo
 * ridisegna senza ripetere le animazioni e lascia al sistema il compito di ricreare l'inventario.
 *
 * <p>Ogni azione di gioco, dalla mappa o dall'inventario, passa da {@link GatedPlayerInput}: mentre
 * un turno viene riprodotto resta in coda solo l'ultima.
 */
@AndroidEntryPoint
public class GameActivity extends AppCompatActivity implements InventoryHost, FeedbackHost {

    private static final String TAG = "GameActivity";

    @Inject ContentPresentation content;
    @Inject EventPlayer eventPlayer;
    @Inject Scene scene;

    private ActivityGameBinding binding;
    private GameViewModel viewModel;
    private TurnPlayback playback;
    private GatedPlayerInput input;
    private GestureDetector gestures;
    private @Nullable Snackbar lostGameNotice;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(GameViewModel.class);

        playback = new TurnPlayback(eventPlayer, scene);
        input = new GatedPlayerInput(viewModel, playback);
        playback.setOnIdle(input::flush);

        binding.gameView.setUp(
                new Camera(getResources().getDimension(R.dimen.cell_size)),
                TilePalette.from(this),
                content);
        gestures =
                new GestureDetector(
                        this,
                        new SwipeInputMapper(
                                input, getResources().getDimension(R.dimen.swipe_min_distance)));
        binding.newGameButton.setOnClickListener(v -> viewModel.newGame());
        if (getResources().getBoolean(R.bool.feedback_enabled)) {
            binding.feedbackButton.setVisibility(View.VISIBLE);
            binding.feedbackButton.setOnClickListener(v -> showFeedbackDialog());
        }
        keepTextAwayFromSystemBars();

        viewModel.uiState().observe(this, this::render);
    }

    /** I tocchi che nessuna vista ha consumato sono gesti sulla mappa. */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestures.onTouchEvent(event) || super.onTouchEvent(event);
    }

    /**
     * L'app è disegnata anche sotto le barre di sistema: la mappa ci sta, testo e pulsanti no. Gli
     * insets di barre e ritaglio dello schermo diventano il margine interno del livello sopra la
     * mappa.
     */
    private void keepTextAwayFromSystemBars() {
        ViewCompat.setOnApplyWindowInsetsListener(
                binding.hud,
                (view, windowInsets) -> {
                    Insets insets =
                            windowInsets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                                            | WindowInsetsCompat.Type.displayCutout());
                    view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
                    return windowInsets;
                });
    }

    /** Disegna lo stato: riproduzione del turno, barre, fine partita, inventario, avvisi. */
    private void render(GameUiState state) {
        GameSnapshot snapshot = state.result().snapshot();
        playback.show(state.result(), viewModel.claimPlayback(state.result().revision()));
        binding.gameView.show(snapshot, scene);

        PlayerView player = snapshot.player();
        binding.healthBar.setMax(player.maxHealth());
        binding.healthBar.setProgress(player.health());
        binding.xpBar.setMax(player.experienceToNextLevel());
        binding.xpBar.setProgress(player.experience());

        switch (snapshot.phase()) {
            case PLAYING -> binding.endPanel.setVisibility(View.GONE);
            case GAME_OVER -> showEndPanel(R.string.end_game_over);
            case VICTORY -> showEndPanel(R.string.end_victory);
        }

        syncInventorySheet(state);
        showLostGameNotice(state.previousGameLost());
    }

    private void showEndPanel(@StringRes int message) {
        binding.endMessage.setText(message);
        binding.endPanel.setVisibility(View.VISIBLE);
    }

    /**
     * Lo sheet segue lo stato, non il contrario: se deve essere aperto e non c'è lo apre, se c'è
     * gli passa il contenuto, se deve essere chiuso e c'è lo chiude. Dopo una rotazione il sistema
     * ha già ricreato lo sheet, e il controllo evita il duplicato.
     */
    private void syncInventorySheet(GameUiState state) {
        FragmentManager fragments = getSupportFragmentManager();
        InventorySheet sheet = (InventorySheet) fragments.findFragmentByTag(InventorySheet.TAG);
        if (state.inventoryOpen()) {
            if (sheet == null) {
                new InventorySheet().show(fragments, InventorySheet.TAG);
            } else {
                sheet.show(inventory(state));
            }
        } else if (sheet != null) {
            sheet.dismiss();
        }
    }

    private static InventoryContent inventory(GameUiState state) {
        return new InventoryContent(state.result().snapshot().inventory(), state.selectedItem());
    }

    /** L'avviso resta finché il giocatore non lo conferma, anche dopo una rotazione. */
    private void showLostGameNotice(boolean lost) {
        if (lost && lostGameNotice == null) {
            Snackbar notice =
                    Snackbar.make(
                            binding.getRoot(),
                            R.string.previous_game_lost,
                            Snackbar.LENGTH_INDEFINITE);
            notice.setAction(R.string.previous_game_lost_ok, v -> viewModel.acknowledgeLostGame());
            notice.show();
            lostGameNotice = notice;
        } else if (!lost && lostGameNotice != null) {
            lostGameNotice.dismiss();
            lostGameNotice = null;
        }
    }

    @Override
    public ContentPresentation content() {
        return content;
    }

    @Override
    public InventoryContent currentInventory() {
        return inventory(viewModel.state());
    }

    @Override
    public void onItemSelected(EntityId item) {
        viewModel.selectItem(item);
    }

    @Override
    public void onUseSelected() {
        viewModel.state().selectedItem().ifPresent(input::chooseItem);
    }

    @Override
    public void onInventoryToggled() {
        input.toggleInventory();
    }

    @Override
    public void onInventoryCancelled() {
        viewModel.closeInventory();
    }

    /** Apre la finestra della nota, se non è già aperta: non consuma il turno. */
    private void showFeedbackDialog() {
        if (getSupportFragmentManager().findFragmentByTag(FeedbackDialog.TAG) == null) {
            new FeedbackDialog().show(getSupportFragmentManager(), FeedbackDialog.TAG);
        }
    }

    /**
     * Salva la nota con la partita di adesso nella memoria privata dell'app, da dove si legge con
     * ./gradlew :app:pullFeedback.
     */
    @Override
    public void onFeedbackWritten(String text) {
        try {
            int message =
                    viewModel.saveFeedback(text)
                            ? R.string.feedback_saved
                            : R.string.feedback_empty;
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "Nota di collaudo non salvata", e);
            Toast.makeText(this, R.string.feedback_failed, Toast.LENGTH_LONG).show();
        }
    }
}
