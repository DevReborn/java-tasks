package com.reborn.tasks;

import com.reborn.tasks.exceptions.TaskException;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class BaseTask<T> implements IValueTask<T> {
    protected T _result;
    protected boolean _resultSet;
    protected Throwable _exception;

    protected TaskState _state = TaskState.NOT_EXECUTED;
    protected Runnable _preExecute;
    protected Consumer<T> _onSuccess;
    protected Consumer<Object> _onUpdate;
    protected BiConsumer<T, Boolean> _onComplete;
    protected Function<Throwable, Boolean> _onError;

    @Override
    public T getResult(final ITaskExecutor executor) {
        if(_resultSet)
            return _result;
        if(_exception != null && _state == TaskState.ERRORED)
            throw new IllegalStateException("Cannot get result of task that has errored");
        throw new IllegalStateException("Cannot get result of task before its completed");
    }

    @Override
    public ITask onExecute(final Runnable onExecute) {
        checkForValidState("alter");
        if (_preExecute == null) _preExecute = onExecute;
        else {
            final Runnable oldPreExecute = _preExecute;
            _preExecute = () -> {
                oldPreExecute.run();
                onExecute.run();
            };
        }
        return this;
    }

    @Override
    public IValueTask<T> onSuccess(final Consumer<T> onSuccess) {
        checkForValidState("alter");
        if (_onSuccess == null) _onSuccess = onSuccess;
        else _onSuccess = _onSuccess.andThen(onSuccess);
        return this;
    }

    @Override
    public ITask onUpdate(final Consumer<Object> onUpdate) {
        checkForValidState("alter");
        if (_onUpdate == null) _onUpdate = onUpdate;
        else _onUpdate = _onUpdate.andThen(onUpdate);
        return this;
    }

    @Override
    public IValueTask<T> onComplete(final BiConsumer<T, Boolean> onComplete) {
        checkForValidState("alter");
        if (_onComplete == null) _onComplete = onComplete;
        else _onComplete = _onComplete.andThen(onComplete);
        return this;
    }

    @Override
    public ITask onError(final Function<Throwable, Boolean> onError) {
        checkForValidState("alter");
        if (_onError == null) _onError = onError;
        else {
            final Function<Throwable, Boolean> oldOnError = _onError;
            _onError = throwable -> {
                oldOnError.apply(throwable);
                return onError.apply(throwable);
            };
        }
        return this;
    }

    @Override
    public TaskState getState() {
        return _state;
    }
    @Override
    public Throwable getException() {
        return _exception;
    }

    protected void checkForValidState(final String action) {
        if(_state == TaskState.EXECUTING)
            throw new IllegalStateException("Cannot " + action + " a already running task");
        if(_state == TaskState.CANCELED)
            throw new IllegalStateException("Cannot " + action + " a task that has been canceled");
        if(_state == TaskState.ERRORED || _state == TaskState.SUCCEEDED)
            throw new IllegalStateException("Cannot " + action + " a task that has already run.");
    }

    protected void onResultSucceeded(final T result, final ITaskExecutor executor) {
        if(_onSuccess != null || _onComplete != null) {
            executor.postback(() -> {
                _state = TaskState.SUCCEEDED;
                if(_onSuccess != null) _onSuccess.accept(result);
                if(_onComplete != null) _onComplete.accept(result, true);
            });
        } else {
            _state = TaskState.SUCCEEDED;
        }
    }

    protected void onResultErrored(final Throwable throwable, final ITaskExecutor executor) {
        _exception = throwable;
        if(_onError != null || _onComplete != null) {
            executor.postback(() -> {
                _state = TaskState.ERRORED;
                if(_onError != null) {
                    final Boolean swallowException = _onError.apply(throwable);
                    if(!swallowException) {
                        throw new TaskException(throwable);
                    }
                }
                if(_onComplete != null) _onComplete.accept(null, false);
            });
        } else {
            _state = TaskState.ERRORED;
            throw new TaskException(throwable);
        }
    }

    @Override
    public boolean isCanceled() {
        return _state == TaskState.CANCELED;
    }

    @Override
    public void cancel() {
        _state = TaskState.CANCELED;
    }
}
