package io.github.matteocardarelli.simpleroguelike.ui.render;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.RectF;
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
    private final Paint bitmapPaint = PixelSprites.newPaint();
    private final ColorFilter dim = new LightingColorFilter(0xFF8C8C8C, 0);
    private @Nullable PixelSprites sprites;
    private final RectF bitmapBounds = new RectF();
    private @Nullable HeroSprites heroSprites;
    private final Map<Kind, Drawable> arts = new LinkedHashMap<>();
    private @Nullable Camera camera;
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
    public void setUp(Camera camera, ContentPresentation content) {
        this.camera = camera;
        this.sprites = new PixelSprites(getContext());
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
        PixelSprites sprites = this.sprites;
        ContentPresentation content = this.content;
        GameSnapshot snapshot = this.snapshot;
        Scene scene = this.scene;
        if (camera == null
                || sprites == null
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
        drawCells(canvas, sprites, cells, viewport, cell);
        drawRug(canvas, cells, snapshot.shop(), cell);
        drawFixtures(canvas, sprites, cells, snapshot.fixtures(), cell);
        drawItems(canvas, content, snapshot.visibleItems(), cell);
        drawDanger(canvas, snapshot.danger(), cell);
        drawSprites(
                canvas, content, scene.sprites(), phases(snapshot), snapshot.player().id(), cell);
        drawEffects(canvas, content, scene.effects(), cell);
        canvas.restore();
        if (scene.isAnimating()) {
            postInvalidateOnAnimation();
        }
    }

    /**
     * Le celle con la loro immagine: il pavimento in una delle tre varianti, scelta dalla
     * posizione, il muro di faccia se sotto c'è spazio, altrimenti dall'alto. Quelle solo esplorate
     * sono le stesse immagini, scurite.
     */
    private void drawCells(
            Canvas canvas,
            PixelSprites sprites,
            Grid<CellView> cells,
            Viewport viewport,
            float cell) {
        // Le celle della griglia sono in ordine di riga: si leggono per indice, senza creare una
        // Position per cella a ogni disegno.
        List<CellView> all = cells.cells();
        int width = cells.width();
        for (int y = viewport.firstY(); y <= viewport.lastY(); y++) {
            for (int x = viewport.firstX(); x <= viewport.lastX(); x++) {
                CellView at = all.get(y * width + x);
                if (at.visibility() == Visibility.UNEXPLORED) {
                    continue;
                }
                int id;
                if (at.wall()) {
                    boolean belowInGrid = y + 1 < cells.height();
                    boolean belowIsWall = belowInGrid && all.get((y + 1) * width + x).wall();
                    id = TerrainTiles.wall(belowInGrid, belowIsWall);
                } else {
                    id = TerrainTiles.floor(x, y);
                }
                drawBitmap(
                        canvas,
                        sprites.bitmap(id),
                        x * cell,
                        y * cell,
                        cell,
                        at.visibility() == Visibility.VISIBLE ? 255 : -1);
            }
        }
        bitmapPaint.setColorFilter(null);
    }

    /**
     * Disegna un'immagine pixel art nel quadrato di lato {@code side}; {@code alpha} da 0 a 255, o
     * -1 per scurirla come una cella solo esplorata.
     */
    private void drawBitmap(Canvas canvas, Bitmap bitmap, float x, float y, float side, int alpha) {
        bitmapPaint.setColorFilter(alpha < 0 ? dim : null);
        bitmapPaint.setAlpha(alpha < 0 ? 255 : alpha);
        bitmapBounds.set(x, y, x + side, y + side);
        canvas.drawBitmap(bitmap, null, bitmapBounds, bitmapPaint);
    }

    /**
     * Gli oggetti fissi visti almeno una volta, scuriti come le celle fuori dal campo visivo.
     * L'immagine dipende dal tipo e dallo stato: aperto, o tirato per la leva.
     */
    private void drawFixtures(
            Canvas canvas,
            PixelSprites sprites,
            Grid<CellView> cells,
            List<FixtureView> fixtures,
            float cell) {
        for (FixtureView fixture : fixtures) {
            int id = FixtureSprites.sprite(fixture.kind().value(), fixture.open());
            if (id == 0) {
                continue;
            }
            boolean visible = cells.get(fixture.position()).visibility() == Visibility.VISIBLE;
            drawBitmap(
                    canvas,
                    sprites.bitmap(id),
                    fixture.position().x() * cell,
                    fixture.position().y() * cell,
                    cell,
                    visible ? 255 : -1);
        }
        bitmapPaint.setColorFilter(null);
    }

    private void drawItems(
            Canvas canvas, ContentPresentation content, List<ItemView> items, float cell) {
        float inset = cell * 0.15f;
        for (ItemView item : items) {
            float x = item.position().x() * cell;
            float y = item.position().y() * cell;
            int sprite = content.spriteRes(item.kind());
            if (sprite != 0 && sprites != null) {
                drawBitmap(canvas, sprites.bitmap(sprite), x, y, cell, 255);
            } else {
                drawArt(canvas, content, item.kind(), x + inset, y + inset, cell - 2 * inset, 1f);
            }
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
            EntityId playerId,
            float cell) {
        for (Sprite sprite : sprites) {
            float side = sprite.size() * cell;
            float x = sprite.x() * cell;
            float y = sprite.y() * cell;
            if (sprite.id().equals(playerId)) {
                drawHero(canvas, sprite, x, y, side);
                continue;
            }
            ActorView jumper = phases.get(sprite.id());
            float alpha = sprite.alpha();
            float width = side;
            float height = side;
            if (jumper != null && jumper.charging()) {
                float wider = side * SQUASH / 2;
                x -= wider;
                y += side * SQUASH;
                width = side + 2 * wider;
                height = side * (1 - SQUASH);
            } else if (jumper != null && jumper.airborne()) {
                y -= side * LIFT;
                alpha *= 0.85f;
            } else if (sprite.walkFrame() >= 0) {
                y -= Math.round(2 * (sprite.walkFrame() % 2) * cell / 64f);
            }
            int actorSprite = ActorSprites.sprite(sprite.kind(), sprite.facing());
            if (actorSprite != 0) {
                drawActor(canvas, actorSprite, x, y, width, height, alpha);
            } else {
                drawArt(canvas, content, sprite.kind(), x, y, width, height, alpha);
            }
        }
    }

    /** Un attore in pixel art nel rettangolo che parte da (x, y). */
    private void drawActor(
            Canvas canvas, int id, float x, float y, float width, float height, float alpha) {
        PixelSprites pixels = sprites;
        if (pixels == null) {
            pixels = new PixelSprites(getContext());
            sprites = pixels;
        }
        bitmapPaint.setColorFilter(null);
        bitmapPaint.setAlpha(Math.round(alpha * 255));
        bitmapBounds.set(x, y, x + width, y + height);
        canvas.drawBitmap(pixels.bitmap(id), null, bitmapBounds, bitmapPaint);
    }

    /** L'eroe: l'immagine pixel art per dove guarda e per il fotogramma di camminata. */
    private void drawHero(Canvas canvas, Sprite sprite, float x, float y, float side) {
        HeroSprites heroes = heroSprites;
        if (heroes == null) {
            heroes = new HeroSprites(getContext());
            heroSprites = heroes;
        }
        Bitmap bitmap = heroes.bitmap(sprite.facing(), sprite.walkFrame());
        bitmapPaint.setColorFilter(null);
        bitmapPaint.setAlpha(Math.round(sprite.alpha() * 255));
        bitmapBounds.set(x, y, x + side, y + side);
        canvas.drawBitmap(bitmap, null, bitmapBounds, bitmapPaint);
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
            int fx = EffectSprites.drawable(effect.kind(), effect.walkFrame());
            if (fx != 0 && sprites != null) {
                drawBitmap(
                        canvas,
                        sprites.bitmap(fx),
                        effect.x() * cell,
                        effect.y() * cell,
                        cell,
                        255);
                continue;
            }
            float inset = cell * 0.25f;
            float left = effect.x() * cell + inset;
            float top = effect.y() * cell + inset;
            float size = cell - 2 * inset;
            canvas.save();
            canvas.rotate(
                    projectileRotation(effect.facing()),
                    effect.x() * cell + cell / 2f,
                    effect.y() * cell + cell / 2f);
            int sprite = content.spriteRes(effect.kind());
            if (sprite != 0 && sprites != null) {
                drawBitmap(
                        canvas,
                        sprites.bitmap(sprite),
                        effect.x() * cell,
                        effect.y() * cell,
                        cell,
                        255);
            } else {
                drawArt(canvas, content, effect.kind(), left, top, size, 1f);
            }
            canvas.restore();
        }
    }

    /**
     * Di quanti gradi, in senso orario sullo schermo, si ruota un proiettile perché punti verso
     * {@code facing}: le immagini di frecce e coltelli guardano in alto a destra (nord-est).
     */
    static float projectileRotation(Direction facing) {
        return switch (facing) {
            case RIGHT -> 45f;
            case DOWN -> 135f;
            case LEFT -> -135f;
            case UP -> -45f;
        };
    }
}
