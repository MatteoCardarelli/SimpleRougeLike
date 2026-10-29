package com.github.matteoCarda.simplerougelike.activity;

import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.github.matteoCarda.simplerougelike.R;
import com.github.matteoCarda.simplerougelike.view.GameView;
import io.github.matteocardarelli.simpleroguelike.command.MoveCommand;
import io.github.matteocardarelli.simpleroguelike.command.UseItemCommand;
import io.github.matteocardarelli.simpleroguelike.content.StandardItemCatalog;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.session.TurnResult;
import io.github.matteocardarelli.simpleroguelike.snapshot.GameSnapshot;
import io.github.matteocardarelli.simpleroguelike.snapshot.InventoryEntry;
import io.github.matteocardarelli.simpleroguelike.snapshot.Phase;
import io.github.matteocardarelli.simpleroguelike.snapshot.PlayerView;
import java.util.List;

/**
 * Activity principale dell'app. Ospita la GameView, trasforma gli swipe in comandi per la sessione
 * di gioco e mostra il risultato di ogni turno. La partita sta nel {@link SessionViewModel}, che
 * sopravvive alla rotazione.
 */
public class MainActivity extends AppCompatActivity {

    private SessionViewModel viewModel;
    private GameView gameView;
    private GestureDetector gestureDetector;
    private ProgressBar healthBar;
    private ProgressBar xpBar;
    private TextView endMessage;
    private LinearLayout inventoryLayout;
    private LinearLayout inventoryItemList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // La partita è del ViewModel: dopo una rotazione si riprende quella in corso.
        viewModel = new ViewModelProvider(this).get(SessionViewModel.class);
        gameView = findViewById(R.id.gameView);

        // Trova le barre di stato nel layout
        healthBar = findViewById(R.id.healthBar);
        xpBar = findViewById(R.id.xpBar);
        endMessage = findViewById(R.id.end_message);
        inventoryLayout = findViewById(R.id.inventory_layout);
        inventoryItemList = findViewById(R.id.inventory_item_list);

        // NUOVO SISTEMA PER GESTIRE IL TASTO "INDIETRO"
        getOnBackPressedDispatcher()
                .addCallback(
                        this,
                        new OnBackPressedCallback(true) {
                            @Override
                            public void handleOnBackPressed() {
                                // Qui mettiamo la stessa logica di prima:
                                // controlliamo se l'inventario è visibile.

                                if (inventoryLayout.getVisibility() == View.VISIBLE) {
                                    // Se l'inventario è aperto, lo chiudiamo.
                                    closeInventory();
                                } else {
                                    // Se l'inventario è chiuso, eseguiamo l'azione di default
                                    // (che di solito è chiudere l'activity o tornare indietro).
                                    // Per farlo in modo sicuro, disabilitiamo temporaneamente
                                    // questo callback
                                    // e chiamiamo di nuovo il dispatcher.
                                    setEnabled(false); // Disabilita questo callback
                                    getOnBackPressedDispatcher()
                                            .onBackPressed(); // Esegui l'azione standard
                                }
                            }
                        });

        // Imposta il listener per gli swipe.
        setupGestureDetector();
        render(viewModel.current());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Passa l'evento al gestore di swipe.
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    /** Configura il GestureDetector per tradurre gli swipe in comandi di gioco. */
    private void setupGestureDetector() {
        gestureDetector =
                new GestureDetector(
                        this,
                        new GestureDetector.SimpleOnGestureListener() {
                            private static final int SWIPE_THRESHOLD =
                                    100; // Minima distanza (pixel)
                            private static final int SWIPE_VELOCITY_THRESHOLD =
                                    100; // Minima velocità

                            @Override
                            public boolean onFling(
                                    MotionEvent e1,
                                    MotionEvent e2,
                                    float velocityX,
                                    float velocityY) {
                                float diffX = e2.getX() - e1.getX();
                                float diffY = e2.getY() - e1.getY();

                                if (Math.abs(diffX) > Math.abs(diffY)) {
                                    // Swipe orizzontale
                                    if (Math.abs(diffX) > SWIPE_THRESHOLD
                                            && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                                        move(diffX > 0 ? Direction.RIGHT : Direction.LEFT);
                                        return true;
                                    }
                                } else {
                                    // Swipe verticale
                                    if (Math.abs(diffY) > SWIPE_THRESHOLD
                                            && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                                        move(diffY > 0 ? Direction.DOWN : Direction.UP);
                                        return true;
                                    }
                                }
                                return false;
                            }

                            @Override
                            public boolean onDoubleTap(MotionEvent e) {
                                // A partita finita il doppio tap ne inizia una nuova; durante la
                                // partita apre o chiude l'inventario.
                                if (viewModel.current().snapshot().phase() != Phase.PLAYING) {
                                    render(viewModel.newGame());
                                } else if (inventoryLayout.getVisibility() == View.VISIBLE) {
                                    closeInventory();
                                } else {
                                    openInventory();
                                }
                                return true;
                            }
                        });
    }

