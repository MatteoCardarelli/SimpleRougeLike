package com.github.matteoCarda.simplerougelike.controller;

/**
 * Interfaccia per notificare la UI che lo stato del gioco è cambiato
 * e che è necessario aggiornare le componenti visive.
 */
public interface GameUpdateListener {
    /**
     * Chiamato quando i dati del gioco sono cambiati e la UI deve essere aggiornata.
     */
    void onGameUpdated();
}
    