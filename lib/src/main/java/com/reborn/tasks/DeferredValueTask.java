package com.reborn.tasks;

import com.reborn.tasks.common.ICancelable;

import java.util.function.Function;

public class DeferredValueTask<T> extends BaseTask<T> implements IDeferredValueTask<T> {
    private final Function<IDeferredValueTask<T>, ICancelable> _callable;
    private final ITaskExecutor _defaultExecutor;

    private ICancelable _cancelable;
    private ITaskExecutor _executor;

    public DeferredValueTask(final Function<IDeferredValueTask<T>, ICancelable> callable,
                             final ITaskExecutor executor) {
        _callable = callable;
        _defaultExecutor = executor;
    }
    public DeferredValueTask(final ITaskExecutor executor) {
        _defaultExecutor = executor;
        _callable = null;
    }

    public void setSucceeded(final T result) {
        if(_resultSet)
            throw new IllegalStateException("cannot set success result more than once");

        _result = result;
        _resultSet = true;
        if(_state == TaskState.EXECUTING) {
            onResultSucceeded(result, _executor);
        }
    }

    public void setErrored(final Throwable throwable) {
        if(_exception != null)
            throw new IllegalStateException("cannot set error result more than once");

        _exception = throwable;
        if(_state == TaskState.EXECUTING) {
            onResultErrored(_exception, _executor);
        }
    }

    @Override
    public ICancelable execute(final ITaskExecutor executor) {
        checkForValidState("re-execute");

        _executor = executor == null ? _defaultExecutor : executor;

        _state = TaskState.EXECUTING;
        if(_resultSet) {
            if(_preExecute != null) _preExecute.run();
            onResultSucceeded(_result, _executor);
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
