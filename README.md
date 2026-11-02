# SimpleRogueLike

[![Android CI](https://github.com/MatteoCardarelli/SimpleRougeLike/actions/workflows/android-ci.yml/badge.svg?branch=master)](https://github.com/MatteoCardarelli/SimpleRougeLike/actions/workflows/android-ci.yml)

Un roguelike a turni per Android, scritto in Java.

Il gioco è volutamente piccolo: un piano, qualche nemico, delle pozioni. Il progetto invece è un esercizio di ingegneria del software: architettura esplicita, regole verificate automaticamente, test e integrazione continua, come in un prodotto vero.

> **Stato: `v1.6.0`.** Il refactoring è finito: l'app gioca sul nuovo motore (`:domain`, `:application`, `:infrastructure`), con Hilt e MVVM, e il codice di prima è stato rimosso. Le funzionalità nuove arrivano una alla volta, ognuna con la sua release, a partire dalle fondamenta (vedi [Roadmap](#roadmap)).

## Il gioco

- **Mappa a griglia** di stanze e corridoi, generata a ogni partita.
- **A turni:** il tempo passa solo quando il giocatore agisce, e dopo ogni sua azione agiscono tutti i nemici.
- **Swipe** per muoversi; muoversi verso un nemico vuol dire attaccarlo.
- **Esperienza e livelli:** ogni nemico sconfitto dà esperienza, ogni livello aumenta salute e attacco.
- **Pozioni**, normali e grandi, da raccogliere sulla mappa e usare dall'inventario, che si apre con un doppio tap: una griglia di oggetti con la scheda di quello selezionato.
- **Salute ed esperienza con i valori**, sulla mappa e in cima all'inventario.
- **Registro dei messaggi** con quello che succede in ogni turno, letto anche da TalkBack, che si può nascondere dalle impostazioni (l'ingranaggio in alto a destra).
- **Nebbia di guerra:** si vede solo quello che è nel campo visivo, e la mappa ricorda le zone già esplorate.
- **Vittoria e sconfitta:** si vince sconfiggendo tutti i nemici; la schermata di fine partita dice fin dove si è arrivati, e "Nuova partita" ne inizia un'altra.
- **Note di collaudo:** nelle build di debug, dalle impostazioni, si salva un'osservazione insieme a tutto quello che serve a rigiocare la partita fino a quel momento.
- **Accessibilità di base:** barre di salute ed esperienza descritte a parole, celle visibili ed esplorate distinte per luminosità, pulsanti e caselle da almeno 48 dp.

## Architettura

Architettura esagonale (*ports and adapters*) divisa in quattro moduli Gradle. Le dipendenze vanno solo verso il centro, cioè verso le regole del gioco.

```mermaid
flowchart LR
  app[":app<br/>Android · MVVM · Hilt"]
  application[":application<br/>turni, comandi, stati"]
  infrastructure[":infrastructure<br/>adapter verso SquidLib"]
  domain[":domain<br/>regole del gioco, Java puro"]

  app --> application
  app --> infrastructure
  application --> domain
  infrastructure --> domain
```

| Modulo | Cosa contiene |
|---|---|
| `:domain` | Le regole: mappa, personaggi, oggetti, combattimento, esperienza, intelligenza dei nemici. Java puro, senza Android e senza librerie esterne. Definisce le *porte*: le interfacce per generare il terreno, trovare un percorso e calcolare il campo visivo. |
| `:application` | Un turno dall'inizio alla fine: il comando del giocatore, le azioni dei nemici, lo stato della partita, cosa il giocatore può vedere. È l'unico punto d'ingresso per l'interfaccia. |
| `:infrastructure` | Gli *adapter*: implementano le porte del dominio usando SquidLib. Sostituire la libreria vuol dire scrivere un altro adapter, senza toccare le regole. |
| `:app` | L'app Android: trasforma i gesti in comandi, disegna la partita, riproduce gli eventi di ogni turno, gestisce l'inventario. |

**Le scelte principali**

- **Il gioco non conosce Android.** I tre moduli interni sono librerie Java: lì un `import android.*` non compila, e le regole si provano sulla JVM in pochi millisecondi.
- **Deterministico.** Stesso seed e stessi comandi danno la stessa partita, sul computer come sul telefono. Un bug si riproduce dal seed della partita in cui è comparso.
- **Ogni turno racconta cosa è successo.** Oltre allo stato finale, un turno restituisce la sequenza ordinata degli eventi (mosse, attacchi, danni, morti, oggetti raccolti). L'interfaccia li usa per i messaggi e, più avanti, per le animazioni.
- **Si aggiunge, non si modifica.** Un oggetto o un nemico nuovo è una riga di catalogo. Comportamenti dei nemici, effetti degli oggetti, comandi e regole di fine turno si aggiungono implementando un'interfaccia, senza cambiare le classi esistenti.

## Qualità

La CI, su ogni push e pull request, controlla formattazione, Lint (senza eccezioni registrate), NullAway e Animal Sniffer, esegue i test (unitari, di proprietà, di contratto, ArchUnit, golden) con le soglie di copertura e verifica i checksum delle dipendenze. Sulle pull request verso la branch principale esegue anche PIT e i test strumentati su un emulatore gestito da Gradle: avvio, rotazione, tasto indietro, inventario, impostazioni e il golden sul dispositivo, più l'avvio sulla build con R8.

- **Regole d'architettura verificate dai test** con ArchUnit: dipendenze tra moduli e package, niente Android nel dominio, niente sorgenti casuali che rompano il determinismo.
- **Test sulla JVM** con JUnit 5 e AssertJ, senza mock nel dominio: i test giocano su mappe disegnate in ASCII.
- **Test di proprietà** con jqwik, su sequenze casuali di mosse: due personaggi non occupano mai la stessa cella, un morto resta morto, la stessa partita giocata due volte dà lo stesso risultato.
- **Test di contratto:** ogni adapter e ogni implementazione finta usata nei test passano gli stessi test.
- **Test golden:** una partita registrata deve produrre lo stesso risultato sulla JVM e su un dispositivo Android.
- **Copertura e mutation testing** con soglie minime (JaCoCo e PIT).
- **Stile e controlli statici** bloccanti: Spotless, Android Lint, NullAway, Animal Sniffer (nessuna API Java assente da Android 11 nei moduli Java).
- **GitHub Actions** su ogni push e pull request, con i test strumentati su un emulatore prima del merge nella branch principale.

## Roadmap

- [x] **0. Preparazione:** configurazione di build condivisa, dipendenze ripulite, formattazione automatica, CI su tutte le branch.
- [x] **1–2. `:domain`:** valori di base, mappa, personaggi, oggetti, regole, generazione, intelligenza dei nemici.
- [x] **3. `:infrastructure`:** adapter deterministici per terreno, percorsi e campo visivo.
- [x] **4. `:application`:** turni completi, comandi, stati della partita.
- [x] **5. Nuova app Android:** la stessa app di prima sul nuovo motore, le correzioni del primo collaudo e le note di collaudo, Hilt e MVVM, il nuovo rendering con l'inventario a griglia, le funzionalità nuove (pozione grande, registro dei messaggi, fine partita, barre con i valori, impostazioni), i test sul dispositivo in CI e il workflow di release.
- [x] **6. Pulizia:** rimozione del codice vecchio e release `v1.0.0`.

- [ ] **7. Dopo la v1:** una funzionalità alla volta, ognuna con la sua issue `enhancement` e la sua release.
  - [x] Barre più pulite e impostazioni a tutto schermo, con il seed da copiare e la nuova partita (`v1.1.0`).
  - [x] Statistiche: attacco, difesa, precisione e schivata, con colpi che possono mancare (`v1.2.0`).
  - [x] Equipaggiamento: arma, armatura e anello, che si cambiano senza perdere il turno (`v1.3.0`).
  - [x] Coltelli da lancio, con la modalità mira, e arcieri che tengono la distanza (`v1.4.0`).
  - [x] Animazioni di passi, attacchi, colpi, morti e proiettili (`v1.5.0`).
  - [x] Porte, leve con i loro cancelli e scrigni (`v1.6.0`).
  - [ ] Cinque piani con le scale.
  - [ ] Nemici in branco, chiavi e bottino.
  - [ ] Il boss, uno slime gigante, e i mercanti.
  - [ ] Illustrazioni e salvataggio della partita.

## Compilare ed eseguire

Servono Android Studio, il JDK 21 e un dispositivo o un emulatore con Android 11 (API 30) o successivo. Si apre la cartella del progetto in Android Studio e si avvia la configurazione `app`, oppure da terminale:

```bash
./gradlew assembleDebug        # compila l'APK di debug
./gradlew testDebugUnitTest    # esegue i test unitari
./gradlew spotlessApply        # formatta il codice
```

Su Windows si usa `gradlew.bat` al posto di `./gradlew`.

Ogni release (un tag `vX.Y.Z` sulla branch principale) ha l'APK e l'AAB firmati allegati nella pagina delle release su GitHub.

## Tecnologie

Java 21 · Android SDK (minimo API 30) · AndroidX e Material Components · [SquidLib](https://github.com/yellowstonegames/SquidLib) per la generazione delle mappe e il campo visivo · Hilt · GitHub Actions. Test e controlli: JUnit 5, AssertJ, ArchUnit, jqwik, Espresso, PIT, JaCoCo, Spotless, NullAway, Animal Sniffer.

## Autore

Matteo Cardarelli.

Il progetto non ha ancora una licenza: tutti i diritti sono riservati.
