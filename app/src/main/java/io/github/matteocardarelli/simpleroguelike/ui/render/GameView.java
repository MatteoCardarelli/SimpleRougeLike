package io.github.matteocardarelli.simpleroguelike.ui.render;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import androidx.appcompat.content.res.AppCompatResources;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.snapshot.ActorView;
import io.github.matteocardarelli.simpleroguelike.snapshot.CellView;
import io.github.matteocardarelli.simpleroguelike.snapshot.FixtureView;
import io.github.matteocardarelli.simpleroguelike.snapshot.GameSnapshot;
import io.github.matteocardarelli.simpleroguelike.snapshot.ItemView;
import io.github.matteocardarelli.simpleroguelike.snapshot.Visibility;
import io.github.matteocardarelli.simpleroguelike.ui.content.ContentPresentation;
import io.github.matteocardarelli.simpleroguelike.ui.playback.Scene;
import io.github.matteocardarelli.simpleroguelike.ui.playback.Sprite;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Disegna la partita: le celle dallo snapshot, già filtrato dalla nebbia di guerra, gli oggetti a
 * terra visibili con le loro illustrazioni, e gli attori dalla scena, che durante un'animazione si
 * discosta dallo snapshot. La telecamera segue il player; durante un'animazione la vista si
 * ridisegna a ogni fotogramma.
 */
public final class GameView extends View {

    /** Il rosso semitrasparente delle celle dove sta per atterrare un nemico. */
    private static final int DANGER = 0x66FF1744;

    private static final int SHADOW = 0x99000000;

    /** Quanto si schiaccia uno slime che si carica, in proporzione alla sua altezza. */
    private static final float SQUASH = 0.2f;

    /** Quanto sta sopra la sua ombra uno slime in aria, in proporzione alla sua altezza. */
    private static final float LIFT = 0.3f;

