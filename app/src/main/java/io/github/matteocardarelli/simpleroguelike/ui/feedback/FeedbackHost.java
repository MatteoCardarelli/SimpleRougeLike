package io.github.matteocardarelli.simpleroguelike.ui.feedback;

/** Chi riceve il testo di una nota: l'Activity che ospita {@link FeedbackDialog}. */
public interface FeedbackHost {

    /** Il giocatore ha scritto una nota e ha scelto di salvarla; il testo può essere vuoto. */
    void onFeedbackWritten(String text);
}
