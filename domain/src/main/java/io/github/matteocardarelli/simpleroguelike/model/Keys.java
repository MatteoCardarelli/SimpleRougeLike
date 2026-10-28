package io.github.matteocardarelli.simpleroguelike.model;

import java.util.regex.Pattern;

/** Il formato comune delle chiavi: minuscolo, {@code snake_case}, non vuoto. */
enum Keys {
    ;

    private static final Pattern SNAKE_CASE = Pattern.compile("[a-z][a-z0-9]*(_[a-z0-9]+)*");

    static String requireSnakeCase(String value, String what) {
        if (!SNAKE_CASE.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    what + " deve essere minuscolo e in snake_case: \"" + value + "\"");
        }
        return value;
    }
}
