package com.reborn.tasks;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TaskExecutor implements ITaskExecutor {
    private static final Executor _executor
            = Executors.newSingleThreadExecutor(new ThreadFactoryWithStackSize(8 * 1024 * 1000));

    private static TaskExecutor _instance;

    public static ITaskExecutor getSingleton() {
        return _instance == null ? _instance = new TaskExecutor() : _instance;
    }

    @Override
    public void runTask(final Runnable task) {
        _executor.execute(task);
    }

    @Override
    public void postback(final Runnable postback) {
        postback.run();
    }
}
