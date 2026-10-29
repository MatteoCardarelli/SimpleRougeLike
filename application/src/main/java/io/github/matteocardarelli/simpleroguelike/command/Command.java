package io.github.matteocardarelli.simpleroguelike.command;

/**
 * Un'azione chiesta dal giocatore, come dato puro: l'interfaccia la crea senza conoscere nessun
 * servizio. Chi la esegue è il {@link CommandHandler} registrato per la sua classe.
 *
 * <p>La gerarchia è aperta: un comando nuovo è una classe nuova più il suo handler, senza toccare
 * il codice esistente.
 */
public interface Command {}
