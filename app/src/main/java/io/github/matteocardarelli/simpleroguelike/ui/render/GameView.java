package io.github.matteocardarelli.simpleroguelike.ui.render;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.snapshot.CellView;
import io.github.matteocardarelli.simpleroguelike.snapshot.FixtureView;
import io.github.matteocardarelli.simpleroguelike.snapshot.GameSnapshot;
import io.github.matteocardarelli.simpleroguelike.snapshot.ItemView;
import io.github.matteocardarelli.simpleroguelike.snapshot.Visibility;
import io.github.matteocardarelli.simpleroguelike.ui.content.ContentPresentation;
import io.github.matteocardarelli.simpleroguelike.ui.playback.Scene;
import io.github.matteocardarelli.simpleroguelike.ui.playback.Sprite;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.jspecify.annotations.Nullable;

/**
 * Disegna la partita: le celle dallo snapshot, già filtrato dalla nebbia di guerra, gli oggetti a
 * terra visibili, e gli attori dalla scena, che durante un'animazione si discosta dallo snapshot.
 * La telecamera segue il player; durante un'animazione la vista si ridisegna a ogni fotogramma.
 */
public final class GameView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private @Nullable Camera camera;
    private @Nullable TilePalette palette;
    private @Nullable ContentPresentation content;
    private @Nullable GameSnapshot snapshot;
    private @Nullable Scene scene;

    public GameView(Context context) {
        super(context);
    }

    public GameView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public GameView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Gli strumenti per disegnare: la View la crea il layout, quindi non li riceve nel costruttore.
     */
    public void setUp(Camera camera, TilePalette palette, ContentPresentation content) {
        this.camera = camera;
        this.palette = palette;
        this.content = content;
        invalidate();
    }

    /** Mostra la partita e la scena, e ridisegna. */
    public void show(GameSnapshot snapshot, Scene scene) {
        this.snapshot = snapshot;
        this.scene = scene;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.BLACK);
        Camera camera = this.camera;
        TilePalette palette = this.palette;
        ContentPresentation content = this.content;
        GameSnapshot snapshot = this.snapshot;
        Scene scene = this.scene;
        if (camera == null
                || palette == null
                || content == null
                || snapshot == null
                || scene == null) {
            return;
        }
        Grid<CellView> cells = snapshot.cells();
        // La telecamera segue lo sprite del player, che durante un passo è tra due celle.
        float centerX = snapshot.player().position().x();
        float centerY = snapshot.player().position().y();
        Optional<Sprite> player = scene.sprite(snapshot.player().id());
        if (player.isPresent()) {
            centerX = player.get().x();
            centerY = player.get().y();
        }
        Viewport viewport =
                camera.centerOn(
                        centerX, centerY, getWidth(), getHeight(), cells.width(), cells.height());
        float cell = camera.cellSizePx();

        canvas.save();
        canvas.translate(-viewport.offsetX(), -viewport.offsetY());
        drawCells(canvas, palette, cells, viewport, cell);
        drawFixtures(canvas, content, cells, snapshot.fixtures(), cell);
        drawItems(canvas, content, snapshot.visibleItems(), cell);
        drawSprites(canvas, content, scene.sprites(), cell);
        drawEffects(canvas, content, scene.effects(), cell);
        canvas.restore();
        if (scene.isAnimating()) {
            postInvalidateOnAnimation();
        }
    }

    private void drawCells(
            Canvas canvas,
            TilePalette palette,
            Grid<CellView> cells,
            Viewport viewport,
            float cell) {
        // Le celle della griglia sono in ordine di riga: si leggono per indice, senza creare una
        // Position per cella a ogni disegno.
        List<CellView> all = cells.cells();
        for (int y = viewport.firstY(); y <= viewport.lastY(); y++) {
            for (int x = viewport.firstX(); x <= viewport.lastX(); x++) {
                OptionalInt color = palette.cellColor(all.get(y * cells.width() + x));
                if (color.isPresent()) {
                    paint.setColor(color.getAsInt());
                    canvas.drawRect(x * cell, y * cell, (x + 1) * cell, (y + 1) * cell, paint);
                }
            }
        }
    }

    /**
     * Gli oggetti fissi visti almeno una volta, più tenui fuori dal campo visivo. Una porta chiusa
     * riempie la cella, aperta resta solo il telaio; un cancello sono sbarre; una leva è una tacca
     * nel muro, abbassata se tirata; uno scrigno aperto è solo il contorno.
     */
    private void drawFixtures(
            Canvas canvas,
            ContentPresentation content,
            Grid<CellView> cells,
            List<FixtureView> fixtures,
            float cell) {
        for (FixtureView fixture : fixtures) {
            boolean visible = cells.get(fixture.position()).visibility() == Visibility.VISIBLE;
            paint.setColor(getContext().getColor(content.colorRes(fixture.kind())));
            paint.setAlpha(visible ? 255 : 120);
            float x = fixture.position().x() * cell;
            float y = fixture.position().y() * cell;
            float inset = cell * 0.15f;
            switch (fixture.kind().value()) {
                case "locked_door", "boss_door" -> {
                    canvas.drawRect(x + inset, y, x + cell - inset, y + cell, paint);
                    paint.setColor(Color.BLACK);
                    canvas.drawCircle(x + cell / 2, y + cell / 2, cell / 10f, paint);
                }
                case "door" -> {
                    if (fixture.open()) {
                        frame(canvas, x + inset, y + inset, x + cell - inset, y + cell - inset);
                    } else {
                        canvas.drawRect(x + inset, y, x + cell - inset, y + cell, paint);
                    }
                }
                case "gate" -> {
                    if (!fixture.open()) {
                        float bar = cell / 10f;
                        for (int i = 1; i <= 3; i++) {
                            float bx = x + cell * i / 4f;
                            canvas.drawRect(bx - bar / 2, y, bx + bar / 2, y + cell, paint);
                        }
                    }
                }
                case "lever" -> {
                    float top = fixture.open() ? y + cell * 0.55f : y + cell * 0.2f;
                    canvas.drawRect(
                            x + cell * 0.4f, top, x + cell * 0.6f, top + cell * 0.25f, paint);
                }
                case "chest" -> {
                    if (fixture.open()) {
                        frame(
                                canvas,
                                x + inset,
                                y + cell * 0.3f,
                                x + cell - inset,
                                y + cell - inset);
                    } else {
                        canvas.drawRect(
                                x + inset,
                                y + cell * 0.3f,
                                x + cell - inset,
                                y + cell - inset,
                                paint);
                    }
                }
                case "stairs" -> {
                    float step = cell / 4f;
                    for (int i = 0; i < 3; i++) {
                        float top = y + step * (i + 0.5f);
                        canvas.drawRect(
                                x + inset + step * i,
                                top,
                                x + cell - inset,
                                top + step * 0.6f,
                                paint);
                    }
                }
                default ->
                        canvas.drawRect(
                                x + inset, y + inset, x + cell - inset, y + cell - inset, paint);
            }
        }
        paint.setAlpha(255);
    }

    private void frame(Canvas canvas, float left, float top, float right, float bottom) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(2f, (right - left) / 10f));
        canvas.drawRect(left, top, right, bottom, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawItems(
            Canvas canvas, ContentPresentation content, List<ItemView> items, float cell) {
        float inset = cell * 0.25f;
        for (ItemView item : items) {
            paint.setColor(getContext().getColor(content.colorRes(item.kind())));
            float x = item.position().x() * cell;
            float y = item.position().y() * cell;
            canvas.drawRect(x + inset, y + inset, x + cell - inset, y + cell - inset, paint);
        }
    }

    private void drawSprites(
            Canvas canvas, ContentPresentation content, List<Sprite> sprites, float cell) {
        for (Sprite sprite : sprites) {
            paint.setColor(getContext().getColor(content.colorRes(sprite.kind())));
            paint.setAlpha(Math.round(sprite.alpha() * 255));
            canvas.drawCircle(
                    sprite.x() * cell + cell / 2, sprite.y() * cell + cell / 2, cell / 2.2f, paint);
        }
        paint.setAlpha(255);
    }

    /** Gli effetti, come un proiettile in volo: piccoli, sopra gli attori. */
    private void drawEffects(
            Canvas canvas, ContentPresentation content, List<Sprite> effects, float cell) {
        for (Sprite effect : effects) {
            paint.setColor(getContext().getColor(content.colorRes(effect.kind())));
            canvas.drawCircle(
                    effect.x() * cell + cell / 2, effect.y() * cell + cell / 2, cell / 6f, paint);
        }
    }
}
