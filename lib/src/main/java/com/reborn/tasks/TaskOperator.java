package com.reborn.tasks;

import java.util.function.Consumer;

public class TaskOperator implements ITaskOperator {
    private final Consumer<Object> _onUpdate;
    private final ITask _resultTask;
    private final ITaskExecutor _executor;

    public TaskOperator(final Consumer<Object> onUpdate,
                        final ITask resultTask,
                        final ITaskExecutor executor) {
        _onUpdate = onUpdate;
        _resultTask = resultTask;
        _executor = executor;
    }

    @Override
    public void update(final Object update) {
        if (_onUpdate == null)
            return;
        post(() -> _onUpdate.accept(update));
    }

    protected void post(final Runnable update) {
        _executor.postback(update);
    }

    @Override
    public boolean isCanceled() {
        return _resultTask.getState() == TaskState.CANCELED;
    }
}
