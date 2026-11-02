package io.github.matteocardarelli.simpleroguelike.ui.settings;

import android.app.Dialog;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import io.github.matteocardarelli.simpleroguelike.R;
import org.jspecify.annotations.Nullable;

/**
 * Chiede conferma prima di una partita nuova, perché quella in corso andrebbe persa. La apre {@link
 * SettingsDialog} come fragment figlio, e la risposta torna con un risultato del fragment: così la
 * domanda e la risposta sopravvivono a una rotazione.
 */
public final class ConfirmNewGameDialog extends DialogFragment {

    public static final String TAG = "confirm_new_game";

    /** La chiave del risultato, inviato solo se il giocatore conferma. */
    public static final String CONFIRMED = "new_game_confirmed";

    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(
                        requireContext(), R.style.Theme_SimpleRogueLike_ConfirmDialog)
                .setTitle(R.string.confirm_new_game_title)
                .setMessage(R.string.confirm_new_game_message)
                .setPositiveButton(
                        R.string.new_game,
                        (dialog, which) ->
                                getParentFragmentManager()
                                        .setFragmentResult(CONFIRMED, new Bundle()))
                .setNegativeButton(R.string.confirm_new_game_cancel, null)
                .create();
    }
}
