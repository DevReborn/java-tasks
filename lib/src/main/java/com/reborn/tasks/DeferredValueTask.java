package com.reborn.tasks;

import com.reborn.tasks.common.ICancelable;

import java.util.function.Function;

public class DeferredValueTask<T> extends BaseTask<T> implements IDeferredValueTask<T> {
    private final Function<IDeferredValueTask<T>, ICancelable> _callable;
    private T _result;
    private boolean _resultSet;
    private ICancelable _cancelable;

    public DeferredValueTask(final Function<IDeferredValueTask<T>, ICancelable> callable,
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
            if (_callable != null) {
                _cancelable = _callable.apply(this);
            }
        }
        return this;
    }

    @Override
    public void cancel() {
        super.cancel();
        if(_cancelable != null)
            _cancelable.cancel();
    }
}
