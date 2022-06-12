package com.reborn.tasks;

import com.reborn.tasks.common.ThrowingConsumer;
import com.reborn.tasks.common.ThrowingFunction;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class TaskFactory implements ITaskFactory {
    private static final ITaskExecutor _executor = new TaskExecutor();

    @Override
    public ITask create(final ThrowingConsumer<ITaskOperator> consumer) {
        return new ValueTask<>(op -> {
            consumer.accept(op);
            return null;
        }, _executor);
    }

    @Override
    public <T> IValueTask<T> create(final ThrowingFunction<ITaskOperator, T> function) {
        return new ValueTask<>(function, _executor);
    }

    @Override
    public <T> IValueTask<T> fromResult(final T result) {
        return new ValueTask<>(result, _executor);
    }

    @Override
    public <T> IDeferredValueTask<T> createDeferred(final Consumer<IDeferredValueTask<T>> consumer) {
        return new DeferredValueTask<>(consumer, _executor);
    }

    public static class TaskExecutor implements ITaskExecutor {
        private static final Executor _executor
                = Executors.newSingleThreadExecutor(new ThreadFactoryWithStackSize(8 * 1024 * 1000));

        @Override
        public void runTask(final Runnable task) {
            _executor.execute(task);
        }

        @Override
        public void postback(final Runnable postback) {
            postback.run();
        }
    }
}
