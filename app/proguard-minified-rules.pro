# Regole di R8 solo per la build minified, quella dei test strumentati con R8.
#
# L'APK dei test non contiene le librerie che l'app ha già, ma il runner e Espresso ne usano parti
# che l'app non usa: R8 le toglierebbe dall'app e il runner si fermerebbe all'avvio. Si conservano
# solo le librerie condivise con il runner; il codice dell'app passa da R8 come nella release, che
# non ha bisogno di queste regole.
-keep class kotlin.** { *; }
-keep class androidx.tracing.** { *; }
-keep class androidx.lifecycle.** { *; }
-keep class androidx.concurrent.** { *; }
-keep class com.google.common.util.concurrent.** { *; }
-keep class javax.inject.** { *; }
-keep class dagger.** { *; }

# Gli id delle viste che i test cercano con Espresso: R8 li copia nel codice dell'app e toglie la
# classe.
-keep class io.github.matteocardarelli.simpleroguelike.R$id { *; }

# Lo store della partita salvata: i test lo usano per cancellare il salvataggio dalla stessa coda
# delle scritture dell'app, e R8 ne rinominerebbe i metodi.
-keep class io.github.matteocardarelli.simpleroguelike.save.FileGameStore { *; }
