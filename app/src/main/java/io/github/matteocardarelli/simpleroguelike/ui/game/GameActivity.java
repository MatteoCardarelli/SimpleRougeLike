package io.github.matteocardarelli.simpleroguelike.ui.game;

import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.matteocardarelli.simpleroguelike.R;
import io.github.matteocardarelli.simpleroguelike.command.MoveCommand;
import io.github.matteocardarelli.simpleroguelike.databinding.ActivityGameBinding;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.snapshot.GameSnapshot;
import io.github.matteocardarelli.simpleroguelike.snapshot.InventoryEntry;
import io.github.matteocardarelli.simpleroguelike.snapshot.Phase;
import io.github.matteocardarelli.simpleroguelike.snapshot.PlayerView;
import io.github.matteocardarelli.simpleroguelike.ui.content.ContentPresentation;
import io.github.matteocardarelli.simpleroguelike.ui.feedback.FeedbackDialog;
import io.github.matteocardarelli.simpleroguelike.ui.feedback.FeedbackHost;
import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import org.jspecify.annotations.Nullable;

/**
 * L'Activity del gioco: trasforma i gesti in azioni per il {@link GameViewModel} e disegna ogni
 * {@link GameUiState} che riceve. Non decide niente della partita: dopo una rotazione la nuova
 * Activity riceve lo stesso stato e lo ridisegna.
 */
@AndroidEntryPoint
public class GameActivity extends AppCompatActivity implements FeedbackHost {

    private static final String TAG = "GameActivity";
    private static final int SWIPE_THRESHOLD_PX = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    @Inject ContentPresentation content;

    private ActivityGameBinding binding;
    private GameViewModel viewModel;
    private GestureDetector gestures;
    private OnBackPressedCallback closeInventoryOnBack;
    private @Nullable Snackbar lostGameNotice;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(GameViewModel.class);
        gestures = new GestureDetector(this, new Gestures());

        // Le note di collaudo si scrivono solo nelle build di debug.
        if (getResources().getBoolean(R.bool.feedback_enabled)) {
            binding.feedbackButton.setVisibility(View.VISIBLE);
            binding.feedbackButton.setOnClickListener(v -> showFeedbackDialog());
        }

        // Indietro chiude l'inventario solo se è aperto: con l'inventario chiuso il callback è
        // spento, e indietro fa quello che fa sempre.
        closeInventoryOnBack =
                new OnBackPressedCallback(false) {
                    @Override
                    public void handleOnBackPressed() {
                        viewModel.closeInventory();
                    }
                };
        getOnBackPressedDispatcher().addCallback(this, closeInventoryOnBack);

        viewModel.uiState().observe(this, this::render);
    }

    /**
     * Passa ogni tocco al gestore dei gesti prima che arrivi alle viste: con l'inventario aperto la
     * lista scorrevole consumerebbe i tocchi, e il doppio tap non arriverebbe mai.
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        boolean gesture = gestures.onTouchEvent(event);
        return super.dispatchTouchEvent(event) || gesture;
    }

    private final class Gestures extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onFling(
                @Nullable MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1 == null) {
                return false;
            }
            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();
            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD_PX
                        && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    move(diffX > 0 ? Direction.RIGHT : Direction.LEFT);
                    return true;
                }
            } else if (Math.abs(diffY) > SWIPE_THRESHOLD_PX
                    && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                move(diffY > 0 ? Direction.DOWN : Direction.UP);
                return true;
            }
            return false;
        }

        /**
         * A partita finita il doppio tap ne inizia una nuova; durante la partita apre o chiude
         * l'inventario, anche sopra la lista.
         */
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (viewModel.state().result().snapshot().phase() != Phase.PLAYING) {
                viewModel.newGame();
            } else {
                viewModel.toggleInventory();
            }
            return true;
        }

        /**
         * Un oggetto si usa con un tocco singolo confermato, che arriva solo quando il tempo del
         * doppio tap è scaduto: un doppio tap sopra una pozione chiude l'inventario senza usarla.
         */
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (!viewModel.state().inventoryOpen()) {
                return false;
            }
            EntityId item = itemAt(e.getRawX(), e.getRawY());
            if (item == null) {
                return false;
            }
            viewModel.chooseItem(item);
            return true;
        }
    }

    /** Con l'inventario aperto la partita è in pausa: gli swipe non fanno niente. */
    private void move(Direction direction) {
        if (!viewModel.state().inventoryOpen()) {
            viewModel.submit(new MoveCommand(direction));
        }
    }

    /** L'oggetto della lista sotto il punto toccato, in coordinate dello schermo. */
    private @Nullable EntityId itemAt(float screenX, float screenY) {
        int[] origin = new int[2];
        for (int i = 0; i < binding.inventoryItemList.getChildCount(); i++) {
            View child = binding.inventoryItemList.getChildAt(i);
            if (child.getTag() instanceof EntityId id) {
                child.getLocationOnScreen(origin);
                if (screenX >= origin[0]
                        && screenX < origin[0] + child.getWidth()
                        && screenY >= origin[1]
                        && screenY < origin[1] + child.getHeight()) {
                    return id;
                }
            }
        }
        return null;
    }

    /** Disegna lo stato: barre, messaggio di fine partita, inventario, mappa, avvisi. */
    private void render(GameUiState state) {
        GameSnapshot snapshot = state.result().snapshot();
        PlayerView player = snapshot.player();

        binding.healthBar.setMax(player.maxHealth());
        binding.healthBar.setProgress(player.health());
        binding.xpBar.setMax(player.experienceToNextLevel());
        binding.xpBar.setProgress(player.experience());

        // A partita finita resta solo il messaggio, finché un doppio tap non ne inizia un'altra.
        switch (snapshot.phase()) {
            case PLAYING -> binding.endMessage.setVisibility(View.GONE);
            case GAME_OVER -> showEndMessage(R.string.end_game_over);
            case VICTORY -> showEndMessage(R.string.end_victory);
        }

        showInventory(snapshot, state.inventoryOpen());
        closeInventoryOnBack.setEnabled(state.inventoryOpen());
        showLostGameNotice(state.previousGameLost());
        binding.gameView.show(snapshot);
    }

    private void showEndMessage(@StringRes int message) {
        binding.endMessage.setText(message);
        binding.endMessage.setVisibility(View.VISIBLE);
    }

    /** Mostra l'inventario con gli oggetti dello snapshot, oppure lo nasconde. */
    private void showInventory(GameSnapshot snapshot, boolean open) {
        if (!open) {
            binding.inventoryLayout.setVisibility(View.GONE);
            return;
        }
        binding.inventoryItemList.removeAllViews();
        List<InventoryEntry> inventory = snapshot.inventory();
        if (inventory.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.inventory_empty);
            empty.setTextColor(getColor(android.R.color.white));
            binding.inventoryItemList.addView(empty);
        } else {
            // Il tocco lo riconosce il gestore dei gesti dall'id nel tag: un OnClickListener
            // scatterebbe anche al primo tocco di un doppio tap.
            for (InventoryEntry entry : inventory) {
                TextView item = new TextView(this);
                item.setText(content.nameRes(entry.kind()));
                item.setTextColor(getColor(android.R.color.white));
                item.setTextSize(20);
                item.setPadding(0, 8, 0, 8);
                item.setBackgroundResource(android.R.drawable.btn_default);
                item.setTag(entry.id());
                binding.inventoryItemList.addView(item);
            }
        }
        binding.inventoryLayout.setVisibility(View.VISIBLE);
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
