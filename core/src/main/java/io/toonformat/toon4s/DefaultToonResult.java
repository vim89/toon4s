package io.toonformat.toon4s;

final class DefaultToonResult<Error, T> implements ToonResult<Error, T> {
    private final boolean success;
    private final T value;
    private final Error error;

    public DefaultToonResult(boolean success, T value, Error error) {
        this.success = success;
        this.value = value;
        this.error = error;
    }

    @Override
    public boolean isSuccess() {
        return success;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public Error getError() {
        return error;
    }
}
