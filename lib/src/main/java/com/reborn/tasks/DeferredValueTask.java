package com.reborn.tasks;

import com.reborn.tasks.common.Pair;
import com.reborn.tasks.exceptions.TaskException;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class DeferredValueTask<T> implements IDeferredValueTask<T> {
    private final Consumer<IDeferredValueTask<T>> _callable;
    private final ITaskExecutor _executor;

    protected T result;
    protected boolean resultSet;
    private Throwable _throwable;
    private TaskState _state = TaskState.NOT_EXECUTED;

    private Consumer<T> _onSuccess;
    private Function<Throwable, Boolean> _onError;
    private Consumer<Object> _onUpdate;
    private BiConsumer<T, Boolean> _onComplete;
    private Runnable _preExecute;

    public DeferredValueTask(final Consumer<IDeferredValueTask<T>> callable,
                             final ITaskExecutor executor) {
        _callable = callable;
        _executor = executor;
    }

    protected void post(final Runnable postback) {
        _executor.postback(postback);
    }
    protected void execute(final Runnable task) {
        _executor.runTask(task);
    }

    @Override
    public T getResult() {
        execute();
        return result;
    }

    public void setSucceeded(final T result) {
        if(resultSet)
            throw new IllegalStateException("cannot set success result more than once");

        this.result = result;
        resultSet = true;
        if(_state == TaskState.EXECUTING) {
            onResultSucceeded();
        }
    }

    public void setErrored(final Throwable throwable) {
        if(_throwable != null)
            throw new IllegalStateException("cannot set error result more than once");

        _throwable = throwable;
        if(_state == TaskState.EXECUTING) {
            onResultErrored();
        }
    }

    @Override
    public IValueTask<T> onSuccess(final Consumer<T> onSuccess) {
        checkForValidState("alter");
        if (_onSuccess == null) _onSuccess = onSuccess;
        else _onSuccess = _onSuccess.andThen(onSuccess);
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

    @Override
    public ITask onUpdate(final Consumer<Object> onUpdate) {
        checkForValidState("alter");
        if (_onUpdate == null) _onUpdate = onUpdate;
        else _onUpdate = _onUpdate.andThen(onUpdate);
        return this;
    }
    @Override
    public ITask onUpdate(final BiConsumer<Float, String> onUpdate) {
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
        checkForValidState("re-execute");

        _state = TaskState.EXECUTING;
        if(resultSet) {
            if(_preExecute != null) _preExecute.run();
            onResultSucceeded();
        } else {
            if (_preExecute != null) _preExecute.run();
            _callable.accept(this);
        }
    }

    @Override
    public TaskState getState() {
        return _state;
    }

    @Override
    public Throwable getException() {
        return _throwable;
    }

    private void checkForValidState(final String action) {
        if(_state == TaskState.EXECUTING)
            throw new IllegalStateException("Cannot " + action + " an already running task");
        if(_state == TaskState.CANCELED)
            throw new IllegalStateException("Cannot " + action + " an task that has been canceled");
        if(_state == TaskState.ERRORED || _state == TaskState.SUCCEEDED)
            throw new IllegalStateException("Cannot " + action + " an task that has already run.");
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

    private void onResultErrored() {
        if(_onError != null || _onComplete != null) {
            post(() -> {
                _state = TaskState.ERRORED;
                if(_onError != null) {
                    final Boolean swallowException = _onError.apply(_throwable);
                    if(!swallowException) {
                        throw new TaskException(_throwable);
                    }
                }
                if(_onComplete != null) _onComplete.accept(null, false);
            });
        } else {
            _state = TaskState.ERRORED;
            throw new TaskException(_throwable);
        }
    }
}
