package com.reborn.tasks;

public interface ITaskExecutor {
    void runTask(final Runnable task);
    void postback(final Runnable postback);
}
