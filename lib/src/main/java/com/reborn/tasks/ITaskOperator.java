package com.reborn.tasks;

import com.reborn.tasks.common.Pair;

public interface ITaskOperator {
    void update(Object update);
    default void update(final float progress, final String message) {
        update(new Pair<>(progress, message));
    }
    boolean isCanceled();
}
