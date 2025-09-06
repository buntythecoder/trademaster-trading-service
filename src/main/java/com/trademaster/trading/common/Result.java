package com.trademaster.trading.common;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Functional Result type for error handling without exceptions
 * 
 * Provides railway programming pattern with monadic operations
 * for safe error handling and composition.
 * 
 * @param <T> Success type
 * @param <E> Error type
 */
public sealed interface Result<T, E> permits Result.Success, Result.Failure {
    
    /**
     * Success variant containing the successful value
     */
    record Success<T, E>(T value) implements Result<T, E> {
        @Override
        public boolean isSuccess() { return true; }
        
        @Override
        public boolean isFailure() { return false; }
        
        @Override
        public Optional<T> getValue() { return Optional.of(value); }
        
        @Override
        public Optional<E> getError() { return Optional.empty(); }
    }
    
    /**
     * Failure variant containing the error
     */
    record Failure<T, E>(E error) implements Result<T, E> {
        @Override
        public boolean isSuccess() { return false; }
        
        @Override
        public boolean isFailure() { return true; }
        
        @Override
        public Optional<T> getValue() { return Optional.empty(); }
        
        @Override
        public Optional<E> getError() { return Optional.of(error); }
    }
    
    // Core query methods
    boolean isSuccess();
    boolean isFailure();
    Optional<T> getValue();
    Optional<E> getError();
    
    // Factory methods
    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }
    
    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }
    
    // Monadic operations
    default <U> Result<U, E> map(Function<T, U> mapper) {
        return switch (this) {
            case Success<T, E> success -> Result.success(mapper.apply(success.value()));
            case Failure<T, E> failure -> Result.failure(failure.error());
        };
    }
    
    default <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
        return switch (this) {
            case Success<T, E> success -> mapper.apply(success.value());
            case Failure<T, E> failure -> Result.failure(failure.error());
        };
    }
    
    default Result<T, E> filter(Predicate<T> predicate, Supplier<E> errorSupplier) {
        return switch (this) {
            case Success<T, E> success -> predicate.test(success.value()) 
                ? this 
                : Result.failure(errorSupplier.get());
            case Failure<T, E> failure -> this;
        };
    }
    
    // Recovery operations
    default Result<T, E> recover(Function<E, T> recovery) {
        return switch (this) {
            case Success<T, E> success -> this;
            case Failure<T, E> failure -> Result.success(recovery.apply(failure.error()));
        };
    }
    
    default Result<T, E> recoverWith(Function<E, Result<T, E>> recovery) {
        return switch (this) {
            case Success<T, E> success -> this;
            case Failure<T, E> failure -> recovery.apply(failure.error());
        };
    }
    
    // Side effects
    default Result<T, E> onSuccess(Consumer<T> action) {
        if (this instanceof Success<T, E> success) {
            action.accept(success.value());
        }
        return this;
    }
    
    default Result<T, E> onFailure(Consumer<E> action) {
        if (this instanceof Failure<T, E> failure) {
            action.accept(failure.error());
        }
        return this;
    }
    
    // Transformation
    default <F> Result<T, F> mapError(Function<E, F> mapper) {
        return switch (this) {
            case Success<T, E> success -> Result.success(success.value());
            case Failure<T, E> failure -> Result.failure(mapper.apply(failure.error()));
        };
    }
    
    // Extraction with defaults
    default T getOrElse(T defaultValue) {
        return switch (this) {
            case Success<T, E> success -> success.value();
            case Failure<T, E> failure -> defaultValue;
        };
    }
    
    default T getOrElse(Supplier<T> defaultSupplier) {
        return switch (this) {
            case Success<T, E> success -> success.value();
            case Failure<T, E> failure -> defaultSupplier.get();
        };
    }
    
    // Safe execution wrapper
    static <T> Result<T, Exception> safely(Supplier<T> operation) {
        try {
            return Result.success(operation.get());
        } catch (Exception e) {
            return Result.failure(e);
        }
    }
    
    // Combine multiple results
    static <T1, T2, R, E> Result<R, E> combine(
            Result<T1, E> result1,
            Result<T2, E> result2,
            Function<T1, Function<T2, R>> combiner) {
        
        return result1.flatMap(v1 -> 
            result2.map(v2 -> 
                combiner.apply(v1).apply(v2)));
    }
    
    static <T1, T2, T3, R, E> Result<R, E> combine(
            Result<T1, E> result1,
            Result<T2, E> result2,
            Result<T3, E> result3,
            Function<T1, Function<T2, Function<T3, R>>> combiner) {
        
        return result1.flatMap(v1 ->
            result2.flatMap(v2 ->
                result3.map(v3 ->
                    combiner.apply(v1).apply(v2).apply(v3))));
    }
}