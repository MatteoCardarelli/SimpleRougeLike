package io.github.matteocardarelli.simpleroguelike.model;

/**
 * Il nome della strategia con cui un tipo di nemico decide cosa fare, per esempio {@code
 * chase_or_wander}.
 */
public record BehaviorKey(String value) implements Comparable<BehaviorKey> {

    public BehaviorKey {
        Keys.requireSnakeCase(value, "Una BehaviorKey");
    }

    public static BehaviorKey of(String value) {
        return new BehaviorKey(value);
    }

    @Override
    public int compareTo(BehaviorKey other) {
        return value.compareTo(other.value);
    }
}
