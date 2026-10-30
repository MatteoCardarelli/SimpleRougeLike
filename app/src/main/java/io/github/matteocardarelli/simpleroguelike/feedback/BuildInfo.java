package io.github.matteocardarelli.simpleroguelike.feedback;

/**
 * Da quale codice è compilata l'app: una nota di collaudo si rigioca identica solo sullo stesso.
 *
 * @param commit il commit, con {@code -dirty} se c'erano modifiche non committate
 */
public record BuildInfo(String appVersion, String commit) {}
