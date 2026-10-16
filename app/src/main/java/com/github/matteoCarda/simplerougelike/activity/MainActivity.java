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

import com.github.matteoCarda.simplerougelike.R;
import com.github.matteoCarda.simplerougelike.controller.GameController;
import com.github.matteoCarda.simplerougelike.controller.GameUpdateListener;
import com.github.matteoCarda.simplerougelike.model.entity.Item;
import com.github.matteoCarda.simplerougelike.model.entity.Player;
import com.github.matteoCarda.simplerougelike.view.GameView;

import java.util.List;

/**
 * Activity principale dell'app.
 * Ospita la GameView e cattura l'input dell'utente (swipe) per passarlo al GameController.
 */
public class MainActivity extends AppCompatActivity implements GameUpdateListener {

    private GameController gameController;
    private GameView gameView;
    private GestureDetector gameViewGestureDetector;
    private GestureDetector inventoryGestureDetector;
    private ProgressBar healthBar;
    private ProgressBar xpBar;
    private LinearLayout inventoryLayout;
    private LinearLayout inventoryItemList;
    private static final int MAP_WIDTH = 50;
    private static final int MAP_HEIGHT = 50;
    private static final int ENEMY_COUNT = 20;
    private static final int ITEM_COUNT = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inizializzazione dei componenti principali
        gameController = GameController.getInstance();
        gameController.setGameUpdateListener(this);
        gameView = findViewById(R.id.gameView);

        // Trova le barre di stato nel layout e le passa al controller
        healthBar = findViewById(R.id.healthBar);
        xpBar = findViewById(R.id.xpBar);
        inventoryLayout = findViewById(R.id.inventory_layout);
        inventoryItemList = findViewById(R.id.inventory_item_list);

        // NUOVO SISTEMA PER GESTIRE IL TASTO "INDIETRO"
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
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
                    // Per farlo in modo sicuro, disabilitiamo temporaneamente questo callback
                    // e chiamiamo di nuovo il dispatcher.
                    setEnabled(false); // Disabilita questo callback
                    getOnBackPressedDispatcher().onBackPressed(); // Esegui l'azione standard
                }
            }
        });


        if (savedInstanceState == null || gameController.getGameState() != GameController.GameState.PLAYING) {
            // Inizia una NUOVA partita SOLO se:
            // 1. L'app è stata appena avviata.
            // 2. O se la partita precedente era finita.
            gameController.startNewGame(MAP_WIDTH, MAP_HEIGHT, ENEMY_COUNT, ITEM_COUNT);
        }

        // Imposta il listener per gli swipe.
        setupGestureDetector();
        onGameUpdated();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Passa SEMPRE l'evento al nostro unico GestureDetector
        boolean handled = gameViewGestureDetector.onTouchEvent(event);
        if (!handled) {return super.onTouchEvent(event);
        }
        return true;
    }


    /**
     * Configura il GestureDetector per tradurre gli swipe in comandi di gioco.
     */
    private void setupGestureDetector() {
        gameViewGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100; // Minima distanza (pixel)
            private static final int SWIPE_VELOCITY_THRESHOLD = 100; // Minima velocità

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (inventoryLayout.getVisibility() != View.VISIBLE) {

                    float diffX = e2.getX() - e1.getX();
                    float diffY = e2.getY() - e1.getY();

                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        // Swipe orizzontale
                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) {
                                gameController.handlePlayerTurn(GameController.Direction.RIGHT);
                            } else {
                                gameController.handlePlayerTurn(GameController.Direction.LEFT);
                            }
                            return true;
                        }
                    } else {
                        // Swipe verticale
                        if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffY > 0) {
                                gameController.handlePlayerTurn(GameController.Direction.DOWN);
                            } else {
                                gameController.handlePlayerTurn(GameController.Direction.UP);
                            }
                            return true;
                        }
                    }
                }
                return false;
            }
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (inventoryLayout.getVisibility() == View.VISIBLE) {
                    closeInventory();
                } else {
                    openInventory();
                }
                return true;
            }
        });
    }

    private void openInventory() {
        // Mettiamo in pausa il gioco
        gameController.requestOpenInventory();

        // Pulisci la lista da eventuali oggetti precedenti
        inventoryItemList.removeAllViews();

        List<Item> inventory = gameController.getGameMap().getPlayer().getInventory();

        if (inventory.isEmpty()) {
            // Se l'inventario è vuoto, mostra un semplice testo
            TextView emptyText = new TextView(this);
            emptyText.setText("Il tuo inventario è vuoto.");
            emptyText.setTextColor(getResources().getColor(android.R.color.white, null));
            inventoryItemList.addView(emptyText);
        } else {
            // Popola la lista creando un TextView per ogni oggetto
            for (Item item : inventory) {
                TextView itemView = new TextView(this);
                itemView.setText(item.getName());
                itemView.setTextColor(getResources().getColor(android.R.color.white));
                itemView.setTextSize(20);
                itemView.setPadding(0, 8, 0, 8);
                itemView.setBackgroundResource(android.R.drawable.btn_default); // Semplice sfondo cliccabile

                // Imposta l'azione al click
                itemView.setOnClickListener(v -> {
                    gameController.useItemFromInventory(item);
                    closeInventory(); // Chiudi l'inventario dopo aver usato l'oggetto
                });

                inventoryItemList.addView(itemView);
            }
        }

        // Rendi visibile la schermata dell'inventario
        inventoryLayout.setVisibility(View.VISIBLE);
    }

    private void closeInventory() {
        // Nascondi la schermata dell'inventario
        inventoryLayout.setVisibility(View.GONE);
        // Riprendi il gioco
        gameController.requestCloseInventory();
    }

    @Override
    public void onGameUpdated() {
        // Questa funzione ora centralizza TUTTI gli aggiornamenti della UI// 1. Recupera i dati più recenti dal controller
        Player player = gameController.getGameMap().getPlayer();
        if (player == null) return; // Sicurezza

        // 2. Aggiorna le ProgressBar
        healthBar.setMax(player.getMaxHealth());
        healthBar.setProgress(player.getHealth());

        xpBar.setMax(player.getExperienceToNextLevel());
        xpBar.setProgress(player.getExperience());

        // 3. Ridisegna la GameView
        gameView.invalidate();
    }



}
