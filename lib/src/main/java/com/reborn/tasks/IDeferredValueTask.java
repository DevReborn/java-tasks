package com.reborn.tasks;

public interface IDeferredValueTask<T> extends IValueTask<T>, IDeferredTask {
    void setSucceeded(final T result);
    default void setSucceeded() {
        setSucceeded(null);
    }
}