    private static final int RUG = 0xFF8E2430;
    private static final int RUG_BORDER = 0xFFE5B23C;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Map<Kind, Drawable> arts = new LinkedHashMap<>();
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
        drawRug(canvas, cells, snapshot.shop(), cell);
        drawFixtures(canvas, content, cells, snapshot.fixtures(), cell);
        drawItems(canvas, content, snapshot.visibleItems(), cell);
        drawDanger(canvas, snapshot.danger(), cell);
        drawSprites(canvas, content, scene.sprites(), phases(snapshot), cell);
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
        float inset = cell * 0.15f;
        for (ItemView item : items) {
            float x = item.position().x() * cell;
            float y = item.position().y() * cell;
            drawArt(canvas, content, item.kind(), x + inset, y + inset, cell - 2 * inset, 1f);
        }
    }

    /** I nemici visibili che si caricano per saltare o sono in aria, per id. */
    private static Map<EntityId, ActorView> phases(GameSnapshot snapshot) {
        Map<EntityId, ActorView> phases = new LinkedHashMap<>();
        for (ActorView enemy : snapshot.visibleEnemies()) {
            if (enemy.charging() || enemy.airborne()) {
                phases.put(enemy.id(), enemy);
            }
        }
        return phases;
    }

    /**
     * Gli attori. Chi si carica per saltare è schiacciato verso terra; chi è in aria sta sopra la
     * sua ombra, un po' trasparente.
     */
    private void drawSprites(
            Canvas canvas,
            ContentPresentation content,
            List<Sprite> sprites,
            Map<EntityId, ActorView> phases,
            float cell) {
        for (Sprite sprite : sprites) {
            float side = sprite.size() * cell;
            float x = sprite.x() * cell;
            float y = sprite.y() * cell;
            ActorView jumper = phases.get(sprite.id());
            if (jumper != null && jumper.charging()) {
                float wider = side * SQUASH / 2;
                drawArt(
                        canvas,
                        content,
                        sprite.kind(),
                        x - wider,
                        y + side * SQUASH,
                        side + 2 * wider,
                        side * (1 - SQUASH),
                        sprite.alpha());
            } else if (jumper != null && jumper.airborne()) {
                drawArt(
                        canvas,
                        content,
                        sprite.kind(),
                        x,
                        y - side * LIFT,
                        side,
                        side,
                        sprite.alpha() * 0.85f);
            } else {
                drawArt(canvas, content, sprite.kind(), x, y, side, side, sprite.alpha());
            }
        }
    }

    /** L'illustrazione di un {@code Kind} nel quadrato di lato {@code side} che parte da (x, y). */
    private void drawArt(
            Canvas canvas,
            ContentPresentation content,
            Kind kind,
            float x,
            float y,
            float side,
            float alpha) {
        drawArt(canvas, content, kind, x, y, side, side, alpha);
    }

    /** L'illustrazione di un {@code Kind} nel rettangolo che parte da (x, y). */
    private void drawArt(
            Canvas canvas,
            ContentPresentation content,
            Kind kind,
            float x,
            float y,
            float width,
            float height,
            float alpha) {
        Drawable art = art(content, kind);
        art.setBounds(Math.round(x), Math.round(y), Math.round(x + width), Math.round(y + height));
        art.setAlpha(Math.round(alpha * 255));
        art.draw(canvas);
        art.setAlpha(255);
    }

    /** Le illustrazioni, caricate una volta per tipo. */
    private Drawable art(ContentPresentation content, Kind kind) {
        Drawable art = arts.get(kind);
        if (art == null) {
            Drawable loaded =
                    Objects.requireNonNull(
                            AppCompatResources.getDrawable(getContext(), content.artRes(kind)));
            art = loaded.mutate();
            arts.put(kind, art);
        }
        return art;
    }

    /**
     * Il tappeto del negozio sulle celle della sua zona, con un bordo dorato dove finisce: uscendo
     * dal tappeto con merce non pagata si ruba. Più scuro sulle celle che il player non vede
     * adesso.
     */
    private void drawRug(Canvas canvas, Grid<CellView> cells, List<Position> rug, float cell) {
        if (rug.isEmpty()) {
            return;
        }
        Set<Position> inside = new LinkedHashSet<>(rug);
        float edge = cell * 0.12f;
        for (Position at : rug) {
            int alpha = cells.get(at).visibility() == Visibility.VISIBLE ? 255 : 120;
            float left = at.x() * cell;
            float top = at.y() * cell;
            paint.setColor(RUG);
            paint.setAlpha(alpha);
            canvas.drawRect(left, top, left + cell, top + cell, paint);
            paint.setColor(RUG_BORDER);
            paint.setAlpha(alpha);
            if (!inside.contains(at.translate(Direction.UP))) {
                canvas.drawRect(left, top, left + cell, top + edge, paint);
            }
            if (!inside.contains(at.translate(Direction.DOWN))) {
                canvas.drawRect(left, top + cell - edge, left + cell, top + cell, paint);
            }
            if (!inside.contains(at.translate(Direction.LEFT))) {
                canvas.drawRect(left, top, left + edge, top + cell, paint);
            }
            if (!inside.contains(at.translate(Direction.RIGHT))) {
                canvas.drawRect(left + cell - edge, top, left + cell, top + cell, paint);
            }
        }
        paint.setAlpha(255);
    }

    /**
     * L'ombra di chi è in aria, dove atterrerà al prossimo turno: le sue celle in rosso e un ovale
     * scuro sopra.
     */
    private void drawDanger(Canvas canvas, List<Position> danger, float cell) {
        if (danger.isEmpty()) {
            return;
        }
        paint.setColor(DANGER);
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (Position at : danger) {
            canvas.drawRect(
                    at.x() * cell, at.y() * cell, (at.x() + 1) * cell, (at.y() + 1) * cell, paint);
            minX = Math.min(minX, at.x());
            minY = Math.min(minY, at.y());
            maxX = Math.max(maxX, at.x());
            maxY = Math.max(maxY, at.y());
        }
        paint.setColor(SHADOW);
        float inset = cell * 0.4f;
        canvas.drawOval(
                minX * cell + inset,
                minY * cell + inset,
                (maxX + 1) * cell - inset,
                (maxY + 1) * cell - inset,
                paint);
    }

    /** Gli effetti, come un proiettile in volo: piccoli, sopra gli attori. */
    private void drawEffects(
            Canvas canvas, ContentPresentation content, List<Sprite> effects, float cell) {
        for (Sprite effect : effects) {
            float inset = cell * 0.25f;
            drawArt(
                    canvas,
                    content,
                    effect.kind(),
                    effect.x() * cell + inset,
                    effect.y() * cell + inset,
                    cell - 2 * inset,
                    1f);
        }
    }
}
