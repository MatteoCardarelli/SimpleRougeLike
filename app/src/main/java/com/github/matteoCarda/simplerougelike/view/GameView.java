package com.github.matteoCarda.simplerougelike.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.snapshot.ActorView;
import io.github.matteocardarelli.simpleroguelike.snapshot.CellView;
import io.github.matteocardarelli.simpleroguelike.snapshot.GameSnapshot;
import io.github.matteocardarelli.simpleroguelike.snapshot.ItemView;
import io.github.matteocardarelli.simpleroguelike.snapshot.Phase;
import java.util.List;

/**
 * La View principale del gioco, responsabile di tutto il rendering. Disegna la mappa, il giocatore,
 * i nemici e gli oggetti dalla fotografia della partita dopo l'ultimo turno, già filtrata dalla
 * nebbia di guerra. Implementa una telecamera che segue il giocatore.
 */
public class GameView extends View {

    private @Nullable GameSnapshot snapshot;
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

    /** Inizializza gli oggetti necessari per la View: crea l'oggetto Paint. */
    private void init() {
        paint = new Paint();
    }

    /** Mostra la partita dopo un turno e ridisegna la View. */
    public void show(GameSnapshot snapshot) {
        this.snapshot = snapshot;
        invalidate();
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
        GameSnapshot current = snapshot;
        if (current == null || current.phase() != Phase.PLAYING) {
            return;
        }

        Position player = current.player().position();

        // --- 1. CALCOLO DELLA TELECAMERA ---
        float cameraOffsetX = player.x() * cellSize - screenWidth / 2f + cellSize / 2f;
        float cameraOffsetY = player.y() * cellSize - screenHeight / 2f + cellSize / 2f;

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
        endX = Math.min(current.cells().width(), endX);
        startY = Math.max(0, startY);
        endY = Math.min(current.cells().height(), endY);

        // --- DISEGNO A 3 STATI ---
        // Le celle della griglia sono in ordine di riga: si leggono per indice, senza creare
        // una Position per cella a ogni disegno.
        List<CellView> cells = current.cells().cells();
        int width = current.cells().width();
        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                CellView cell = cells.get(y * width + x);
                switch (cell.visibility()) {
                    // DENTRO IL CAMPO VISIVO (colori normali/saturi)
                    case VISIBLE ->
                            paint.setColor(
                                    cell.wall()
                                            ? Color.rgb(139, 69, 19) // Marrone per i muri
                                            : Color.DKGRAY);
                    // GIÀ VISTA, FUORI DAL CAMPO VISIVO (colori scuri/desaturati)
                    case EXPLORED ->
                            paint.setColor(
                                    cell.wall()
                                            ? Color.rgb(50, 25, 5) // Marrone scuro
                                            : Color.rgb(40, 40, 40)); // Grigio scuro
                    // MAI VISTA: resta nera, la fotografia non dice cosa c'è.
                    case UNEXPLORED -> {
                        continue;
                    }
                }

                canvas.drawRect(
                        x * cellSize, y * cellSize, (x + 1) * cellSize, (y + 1) * cellSize, paint);
            }
        }

        // --- 3. DISEGNO DELLE ENTITÀ ---
        // La fotografia contiene solo oggetti e nemici nel campo visivo attuale.
        drawItems(canvas, current);
        drawEnemies(canvas, current);

        // Disegniamo il giocatore per ultimo in modo che sia sempre in cima
        drawPlayer(canvas, player);

        // --- 4. RIPRISTINO DEL CANVAS ---
        canvas.restore();
    }

    /**
     * Disegna il giocatore sulla mappa.
     *
     * @param canvas Il canvas su cui disegnare.
     * @param player La cella del giocatore.
     */
    private void drawPlayer(Canvas canvas, Position player) {
        paint.setColor(Color.GREEN);
        canvas.drawCircle(
                player.x() * cellSize + cellSize / 2,
                player.y() * cellSize + cellSize / 2,
                cellSize / 2.2f,
                paint);
    }

    /**
     * Disegna gli oggetti a terra che il giocatore vede.
     *
     * @param canvas Il canvas su cui disegnare.
     * @param snapshot La partita da disegnare.
     */
    private void drawItems(Canvas canvas, GameSnapshot snapshot) {
        paint.setColor(Color.CYAN);
        float offset = cellSize * 0.25f;
        for (ItemView item : snapshot.visibleItems()) {
            Position at = item.position();
            canvas.drawRect(
                    at.x() * cellSize + offset,
                    at.y() * cellSize + offset,
                    (at.x() + 1) * cellSize - offset,
                    (at.y() + 1) * cellSize - offset,
                    paint);
        }
    }

    /**
     * Disegna i nemici vivi che il giocatore vede.
     *
     * @param canvas Il canvas su cui disegnare.
     * @param snapshot La partita da disegnare.
     */
    private void drawEnemies(Canvas canvas, GameSnapshot snapshot) {
        paint.setColor(Color.RED);
        for (ActorView enemy : snapshot.visibleEnemies()) {
            Position at = enemy.position();
            canvas.drawCircle(
                    at.x() * cellSize + cellSize / 2,
                    at.y() * cellSize + cellSize / 2,
                    cellSize / 2.2f,
                    paint);
        }
    }
}