    /** Con l'inventario aperto la partita è in pausa: gli swipe non fanno niente. */
    private void move(Direction direction) {
        if (inventoryLayout.getVisibility() != View.VISIBLE) {
            render(viewModel.handle(new MoveCommand(direction)));
        }
    }

    private void openInventory() {
        // Pulisci la lista da eventuali oggetti precedenti
        inventoryItemList.removeAllViews();

        List<InventoryEntry> inventory = viewModel.current().snapshot().inventory();

        if (inventory.isEmpty()) {
            // Se l'inventario è vuoto, mostra un semplice testo
            TextView emptyText = new TextView(this);
            emptyText.setText(R.string.inventory_empty);
            emptyText.setTextColor(getColor(android.R.color.white));
            inventoryItemList.addView(emptyText);
        } else {
            // Popola la lista creando un TextView per ogni oggetto
            for (InventoryEntry entry : inventory) {
                TextView itemView = new TextView(this);
                itemView.setText(itemName(entry.kind()));
                itemView.setTextColor(getColor(android.R.color.white));
                itemView.setTextSize(20);
                itemView.setPadding(0, 8, 0, 8);
                itemView.setBackgroundResource(
                        android.R.drawable.btn_default); // Semplice sfondo cliccabile

                // Imposta l'azione al click: usare un oggetto consuma il turno
                itemView.setOnClickListener(
                        v -> {
                            closeInventory(); // Chiudi l'inventario dopo aver usato l'oggetto
                            render(viewModel.handle(new UseItemCommand(entry.id())));
                        });

                inventoryItemList.addView(itemView);
            }
        }

        // Rendi visibile la schermata dell'inventario
        inventoryLayout.setVisibility(View.VISIBLE);
    }

    private void closeInventory() {
        // Nascondi la schermata dell'inventario: aprirla e chiuderla non tocca la partita.
        inventoryLayout.setVisibility(View.GONE);
    }

    private String itemName(Kind kind) {
        if (kind.equals(StandardItemCatalog.POTION)) {
            return getString(R.string.kind_potion_name);
        }
        return kind.value();
    }

    /** Aggiorna tutta la UI con il risultato di un turno. */
    private void render(TurnResult result) {
        GameSnapshot snapshot = result.snapshot();
        PlayerView player = snapshot.player();

        // Aggiorna le ProgressBar
        healthBar.setMax(player.maxHealth());
        healthBar.setProgress(player.health());

        xpBar.setMax(player.experienceToNextLevel());
        xpBar.setProgress(player.experience());

        // A partita finita resta solo il messaggio, finché un doppio tap non ne inizia un'altra.
        switch (snapshot.phase()) {
            case PLAYING -> endMessage.setVisibility(View.GONE);
            case GAME_OVER -> showEndMessage(R.string.end_game_over);
            case VICTORY -> showEndMessage(R.string.end_victory);
        }

        // Ridisegna la GameView
        gameView.show(snapshot);
    }

    private void showEndMessage(int message) {
        closeInventory();
        endMessage.setText(message);
        endMessage.setVisibility(View.VISIBLE);
    }
}
