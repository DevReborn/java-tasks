package com.reborn.tasks;

import com.reborn.tasks.common.ICancelable;
import com.reborn.tasks.common.ThrowingConsumer;
import com.reborn.tasks.common.ThrowingFunction;

import java.util.function.Function;

public class TaskFactory implements ITaskFactory {
    private final ITaskExecutor _executor;

    public TaskFactory(final ITaskExecutor executor) {
        _executor = executor;
    }

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
    public <T> IDeferredValueTask<T> createDeferred(final Function<IDeferredValueTask<T>, ICancelable> consumer) {
        return new DeferredValueTask<>(consumer, _executor);
    }
}

