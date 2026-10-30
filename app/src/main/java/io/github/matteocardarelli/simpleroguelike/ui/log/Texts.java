package io.github.matteocardarelli.simpleroguelike.ui.log;

import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;

/**
 * I testi delle risorse, con i loro argomenti. Nell'app vengono dal {@code Context}; nei test da
 * una versione finta che restituisce l'id e gli argomenti, perché sulla JVM le risorse non ci sono.
 */
public interface Texts {

    String text(@StringRes int id, Object... args);

    /** Un testo che cambia con la quantità: "1 danno", "5 danni". */
    String quantity(@PluralsRes int id, int count, Object... args);
}
