package com.github.matteoCarda.simplerougelike.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.github.matteoCarda.simplerougelike.controller.GameController;
import com.github.matteoCarda.simplerougelike.model.entity.Enemy;
import com.github.matteoCarda.simplerougelike.model.entity.Item;
import com.github.matteoCarda.simplerougelike.model.entity.Player;
import com.github.matteoCarda.simplerougelike.model.tile.Tile; // Import necessario

/**
 * La View principale del gioco, responsabile di tutto il rendering. Disegna la mappa, il giocatore,
 * i nemici e gli oggetti basandosi sullo stato fornito dal GameController. Implementa una
 * telecamera che segue il giocatore e un sistema di Field of View (FOV) per il "fog of war".
 */
public class GameView extends View {

    private GameController gameController;
    private Paint paint; // Un unico oggetto Paint per disegnare tutto, per efficienza.

    // Dimensione fissa per ogni cella della griglia. Aumentala o diminuiscila per zoomare.
    private final float cellSize = 80f;

    // Dimensioni dello schermo in pixel, per calcolare la telecamera.
    private int screenWidth;
    private int screenHeight;

    // --- COSTRUTTORI ---
    // Questi sono i costruttori standard di una View Android. Chiamano tutti il metodo init().
    public GameView(Context context) {
        super(context);
        init();
    }

    public GameView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GameView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * Inizializza gli oggetti necessari per la View. Ottiene l'istanza del GameController e crea
     * l'oggetto Paint.
     */
    private void init() {
        gameController = GameController.getInstance();
        paint = new Paint();
    }

    /**
     * Metodo chiamato da Android quando le dimensioni della View cambiano. Lo usiamo per
     * memorizzare larghezza e altezza dello schermo.
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.screenWidth = w;
        this.screenHeight = h;
    }

    /**
     * Il cuore del rendering. Questo metodo viene chiamato da Android ogni volta che la view deve
     * essere ridisegnata.
     *
     * @param canvas L'area di disegno su cui operare.
     */
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        // Sfondo nero, per le aree fuori mappa.
        canvas.drawColor(Color.BLACK);

        // Se non c'è una partita in corso, non c'è nulla da disegnare.
        if (gameController.getGameMap() == null
                || gameController.getGameState() != GameController.GameState.PLAYING) {
            return;
        }

        Player player = gameController.getGameMap().getPlayer();
        double[][] fovMap =
                gameController.getPlayerFov(); // La mappa della visibilità del giocatore.

        // --- 1. CALCOLO DELLA TELECAMERA ---
        float cameraOffsetX = player.getX() * cellSize - screenWidth / 2f + cellSize / 2f;
        float cameraOffsetY = player.getY() * cellSize - screenHeight / 2f + cellSize / 2f;

        canvas.save(); // Salviamo lo stato attuale del canvas...
        canvas.translate(-cameraOffsetX, -cameraOffsetY);

        // --- 2. OTTIMIZZAZIONE: CULLING ---
        // Calcoliamo quali tile sono visibili sullo schermo per non disegnare tutta la mappa.
        int startX = (int) (cameraOffsetX / cellSize);
        int endX = startX + (int) (screenWidth / cellSize) + 2;
        int startY = (int) (cameraOffsetY / cellSize);
        int endY = startY + (int) (screenHeight / cellSize) + 2;

        // Limitiamo i calcoli ai bordi della mappa per evitare errori.
        startX = Math.max(0, startX);
        endX = Math.min(gameController.getGameMap().getWidth(), endX);
        startY = Math.max(0, startY);
        endY = Math.min(gameController.getGameMap().getHeight(), endY);

        // --- NUOVA LOGICA DI DISEGNO A 2 STATI ---
        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                Tile tile = gameController.getGameMap().getTile(x, y);
                if (tile == null) continue;

                boolean isInFov = fovMap != null && fovMap[x][y] > 0.0;

                if (isInFov) {
                    // STATO 1: DENTRO IL CAMPO VISIVO (colori normali/saturi)
                    paint.setColor(
                            tile.isWalkable()
                                    ? Color.DKGRAY
                                    : Color.rgb(139, 69, 19)); // Marrone per i muri
                } else {
                    // STATO 2: FUORI DAL CAMPO VISIVO (colori scuri/desaturati)
                    paint.setColor(
                            tile.isWalkable()
                                    ? Color.rgb(40, 40, 40)
                                    : Color.rgb(50, 25, 5)); // Grigio scuro / Marrone scuro
                }

                // Disegniamo la casella in ogni caso, non ci sono più aree nere.
                canvas.drawRect(
                        x * cellSize, y * cellSize, (x + 1) * cellSize, (y + 1) * cellSize, paint);
            }
        }

        // --- 3. DISEGNO DELLE ENTITÀ ---
        // La logica qui è corretta: disegna oggetti e nemici SOLO se sono nel campo visivo attuale.
        drawItems(canvas, fovMap);
        drawEnemies(canvas, fovMap);

        // Disegniamo il giocatore per ultimo in modo che sia sempre in cima
        drawPlayer(canvas, player);

        // --- 4. RIPRISTINO DEL CANVAS ---
        canvas.restore();
    }

    /**
     * Disegna il giocatore sulla mappa.
     *
     * @param canvas Il canvas su cui disegnare.
     * @param player L'oggetto Player da disegnare.
     */
    private void drawPlayer(Canvas canvas, Player player) {
        paint.setColor(Color.GREEN);
        canvas.drawCircle(
                player.getX() * cellSize + cellSize / 2,
                player.getY() * cellSize + cellSize / 2,
                cellSize / 2.2f,
                paint);
    }

    /**
     * Disegna tutti gli oggetti presenti sulla mappa che sono visibili al giocatore.
     *
     * @param canvas Il canvas su cui disegnare.
     * @param fovMap La mappa del campo visivo per controllare la visibilità.
     */
    private void drawItems(Canvas canvas, double[][] fovMap) {
        if (fovMap == null) return;
        paint.setColor(Color.CYAN);
        for (Item item : gameController.getGameMap().getItems()) {
            if (fovMap[item.getX()][item.getY()] > 0.0) {
                float offset = cellSize * 0.25f;
                canvas.drawRect(
                        item.getX() * cellSize + offset,
                        item.getY() * cellSize + offset,
                        (item.getX() + 1) * cellSize - offset,
                        (item.getY() + 1) * cellSize - offset,
                        paint);
            }
        }
    }

    /**
     * Disegna tutti i nemici vivi che sono visibili al giocatore.
     *
     * @param canvas Il canvas su cui disegnare.
     * @param fovMap La mappa del campo visivo per controllare la visibilità.
     */
    private void drawEnemies(Canvas canvas, double[][] fovMap) {
        if (fovMap == null) return;
        paint.setColor(Color.RED);
        for (Enemy enemy : gameController.getGameMap().getEnemies()) {
            // BUGFIX: Ho aggiunto il controllo dei limiti dell'array per fovMap
            if (gameController.isAlive(enemy)
                    && enemy.getX() >= 0
                    && enemy.getX() < fovMap.length
                    && enemy.getY() >= 0
                    && enemy.getY() < fovMap[0].length
                    && fovMap[enemy.getX()][enemy.getY()] > 0.0) {
                canvas.drawCircle(
                        enemy.getX() * cellSize + cellSize / 2,
                        enemy.getY() * cellSize + cellSize / 2,
                        cellSize / 2.2f,
                        paint);
            }
        }
    }
}
