package io.github.matteocardarelli.simpleroguelike.ui.game;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressBack;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.matcher.ViewMatchers.Visibility;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import io.github.matteocardarelli.simpleroguelike.R;
import io.github.matteocardarelli.simpleroguelike.command.MoveCommand;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.settings.PreferencesSettingsStore;
import io.github.matteocardarelli.simpleroguelike.snapshot.PlayerView;
import io.github.matteocardarelli.simpleroguelike.ui.inventory.InventorySheet;
import io.github.matteocardarelli.simpleroguelike.ui.settings.SettingsDialog;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * L'app vera sul dispositivo: avvio, rotazione, tasto indietro, doppio tap sull'inventario, barre,
 * impostazioni e partita nuova.
 */
@RunWith(AndroidJUnit4.class)
public class GameActivityTest {

    /** Le impostazioni restano tra un avvio e l'altro: ogni test parte da quelle iniziali. */
    @Before
    @After
    public void clearTheSettings() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences(PreferencesSettingsStore.FILE, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    private static GameViewModel viewModel(GameActivity activity) {
        return new ViewModelProvider(activity).get(GameViewModel.class);
    }

    /** Il controllo minimo, che gira anche sulla build con R8. */
    @Test
    public void launch_showsTheMapAndTheBars() {
        try (ActivityScenario<GameActivity> scenario =
                ActivityScenario.launch(GameActivity.class)) {
            onView(withId(R.id.game_view)).check(matches(isDisplayed()));
            onView(withId(R.id.health_bar)).check(matches(isDisplayed()));
        }
    }

    /** Bug B3 e B7: dopo una rotazione la partita è la stessa e l'inventario resta aperto. */
    @Test
    public void rotation_keepsTheGameAndTheOpenInventory() {
        try (ActivityScenario<GameActivity> scenario =
                ActivityScenario.launch(GameActivity.class)) {
            AtomicLong revision = new AtomicLong();
            AtomicLong seed = new AtomicLong();
            scenario.onActivity(
                    activity -> {
                        GameViewModel viewModel = viewModel(activity);
                        viewModel.submit(new MoveCommand(Direction.UP));
                        viewModel.toggleInventory();
                        activity.getSupportFragmentManager().executePendingTransactions();
                        revision.set(viewModel.state().result().revision());
                        seed.set(viewModel.state().result().snapshot().seed());
                    });

            scenario.recreate();

            scenario.onActivity(
                    activity -> {
                        GameViewModel viewModel = viewModel(activity);
                        assertEquals(revision.get(), viewModel.state().result().revision());
                        assertEquals(seed.get(), viewModel.state().result().snapshot().seed());
                        assertTrue(viewModel.state().inventoryOpen());
                        assertNotNull(
                                activity.getSupportFragmentManager()
                                        .findFragmentByTag(InventorySheet.TAG));
                    });
        }
    }

    /**
     * Bug B4: dopo un ritorno dalle app recenti, indietro chiude ancora l'inventario e l'Activity
     * resta in primo piano.
     */
    @Test
    public void back_withTheInventoryOpen_closesIt_alsoAfterComingBackFromRecents() {
        try (ActivityScenario<GameActivity> scenario =
                ActivityScenario.launch(GameActivity.class)) {
            scenario.moveToState(Lifecycle.State.CREATED);
            scenario.moveToState(Lifecycle.State.RESUMED);
            scenario.onActivity(
                    activity -> {
                        viewModel(activity).toggleInventory();
                        activity.getSupportFragmentManager().executePendingTransactions();
                    });
            // Indietro va alla finestra dello sheet: si aspetta che ci sia e che abbia il fuoco.
            onView(withId(R.id.grid)).inRoot(isDialog()).check(matches(isDisplayed()));

            onView(withId(R.id.grid)).inRoot(isDialog()).perform(pressBack());

            scenario.onActivity(
                    activity -> assertFalse(viewModel(activity).state().inventoryOpen()));
            assertEquals(Lifecycle.State.RESUMED, scenario.getState());
        }
    }

    /** Bug B6: un doppio tap sopra la lista chiude l'inventario senza usare niente. */
    @Test
    public void doubleTap_onTheInventoryList_closesItWithoutUsingAnything() {
        try (ActivityScenario<GameActivity> scenario =
                ActivityScenario.launch(GameActivity.class)) {
            AtomicLong revision = new AtomicLong();
            scenario.onActivity(
                    activity -> {
                        GameViewModel viewModel = viewModel(activity);
                        viewModel.toggleInventory();
                        activity.getSupportFragmentManager().executePendingTransactions();
                        revision.set(viewModel.state().result().revision());
                    });
            onView(withId(R.id.grid)).inRoot(isDialog()).check(matches(isDisplayed()));

            scenario.onActivity(GameActivityTest::doubleTapOnTheInventoryGrid);

            scenario.onActivity(
                    activity -> {
                        GameViewModel viewModel = viewModel(activity);
                        assertFalse(viewModel.state().inventoryOpen());
                        assertEquals(revision.get(), viewModel.state().result().revision());
                    });
        }
    }

    /**
     * Un doppio tap al centro della griglia, mandato alla finestra dello sheet. I tempi degli
     * eventi sono fissati: con i tocchi di Espresso, su un emulatore lento il secondo può arrivare
     * dopo il tempo massimo di un doppio tap.
     */
    private static void doubleTapOnTheInventoryGrid(GameActivity activity) {
        InventorySheet sheet =
                (InventorySheet)
                        activity.getSupportFragmentManager().findFragmentByTag(InventorySheet.TAG);
        assertNotNull(sheet);
        View grid = sheet.requireView().findViewById(R.id.grid);
        int[] position = new int[2];
        grid.getLocationInWindow(position);
        float x = position[0] + grid.getWidth() / 2f;
        float y = position[1] + grid.getHeight() / 2f;
        long start = SystemClock.uptimeMillis();
        int[] actions = {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_UP
        };
        long[] times = {0, 50, 150, 200};
        for (int i = 0; i < actions.length; i++) {
            long downTime = start + (i < 2 ? 0 : times[2]);
            MotionEvent event = MotionEvent.obtain(downTime, start + times[i], actions[i], x, y, 0);
            sheet.requireDialog().dispatchTouchEvent(event);
            event.recycle();
        }
    }

    /** Issue #33: con l'inventario aperto si leggono salute ed esperienza con i loro valori. */
    @Test
    public void inventory_showsHealthAndExperienceWithTheirValues() {
        try (ActivityScenario<GameActivity> scenario =
                ActivityScenario.launch(GameActivity.class)) {
            AtomicReference<String> health = new AtomicReference<>();
            AtomicReference<String> experience = new AtomicReference<>();
            scenario.onActivity(
                    activity -> {
                        GameViewModel viewModel = viewModel(activity);
                        viewModel.toggleInventory();
                        activity.getSupportFragmentManager().executePendingTransactions();
                        PlayerView player = viewModel.state().result().snapshot().player();
                        health.set(
                                activity.getString(
                                        R.string.hud_health_value,
                                        player.health(),
                                        player.maxHealth()));
                        experience.set(
                                activity.getString(
                                        R.string.hud_experience_value,
                                        player.experience(),
                                        player.experienceToNextLevel(),
                                        player.level()));
                    });

            onView(withId(R.id.health_value))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                    .check(matches(withText(health.get())));
            onView(withId(R.id.xp_value))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                    .check(matches(withText(experience.get())));
        }
    }

    /** Issue #34: il registro nascosto dalle impostazioni resta nascosto dopo una rotazione. */
    @Test
    public void settings_hidingTheLog_keepsItHiddenAfterARotation() {
        try (ActivityScenario<GameActivity> scenario =
                ActivityScenario.launch(GameActivity.class)) {
            onView(withId(R.id.log)).check(matches(isDisplayed()));

            onView(withId(R.id.settings_button)).perform(click());
            onView(withId(R.id.log_switch)).inRoot(isDialog()).perform(click());
            onView(withContentDescription(R.string.settings_close))
                    .inRoot(isDialog())
                    .perform(click());

            onView(withId(R.id.log)).check(matches(withEffectiveVisibility(Visibility.GONE)));
            scenario.recreate();
            onView(withId(R.id.log)).check(matches(withEffectiveVisibility(Visibility.GONE)));
            scenario.onActivity(activity -> assertFalse(viewModel(activity).state().logVisible()));
        }
    }

    /**
     * Issue #39: sulla mappa le barre non hanno numeri, solo il livello; TalkBack legge comunque i
     * valori.
     */
    @Test
    public void map_showsTheLevelWithoutTheValues() {
        try (ActivityScenario<GameActivity> scenario =
                ActivityScenario.launch(GameActivity.class)) {
            AtomicReference<String> level = new AtomicReference<>();
            scenario.onActivity(
                    activity -> {
                        PlayerView player =
                                viewModel(activity).state().result().snapshot().player();
                        level.set(activity.getString(R.string.hud_level, player.level()));
                        View healthBar = activity.findViewById(R.id.health_bar);
                        assertEquals(
                                activity.getString(
                                        R.string.a11y_health_value,
                                        player.health(),
                                        player.maxHealth()),
                                String.valueOf(healthBar.getStateDescription()));
                    });

            onView(withId(R.id.health_value))
                    .check(matches(withEffectiveVisibility(Visibility.GONE)));
            onView(withId(R.id.xp_value)).check(matches(withText(level.get())));
        }
    }

    /** Issue #39: le impostazioni occupano tutto lo schermo e si chiudono con la X. */
    @Test
    public void settings_fillTheScreen_andCloseWithTheX() {
        try (ActivityScenario<GameActivity> scenario =
                ActivityScenario.launch(GameActivity.class)) {
            onView(withId(R.id.settings_button)).perform(click());
            onView(withId(R.id.toolbar)).inRoot(isDialog()).check(matches(isDisplayed()));
            scenario.onActivity(
                    activity -> {
                        View screen = activity.getWindow().getDecorView();
                        View settings = settingsDialog(activity).requireView();
                        assertEquals(screen.getWidth(), settings.getWidth());
                        assertEquals(screen.getHeight(), settings.getHeight());
                    });

            onView(withContentDescription(R.string.settings_close))
                    .inRoot(isDialog())
                    .perform(click());

            scenario.onActivity(activity -> assertNull(settingsDialogOrNull(activity)));
        }
    }

    /** Issue #39: anche indietro chiude le impostazioni. */
    @Test
    public void settings_closeWithBack() {
        try (ActivityScenario<GameActivity> scenario =
                ActivityScenario.launch(GameActivity.class)) {
            onView(withId(R.id.settings_button)).perform(click());
            onView(withId(R.id.toolbar)).inRoot(isDialog()).perform(pressBack());

            scenario.onActivity(activity -> assertNull(settingsDialogOrNull(activity)));
            assertEquals(Lifecycle.State.RESUMED, scenario.getState());
        }
    }

    /** Issue #39: le impostazioni aperte restano aperte dopo una rotazione. */
    @Test
    public void settings_stayOpenAfterARotation() {
        try (ActivityScenario<GameActivity> scenario =
                ActivityScenario.launch(GameActivity.class)) {
            onView(withId(R.id.settings_button)).perform(click());
            onView(withId(R.id.toolbar)).inRoot(isDialog()).check(matches(isDisplayed()));

            scenario.recreate();

            onView(withId(R.id.toolbar)).inRoot(isDialog()).check(matches(isDisplayed()));
        }
    }

    /**
     * Issue #39: le impostazioni mostrano il seed della partita, e "Copia" lo mette negli appunti.
     */
    @Test
    public void settings_showTheSeed_andCopyIt() {
        try (ActivityScenario<GameActivity> scenario =
                ActivityScenario.launch(GameActivity.class)) {
            AtomicLong seed = new AtomicLong();
            scenario.onActivity(
                    activity -> seed.set(viewModel(activity).state().result().snapshot().seed()));

            onView(withId(R.id.settings_button)).perform(click());
            onView(withId(R.id.seed_value))
                    .inRoot(isDialog())
                    .check(matches(withText(Long.toString(seed.get()))));
            onView(withId(R.id.copy_seed)).inRoot(isDialog()).perform(click());

            scenario.onActivity(
                    activity -> {
                        ClipboardManager clipboard =
                                activity.getSystemService(ClipboardManager.class);
                        ClipData clip = clipboard.getPrimaryClip();
                        assertNotNull(clip);
                        assertEquals(
                                Long.toString(seed.get()),
                                String.valueOf(clip.getItemAt(0).getText()));
                    });
        }
    }

    /** Issue #39: annullando la conferma, la partita in corso resta com'è. */
    @Test
    public void settings_newGame_cancelled_keepsTheGame() {
        try (ActivityScenario<GameActivity> scenario =
                ActivityScenario.launch(GameActivity.class)) {
            AtomicLong seed = new AtomicLong();
            AtomicLong turn = new AtomicLong();
            scenario.onActivity(
                    activity -> {
                        GameViewModel viewModel = viewModel(activity);
                        viewModel.submit(new MoveCommand(Direction.UP));
                        seed.set(viewModel.state().result().snapshot().seed());
                        turn.set(viewModel.state().result().snapshot().turn());
                    });

            onView(withId(R.id.settings_button)).perform(click());
            onView(withId(R.id.new_game)).inRoot(isDialog()).perform(click());
            onView(withId(android.R.id.button2)).inRoot(isDialog()).perform(click());

            onView(withId(R.id.toolbar)).inRoot(isDialog()).check(matches(isDisplayed()));
            scenario.onActivity(
                    activity -> {
                        GameViewModel viewModel = viewModel(activity);
                        assertEquals(seed.get(), viewModel.state().result().snapshot().seed());
                        assertEquals(turn.get(), viewModel.state().result().snapshot().turn());
                    });
        }
    }

    /**
     * Issue #39: la conferma sopravvive a una rotazione e, confermata, chiude le impostazioni e
     * inizia un'altra partita.
     */
    @Test
    public void settings_newGame_confirmedAfterARotation_startsAnotherGame() {
        try (ActivityScenario<GameActivity> scenario =
                ActivityScenario.launch(GameActivity.class)) {
            AtomicLong seed = new AtomicLong();
            scenario.onActivity(
                    activity -> {
                        GameViewModel viewModel = viewModel(activity);
                        viewModel.submit(new MoveCommand(Direction.UP));
                        seed.set(viewModel.state().result().snapshot().seed());
                    });

            onView(withId(R.id.settings_button)).perform(click());
            onView(withId(R.id.new_game)).inRoot(isDialog()).perform(click());
            onView(withId(android.R.id.button1)).inRoot(isDialog()).check(matches(isDisplayed()));
            scenario.recreate();
            onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click());

            scenario.onActivity(
                    activity -> {
                        assertNull(settingsDialogOrNull(activity));
                        GameViewModel viewModel = viewModel(activity);
                        assertNotEquals(seed.get(), viewModel.state().result().snapshot().seed());
                        // Una partita nuova parte dal primo turno.
                        assertEquals(1, viewModel.state().result().snapshot().turn());
                    });
        }
    }

    private static @Nullable SettingsDialog settingsDialogOrNull(GameActivity activity) {
        activity.getSupportFragmentManager().executePendingTransactions();
        return (SettingsDialog)
                activity.getSupportFragmentManager().findFragmentByTag(SettingsDialog.TAG);
    }

    private static SettingsDialog settingsDialog(GameActivity activity) {
        SettingsDialog settings = settingsDialogOrNull(activity);
        assertNotNull(settings);
        return settings;
    }
}
