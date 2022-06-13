package com.reborn.tasks;

import com.reborn.tasks.common.Pair;
import com.reborn.tasks.common.ThrowingFunction;
import com.reborn.tasks.exceptions.TaskException;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class ValueTask<T> implements IValueTask<T> {
    private final ITaskExecutor _taskExecutor;

    protected T result;
    protected boolean resultSet;
    private TaskState _state = TaskState.NOT_EXECUTED;
    private Throwable _exception;

    private Runnable _preExecute;
    private ThrowingFunction<ITaskOperator, T> _callable;
    private Consumer<Object> _onUpdate;
    private Consumer<T> _onSuccess;
    private Function<Throwable, Boolean> _onError;
    private BiConsumer<T, Boolean> _onComplete;

    public ValueTask(final ThrowingFunction<ITaskOperator, T> callable,
                     final ITaskExecutor taskExecutor) {
        _callable = callable;
        _taskExecutor = taskExecutor;
    }
    public ValueTask(final T result,
                     final ITaskExecutor taskExecutor) {
        checkForValidState("set result on");
        this.result = result;
        resultSet = true;
        _taskExecutor = taskExecutor;
    }

    protected void post(final Runnable postback) {
        postback.run();
    }
    protected void execute(final Runnable task) {
        task.run();
    }
    protected ITaskOperator createTaskOperator(final Consumer<Object> onUpdate) {
        return new TaskOperator(onUpdate, this, _taskExecutor);
    }

    @Override
    public T getResult() {
        if(resultSet)
            return result;
        execute();
        return result;
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
    public void execute() {
        checkForValidState("execute");

        _state = TaskState.EXECUTING;
        if(resultSet) {
            if(_preExecute != null) _preExecute.run();
            onResultSucceeded();
        } else {
            if(_preExecute != null) _preExecute.run();
            execute(this::runOnThread);
        }
    }

    @Override
    public TaskState getState() {
        return _state;
    }
    @Override
    public Throwable getException() {
        return _exception;
    }

    @Override
    public ITask onExecuted(final Runnable onExecuted) {
        checkForValidState("alter");
        if (_preExecute == null) _preExecute = onExecuted;
        else {
            final Runnable oldPreExecute = _preExecute;
            _preExecute = () -> {
                oldPreExecute.run();
                onExecuted.run();
            };
        }
        return this;
    }

    private void checkForValidState(final String action) {
        if(_state == TaskState.EXECUTING)
            throw new IllegalStateException("Cannot " + action + " an already running task");
        if(_state == TaskState.CANCELED)
            throw new IllegalStateException("Cannot " + action + " an task that has been canceled");
        if(_state == TaskState.ERRORED || _state == TaskState.SUCCEEDED)
            throw new IllegalStateException("Cannot " + action + " an task that has already run.");
    }

    private void runOnThread() {
        try {
            result = _callable.apply(createTaskOperator(_onUpdate));
            resultSet = true;
            onResultSucceeded();
        } catch (final Exception e) {
            onResultErrored(e);
        }
    }

    private void onResultSucceeded() {
        if(_onSuccess != null || _onComplete != null) {
            post(() -> {
                _state = TaskState.SUCCEEDED;
                if(_onSuccess != null) _onSuccess.accept(result);
                if(_onComplete != null) _onComplete.accept(result, true);
            });
        } else {
            _state = TaskState.SUCCEEDED;
        }
    }

    private void onResultErrored(final Exception e) {
        _exception = e;
        if(_onError != null || _onComplete != null) {
            post(() -> {
                _state = TaskState.ERRORED;
                if(_onError != null) {
                    final Boolean swallowException = _onError.apply(e);
                    if(!swallowException) {
                        throw new TaskException(e);
                    }
                }
                if(_onComplete != null) _onComplete.accept(null, false);
            });
        } else {
            _state = TaskState.ERRORED;
            throw new TaskException(e);
        }
    }
}
