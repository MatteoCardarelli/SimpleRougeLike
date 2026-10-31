package io.github.matteocardarelli.simpleroguelike.ui.settings;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import io.github.matteocardarelli.simpleroguelike.R;
import io.github.matteocardarelli.simpleroguelike.databinding.DialogSettingsBinding;
import org.jspecify.annotations.Nullable;

/**
 * Le impostazioni: l'interruttore del registro dei messaggi e, nelle build di debug, il pulsante
 * per scrivere una nota di collaudo. È un {@link DialogFragment}: dopo una rotazione il sistema la
 * ricrea. Conosce solo {@link SettingsHost}, che l'Activity deve implementare.
 */
public final class SettingsDialog extends DialogFragment {

    public static final String TAG = "settings";

    private SettingsHost host() {
        return (SettingsHost) requireActivity();
    }

    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        DialogSettingsBinding binding = DialogSettingsBinding.inflate(getLayoutInflater());
        binding.logSwitch.setChecked(host().currentSettings().logVisible());
        binding.logSwitch.setOnCheckedChangeListener(
                (button, checked) -> host().onLogVisibilityChanged(checked));
        if (getResources().getBoolean(R.bool.feedback_enabled)) {
            binding.writeNote.setVisibility(View.VISIBLE);
            binding.writeNote.setOnClickListener(
                    v -> {
                        SettingsHost host = host();
                        dismiss();
                        host.onWriteNoteRequested();
                    });
        }
        return new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_title)
                .setView(binding.getRoot())
                .setPositiveButton(R.string.settings_close, null)
                .create();
    }
}
