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
import io.github.matteocardarelli.simpleroguelike.command.DropItemCommand;
import io.github.matteocardarelli.simpleroguelike.command.TakeWareCommand;
import io.github.matteocardarelli.simpleroguelike.databinding.ActivityGameBinding;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.settings.Settings;
import io.github.matteocardarelli.simpleroguelike.snapshot.ActorView;
import io.github.matteocardarelli.simpleroguelike.snapshot.GameSnapshot;
import io.github.matteocardarelli.simpleroguelike.snapshot.Phase;
import io.github.matteocardarelli.simpleroguelike.snapshot.WareView;
import io.github.matteocardarelli.simpleroguelike.ui.content.ContentPresentation;
import io.github.matteocardarelli.simpleroguelike.ui.feedback.FeedbackDialog;
import io.github.matteocardarelli.simpleroguelike.ui.feedback.FeedbackHost;
import io.github.matteocardarelli.simpleroguelike.ui.hud.PlayerBars;
import io.github.matteocardarelli.simpleroguelike.ui.input.SwipeInputMapper;
import io.github.matteocardarelli.simpleroguelike.ui.inventory.InventoryContent;
import io.github.matteocardarelli.simpleroguelike.ui.inventory.InventoryHost;
import io.github.matteocardarelli.simpleroguelike.ui.inventory.InventorySheet;
import io.github.matteocardarelli.simpleroguelike.ui.log.EventMessageFormatter;
import io.github.matteocardarelli.simpleroguelike.ui.playback.EventPlayer;
import io.github.matteocardarelli.simpleroguelike.ui.playback.GatedPlayerInput;
import io.github.matteocardarelli.simpleroguelike.ui.playback.Scene;
import io.github.matteocardarelli.simpleroguelike.ui.playback.TurnPlayback;
import io.github.matteocardarelli.simpleroguelike.ui.render.Camera;
import io.github.matteocardarelli.simpleroguelike.ui.render.TilePalette;
import io.github.matteocardarelli.simpleroguelike.ui.settings.SettingsDialog;
import io.github.matteocardarelli.simpleroguelike.ui.settings.SettingsHost;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import org.jspecify.annotations.Nullable;

/**
 * L'Activity del gioco: trasforma i gesti in azioni e disegna ogni {@link GameUiState} che riceve.
 * Non decide niente della partita: dopo una rotazione la nuova Activity riceve lo stesso stato, lo
 * ridisegna senza ripetere le animazioni e lascia al sistema il compito di ricreare l'inventario.
 *
 * <p>Ogni azione di gioco, dalla mappa o dall'inventario, passa da {@link GatedPlayerInput}: mentre
 * un turno viene riprodotto resta in coda solo l'ultima. Impostazioni e note non sono azioni di
 * gioco: non passano da lì.
 */
