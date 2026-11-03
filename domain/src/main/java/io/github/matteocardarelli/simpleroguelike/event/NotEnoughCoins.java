package io.github.matteocardarelli.simpleroguelike.event;

import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;

/** Il player ha provato a pagare, ma le monete non bastano. */
public record NotEnoughCoins(int bill, int coins, Position at) implements GameEvent {

    @Override
    public List<Position> where() {
        return List.of(at);
    }
}
