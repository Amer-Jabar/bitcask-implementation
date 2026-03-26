package org.bitcask.infra;

import java.util.function.Function;

public final class Utils {
    public static <T, R> Function<T, R> tryFunction(ThrowingFunction<T, R, Exception> function) {
        return t -> {
            try {
                return function.apply(t);
            } catch (Exception e) {
                throw new RuntimeException("Something went wrong");
            }
        };
    }

    @FunctionalInterface
    public interface ThrowingFunction<T, R, E extends Exception> {
        R apply(T target) throws E;
    }
}