@AndroidEntryPoint
public class GameActivity extends AppCompatActivity
        implements InventoryHost, FeedbackHost, SettingsHost {

    private static final String TAG = "GameActivity";

    @Inject ContentPresentation content;
    @Inject EventPlayer eventPlayer;
    @Inject Scene scene;
    @Inject EventMessageFormatter formatter;

    /** Quante righe mostra il registro: le ultime, le più recenti in fondo. */
    private static final int LOG_LINES = 4;

    private ActivityGameBinding binding;
    private GameViewModel viewModel;
    private TurnPlayback playback;
    private GatedPlayerInput input;
    private GestureDetector gestures;
    private @Nullable Snackbar lostGameNotice;
    private @Nullable Snackbar wareNotice;
    private @Nullable EntityId wareShown;

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
        binding.settingsButton.setOnClickListener(v -> showSettingsDialog());
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

    /**
     * Disegna lo stato: riproduzione del turno, barre, registro, fine partita, inventario, avvisi.
     */
    private void render(GameUiState state) {
        GameSnapshot snapshot = state.result().snapshot();
        playback.show(state.result(), viewModel.claimPlayback(state.result().revision()));
        binding.gameView.show(snapshot, scene);

        PlayerBars.showOnMap(binding.bars, snapshot.player(), snapshot.depth(), snapshot.floors());

        showLog(state);
        binding.aimHint.setVisibility(state.aimingItem().isPresent() ? View.VISIBLE : View.GONE);
        showBoss(snapshot);
        // Il seed sta nelle impostazioni, in tutte le build: qui resta solo il turno.
        boolean debugInfo = getResources().getBoolean(R.bool.show_debug_info);
        if (debugInfo) {
            binding.debugInfo.setVisibility(View.VISIBLE);
            binding.debugInfo.setText(getString(R.string.debug_turn, snapshot.turn()));
        }
        // Senza registro e senza informazioni di debug il pannello in basso non resta vuoto.
        binding.bottomPanel.setVisibility(
                state.logVisible() || debugInfo ? View.VISIBLE : View.GONE);

        switch (snapshot.phase()) {
            case PLAYING -> binding.endPanel.setVisibility(View.GONE);
            case GAME_OVER -> showEndPanel(R.string.end_game_over, snapshot);
            case VICTORY -> showEndPanel(R.string.end_victory, snapshot);
        }

        syncInventorySheet(state);
        showLostGameNotice(state.previousGameLost());
        showWare(snapshot);
    }

    /** Nome e salute del boss, quando è visibile. */
    private void showBoss(GameSnapshot snapshot) {
        for (ActorView enemy : snapshot.visibleEnemies()) {
            if (enemy.boss()) {
                binding.bossStatus.setVisibility(View.VISIBLE);
                binding.bossStatus.setText(
                        getString(
                                R.string.boss_status,
                                getString(content.nameRes(enemy.kind())),
                                enemy.health(),
                                enemy.maxHealth()));
                return;
            }
        }
        binding.bossStatus.setVisibility(View.GONE);
    }

    /** La fine della partita: il risultato, fin dove si è arrivati, e "Nuova partita". */
    private void showEndPanel(@StringRes int message, GameSnapshot snapshot) {
        binding.endMessage.setText(message);
        binding.endStats.setText(
                getString(R.string.end_stats, snapshot.turn(), snapshot.player().level()));
        binding.endPanel.setVisibility(View.VISIBLE);
    }

    /**
     * Le ultime righe del registro, se il giocatore non l'ha nascosto. È una regione live: TalkBack
     * legge i messaggi nuovi mentre si gioca.
     */
    private void showLog(GameUiState state) {
        binding.log.setVisibility(state.logVisible() ? View.VISIBLE : View.GONE);
        EntityId player = state.result().snapshot().player().id();
        List<String> lines = new ArrayList<>();
        for (GameEvent event : state.log()) {
            formatter.format(event, player).ifPresent(lines::add);
        }
        List<String> last = lines.subList(Math.max(0, lines.size() - LOG_LINES), lines.size());
        String text = String.join("\n", last);
        if (!text.contentEquals(binding.log.getText())) {
            binding.log.setText(text);
        }
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
        GameSnapshot snapshot = state.result().snapshot();
        return new InventoryContent(snapshot.inventory(), state.selectedItem(), snapshot.player());
    }

    /**
     * La merce del negozio su cui sta il player: nome e prezzo, e "Prendi". L'avviso sparisce
     * quando il player si sposta o prende la merce.
     */
    private void showWare(GameSnapshot snapshot) {
        Optional<WareView> ware = snapshot.ware();
        EntityId shown = wareShown;
        if (ware.isPresent() && ware.get().item().equals(shown)) {
            return;
        }
        Snackbar notice = wareNotice;
        if (notice != null) {
            notice.dismiss();
            wareNotice = null;
            wareShown = null;
        }
        if (ware.isPresent()) {
            WareView offer = ware.get();
            Snackbar created =
                    Snackbar.make(
                            binding.getRoot(),
                            getString(
                                    R.string.ware_offer,
                                    getString(content.nameRes(offer.kind())),
                                    offer.price()),
                            Snackbar.LENGTH_INDEFINITE);
            created.setAction(
                    R.string.ware_take, v -> input.submit(new TakeWareCommand(offer.item())));
            created.show();
            wareNotice = created;
            wareShown = offer.item();
        }
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
    public void onDropSelected() {
        viewModel.state().selectedItem().ifPresent(item -> input.submit(new DropItemCommand(item)));
    }

    @Override
    public void onInventoryToggled() {
        input.toggleInventory();
    }

    @Override
    public void onInventoryCancelled() {
        viewModel.closeInventory();
    }

    /** Apre le impostazioni, se non sono già aperte: non consuma il turno. */
    private void showSettingsDialog() {
        if (getSupportFragmentManager().findFragmentByTag(SettingsDialog.TAG) == null) {
            new SettingsDialog().show(getSupportFragmentManager(), SettingsDialog.TAG);
        }
    }

    @Override
    public Settings currentSettings() {
        return viewModel.settings();
    }

    @Override
    public long currentSeed() {
        return viewModel.state().result().snapshot().seed();
    }

    @Override
    public boolean gameInProgress() {
        return viewModel.state().result().snapshot().phase() == Phase.PLAYING;
    }

    @Override
    public void onLogVisibilityChanged(boolean visible) {
        viewModel.setLogVisible(visible);
    }

    @Override
    public void onNewGameRequested() {
        viewModel.newGame();
    }

    @Override
    public void onWriteNoteRequested() {
        showFeedbackDialog();
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
