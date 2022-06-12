package com.reborn.tasks;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ITask {
    void execute();
    TaskState getState();
    Throwable getException();

    ITask onExecuted(final Runnable onExecuted);

    ITask onSuccess(final Runnable onSuccess);

    ITask onUpdate(final Consumer<Object> onUpdate);
    ITask onUpdate(final BiConsumer<Float, String> onUpdate);

    ITask onComplete(final Consumer<Boolean> onComplete);
    default ITask onComplete(final Runnable onComplete) {
        return onComplete(r -> onComplete.run());
    }

    ITask onError(final Function<Throwable, Boolean> onError);
    default ITask onError(final Consumer<Throwable> onError) {
        return onError(t -> {
            onError.accept(t);
            return true;
        });
    }
}
