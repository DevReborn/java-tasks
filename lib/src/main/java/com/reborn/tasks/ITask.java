package com.reborn.tasks;

import com.reborn.tasks.common.Pair;

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
    default ITask onUpdate(final BiConsumer<Float, String> onUpdate) {
        return onUpdate(o -> {
            if(o instanceof Pair) {
                final Pair pair = (Pair) o;
                if(pair.first instanceof Float && pair.second instanceof String) {
                    onUpdate.accept((Float)pair.first, (String)pair.second);
                } else {
                    final String firstName = pair.first.getClass().getSimpleName();
                    final String secondName = pair.second.getClass().getSimpleName();
                    throw new IllegalStateException("update object was a Pair<"
                            + firstName + ", " + secondName + "> and not of type Pair<Float, String>");
                }
            } else {
                final String simpleName = o.getClass().getSimpleName();
                throw new IllegalStateException("update object was a "
                        + simpleName + " and not of type Pair<Float, String>");
            }
        });
    }

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
