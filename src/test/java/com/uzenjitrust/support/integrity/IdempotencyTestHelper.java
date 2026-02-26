package com.uzenjitrust.support.integrity;

import java.util.function.Supplier;

public final class IdempotencyTestHelper {

    private IdempotencyTestHelper() {
    }

    public static <T> Pair<T, T> runSameCommandTwice(Supplier<T> command) {
        T first = command.get();
        T second = command.get();
        return new Pair<>(first, second);
    }

    public record Pair<T, U>(T first, U second) {
    }
}
