package io.github.matteocardarelli.simpleroguelike.ui.feedback;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import io.github.matteocardarelli.simpleroguelike.R;
import org.jspecify.annotations.Nullable;

/**
 * La finestra per scrivere una nota di collaudo. È un {@link DialogFragment}: dopo una rotazione il
 * sistema la ricrea con il testo già scritto. Conosce solo {@link FeedbackHost}, che l'Activity
 * deve implementare.
 */
public final class FeedbackDialog extends DialogFragment {

    public static final String TAG = "feedback";

    // La vista di una finestra non ha un genitore a cui agganciarsi: è il caso in cui Lint ammette
    // di non passarne uno.
    @SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.dialog_feedback, null);
        EditText text = view.findViewById(R.id.feedback_text);
        return new AlertDialog.Builder(requireContext())
                .setTitle(R.string.feedback_title)
                .setView(view)
                .setPositiveButton(
                        R.string.feedback_save,
                        (dialog, which) ->
                                ((FeedbackHost) requireActivity())
                                        .onFeedbackWritten(text.getText().toString()))
                .setNegativeButton(R.string.feedback_cancel, null)
                .create();
    }
}
