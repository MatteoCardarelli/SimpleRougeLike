package io.github.matteocardarelli.simpleroguelike.ui.render;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.snapshot.CellView;
import io.github.matteocardarelli.simpleroguelike.snapshot.GameSnapshot;
import io.github.matteocardarelli.simpleroguelike.snapshot.ItemView;
import io.github.matteocardarelli.simpleroguelike.ui.content.ContentPresentation;
import io.github.matteocardarelli.simpleroguelike.ui.playback.Scene;
import io.github.matteocardarelli.simpleroguelike.ui.playback.Sprite;
import java.util.List;
import java.util.OptionalInt;
import org.jspecify.annotations.Nullable;

/**
 * Disegna la partita: le celle dallo snapshot, già filtrato dalla nebbia di guerra, gli oggetti a
 * terra visibili, e gli attori dalla scena, che durante un'animazione si discosta dallo snapshot.
 * La telecamera segue il player.
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
        Viewport viewport =
                camera.centerOn(
                        snapshot.player().position(),
                        getWidth(),
                        getHeight(),
                        cells.width(),
                        cells.height());
        float cell = camera.cellSizePx();

        canvas.save();
        canvas.translate(-viewport.offsetX(), -viewport.offsetY());
        drawCells(canvas, palette, cells, viewport, cell);
        drawItems(canvas, content, snapshot.visibleItems(), cell);
        drawSprites(canvas, content, scene.sprites(), cell);
        canvas.restore();
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
}
