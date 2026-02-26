package com.uzenjitrust.support.integrity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class ConcurrencyTestHelper {

    private ConcurrencyTestHelper() {
    }

    public static <T> List<Result<T>> runConcurrently(int threads, Callable<T> work) {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Result<T>>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) {
                    return Result.failure(new IllegalStateException("Timed out waiting for concurrent start"));
                }
                try {
                    return Result.success(work.call());
                } catch (Throwable t) {
                    return Result.failure(t);
                }
            }));
        }

        try {
            if (!ready.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Workers did not become ready");
            }
            start.countDown();

            List<Result<T>> results = new ArrayList<>();
            for (Future<Result<T>> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        } catch (Exception ex) {
            throw new RuntimeException("Concurrent execution failed", ex);
        } finally {
            executor.shutdownNow();
        }
    }

    public record Result<T>(T value, Throwable error) {
        public static <T> Result<T> success(T value) {
            return new Result<>(value, null);
        }

        public static <T> Result<T> failure(Throwable error) {
            return new Result<>(null, error);
        }

        public boolean isSuccess() {
            return error == null;
        }
    }
}
