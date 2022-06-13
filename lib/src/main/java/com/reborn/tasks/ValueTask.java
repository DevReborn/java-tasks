package com.reborn.tasks;

import com.reborn.tasks.common.ICancelable;
import com.reborn.tasks.common.ThrowingFunction;

import java.util.function.Consumer;

public class ValueTask<T> extends BaseTask<T> {
    private final ITaskExecutor _taskExecutor;

    private T _result;
    private boolean _resultSet;
    private ThrowingFunction<ITaskOperator, T> _callable;

    public ValueTask(final ThrowingFunction<ITaskOperator, T> callable,
                     final ITaskExecutor taskExecutor) {
        super(taskExecutor);
        _callable = callable;
        _taskExecutor = taskExecutor;
    }
    public ValueTask(final T result,
                     final ITaskExecutor taskExecutor) {
        super(taskExecutor);
        checkForValidState("set result on");
        _result = result;
        _resultSet = true;
        _taskExecutor = taskExecutor;
    }

    protected ITaskOperator createTaskOperator(final Consumer<Object> onUpdate) {
        return new TaskOperator(onUpdate, this, _taskExecutor);
    }

    @Override
    public T getResult() {
        if(_resultSet)
            return _result;
        execute();
        return _result;
    }

    @Override
    public ICancelable execute() {
        checkForValidState("execute");

        _state = TaskState.EXECUTING;
        if(_resultSet) {
            if(_preExecute != null) _preExecute.run();
            onResultSucceeded(_result);
        } else {
            if(_preExecute != null) _preExecute.run();
            _taskExecutor.runTask(this::runOnThread);
        }
        return this;
    }

    private void runOnThread() {
        try {
            final ITaskOperator taskOperator = createTaskOperator(_onUpdate);
            _result = _callable.apply(taskOperator);
            _resultSet = true;
            onResultSucceeded(_result);
        } catch (final Exception e) {
            onResultErrored(e);
        }
    }
}
