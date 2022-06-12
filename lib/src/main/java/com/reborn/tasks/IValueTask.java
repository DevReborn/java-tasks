package com.reborn.tasks;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public interface IValueTask<T> extends ITask {
    IValueTask<T> onSuccess(final Consumer<T> consumer);

    @Override
    default ITask onSuccess(final Runnable onSuccess) {
        return onSuccess(x -> onSuccess.run());
    }
    T getResult();
    IValueTask<T> onComplete(final BiConsumer<T, Boolean> onComplete);

    @Override
    default ITask onComplete(final Consumer<Boolean> onComplete) {
        return onComplete((r, s) -> onComplete.accept(s));
    }
}
