package io.github.matteocardarelli.simpleroguelike.ui.game;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.pressBack;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import io.github.matteocardarelli.simpleroguelike.R;
import io.github.matteocardarelli.simpleroguelike.command.MoveCommand;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.ui.inventory.InventorySheet;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import org.junit.runner.RunWith;

/** L'app vera sul dispositivo: avvio, rotazione, tasto indietro e doppio tap sull'inventario. */
@RunWith(AndroidJUnit4.class)
public class GameActivityTest {

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
}
