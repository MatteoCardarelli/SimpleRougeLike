package io.github.matteocardarelli.simpleroguelike.ui.settings;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;
import io.github.matteocardarelli.simpleroguelike.R;
import io.github.matteocardarelli.simpleroguelike.databinding.DialogSettingsBinding;
import org.jspecify.annotations.Nullable;

/**
 * Le impostazioni, a tutto schermo e scure come il gioco: il seed della partita da copiare, la
 * partita nuova, l'interruttore del registro dei messaggi e, nelle build di debug, la nota di
 * collaudo. Si chiudono con la X in alto o con indietro. È un {@link DialogFragment}: dopo una
 * rotazione il sistema la ricrea. Conosce solo {@link SettingsHost}, che l'Activity deve
 * implementare.
 */
public final class SettingsDialog extends DialogFragment {

    public static final String TAG = "settings";

    private SettingsHost host() {
        return (SettingsHost) requireActivity();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.Theme_SimpleRogueLike_Settings);
        getChildFragmentManager()
                .setFragmentResultListener(
                        ConfirmNewGameDialog.CONFIRMED, this, (key, result) -> startNewGame());
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        DialogSettingsBinding binding = DialogSettingsBinding.inflate(inflater, container, false);
        binding.toolbar.setNavigationOnClickListener(v -> dismiss());

        String seed = Long.toString(host().currentSeed());
        binding.seedValue.setText(getString(R.string.settings_seed_value, seed));
        binding.copySeed.setOnClickListener(v -> copySeed(seed));
        binding.newGame.setOnClickListener(v -> onNewGameClicked());

        binding.logSwitch.setChecked(host().currentSettings().logVisible());
        binding.logSwitch.setOnCheckedChangeListener(
                (button, checked) -> host().onLogVisibilityChanged(checked));

        if (getResources().getBoolean(R.bool.feedback_enabled)) {
            binding.testingSection.setVisibility(View.VISIBLE);
            binding.writeNote.setOnClickListener(
                    v -> {
                        SettingsHost host = host();
                        dismiss();
                        host.onWriteNoteRequested();
                    });
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        drawUnderSystemBars(view);
    }

    /**
     * La finestra si disegna anche sotto le barre di sistema, come la mappa, su tutte le versioni
     * di Android: gli insets di barre e ritaglio diventano il margine interno, così la X e l'ultima
     * voce restano raggiungibili.
     */
    private void drawUnderSystemBars(View root) {
        Window window = requireDialog().getWindow();
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false);
        }
        ViewCompat.setOnApplyWindowInsetsListener(
                root,
                (view, windowInsets) -> {
                    Insets insets =
                            windowInsets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                                            | WindowInsetsCompat.Type.displayCutout());
                    view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
                    return WindowInsetsCompat.CONSUMED;
                });
    }

    /**
     * Mette il seed negli appunti. Da Android 13 il sistema mostra già la sua conferma: l'avviso
     * dell'app serve solo prima.
     */
    private void copySeed(String seed) {
        ClipboardManager clipboard = requireContext().getSystemService(ClipboardManager.class);
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.settings_seed), seed));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(requireContext(), R.string.settings_seed_copied, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    /** A partita in corso chiede conferma; a partita finita non c'è niente da perdere. */
    private void onNewGameClicked() {
        if (!host().gameInProgress()) {
            startNewGame();
        } else if (getChildFragmentManager().findFragmentByTag(ConfirmNewGameDialog.TAG) == null) {
            new ConfirmNewGameDialog().show(getChildFragmentManager(), ConfirmNewGameDialog.TAG);
        }
    }

    private void startNewGame() {
        SettingsHost host = host();
        dismiss();
        host.onNewGameRequested();
    }
}
