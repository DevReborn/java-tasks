package com.reborn.tasks;

import com.reborn.tasks.common.ICancelable;
import com.reborn.tasks.common.ThrowingFunction;

public class ValueTask<T> extends BaseTask<T> {
    private final ITaskExecutor _taskExecutor;

    private ThrowingFunction<ITaskOperator, T> _callable;

    public ValueTask(final ThrowingFunction<ITaskOperator, T> callable,
                     final ITaskExecutor taskExecutor) {
        _callable = callable;
        _taskExecutor = taskExecutor;
    }
    public ValueTask(final T result,
                     final ITaskExecutor taskExecutor) {
        checkForValidState("set result on");
        _result = result;
        _resultSet = true;
        _taskExecutor = taskExecutor;
    }

    @Override
    public ICancelable execute(final ITaskExecutor executor) {
        checkForValidState("execute");

        final ITaskExecutor executorToUse = executor == null ? _taskExecutor : executor;

        _state = TaskState.EXECUTING;
        if(_resultSet) {
            if(_preExecute != null) _preExecute.run();
            onResultSucceeded(_result, executorToUse);
        } else {
            if(_preExecute != null) _preExecute.run();
            executorToUse.runTask(() -> runOnThread(executorToUse));
        }
        return this;
    }

    private void runOnThread(final ITaskExecutor executor) {
        try {
            final ITaskOperator taskOperator = new TaskOperator(_onUpdate, this, executor);
            _result = _callable.apply(taskOperator);
            _resultSet = true;
            onResultSucceeded(_result, executor);
        } catch (final Exception e) {
            onResultErrored(e, executor);
        }
    }
}
