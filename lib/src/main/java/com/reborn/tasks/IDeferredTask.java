package com.reborn.tasks;

public interface IDeferredTask extends ITask {
    void setSucceeded();
    void setErrored(final Throwable throwable);
}
