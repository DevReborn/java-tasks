package com.reborn.tasks;

import com.reborn.tasks.common.ICancelable;
import com.reborn.tasks.common.ThrowingConsumer;
import com.reborn.tasks.common.ThrowingFunction;

import java.util.function.Function;

public interface ITaskFactory {
    ITask create(final ThrowingConsumer<ITaskOperator> consumer);
    <T> IValueTask<T> create(final ThrowingFunction<ITaskOperator, T> function);
    <T> IValueTask<T> fromResult(final T result);
    <T> IDeferredValueTask<T> createDeferred(final Function<IDeferredValueTask<T>, ICancelable> function);
}
