package io.github.matteocardarelli.simpleroguelike.ai;

import io.github.matteocardarelli.simpleroguelike.model.Direction;

/**
 * Un passo per allontanarsi dal player. Come {@link StepIntent}, ma il nemico se lo ricorda: subito
 * dopo non può scappare di nuovo.
 */
public record RetreatIntent(Direction direction) implements EnemyIntent {}
