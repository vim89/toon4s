package io.toonformat.toon4s;


public interface ToonResult<Error, T> {
    boolean isSuccess();

    T getValue();

    Error getError();

    static <Error, T> ToonResult<Error, T> success(T value) {
        return new DefaultToonResult<>(true, value, null);
    }

    static <Error, T> ToonResult<Error, T> failure(Error error) {
        return new DefaultToonResult<>(false, null, error);
    }
}