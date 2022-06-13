package com.reborn.tasks;

import com.reborn.tasks.common.ICancelable;

import java.util.function.Consumer;

public class DeferredValueTask<T> extends BaseTask<T> implements IDeferredValueTask<T> {
    private final Consumer<IDeferredValueTask<T>> _callable;
    private T _result;
    private boolean _resultSet;

    public DeferredValueTask(final Consumer<IDeferredValueTask<T>> callable,
                             final ITaskExecutor executor) {
        super(executor);
        _callable = callable;
    }
    public DeferredValueTask(final ITaskExecutor executor) {
        super(executor);
        _callable = null;
    }

    @Override
    public T getResult() {
        execute();
        return _result;
    }

    public void setSucceeded(final T result) {
        if(_resultSet)
            throw new IllegalStateException("cannot set success result more than once");

        _result = result;
        _resultSet = true;
        if(_state == TaskState.EXECUTING) {
            onResultSucceeded(result);
        }
    }

    public void setErrored(final Throwable throwable) {
        if(_exception != null)
            throw new IllegalStateException("cannot set error result more than once");

        _exception = throwable;
        if(_state == TaskState.EXECUTING) {
            onResultErrored(_exception);
        }
    }

    @Override
    public ICancelable execute() {
        checkForValidState("re-execute");

        _state = TaskState.EXECUTING;
        if(_resultSet) {
            if(_preExecute != null) _preExecute.run();
            onResultSucceeded(_result);
        } else {
            if (_preExecute != null) _preExecute.run();
            if (_callable != null) _callable.accept(this);
        }
        return this;
    }
}
