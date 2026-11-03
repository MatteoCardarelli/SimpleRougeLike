package io.github.matteocardarelli.simpleroguelike.ai;

/**
 * Cosa vuole fare un nemico in questo turno. Le strategie la decidono, la sessione la esegue con la
 * stessa regola del player: un passo verso un ostile è un attacco, un colpo a distanza segue la
 * regola dei coltelli lanciati; un nemico grande può annunciare un salto, atterrare al turno dopo e
 * riposare.
 *
 * <p>La gerarchia è chiusa: chi la interpreta usa uno {@code switch} esaustivo, e un'intenzione
 * nuova non compila finché non viene gestita.
 */
public sealed interface EnemyIntent
        permits StepIntent, RangedAttackIntent, JumpIntent, LandIntent, RestIntent, WaitIntent {}
