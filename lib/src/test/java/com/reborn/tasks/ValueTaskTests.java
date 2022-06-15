package com.reborn.tasks;

import com.reborn.tasks.exceptions.TaskException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ValueTaskTests {
    private static final ITaskExecutor executor = new ITaskExecutor() {
        @Override
        public void runTask(final Runnable task) {
            task.run();
        }

        @Override
        public void postback(final Runnable postback) {
            postback.run();
        }
    };

    @Test
    public void orderOfEvents() {
        // Arrange
        final ArrayList<Integer> messages = new ArrayList<>();
        final ITask task = new ValueTask<>(op -> {
            messages.add(1);
            return null;
        }, executor)
        .onExecute(() -> messages.add(0))
        .onSuccess(() -> messages.add(2))
        .onError(throwable -> {messages.add(3);})
        .onComplete(() -> messages.add(4));

        // Act
        task.execute();

        // Assert
        Assertions.assertIterableEquals(Stream.of(0, 1, 2, 4).collect(Collectors.toList()), messages);
    }

    @Test
    public void orderOfEvents_catchesException() {
        // Arrange
        final ArrayList<Integer> messages = new ArrayList<>();
        final ITask task = new ValueTask<>(op -> {
            throw new Exception("hello!");
        }, executor)
        .onExecute(() -> messages.add(0))
        .onError(throwable -> {messages.add(1);})
        .onSuccess(() -> messages.add(2))
        .onComplete(throwable -> messages.add(3));

        // Act
        task.execute();

        // Assert
        Assertions.assertIterableEquals(Stream.of(0, 1, 3).collect(Collectors.toList()), messages);
    }

    @Test
    public void updateCalled_withObject() {
        // Arrange
        final ArrayList<String> messages = new ArrayList<>();
        final ITask task = new ValueTask<>(op -> {
            op.update("hello!");
            return null;
        }, executor)
        .onUpdate(s -> messages.add((String) s));

        // Act
        task.execute();

        // Assert
        Assertions.assertIterableEquals(Stream.of("hello!").collect(Collectors.toList()), messages);
    }

    @Test
    public void updateCalled_withFloatAndString() {
        // Arrange
        final ArrayList<String> messages = new ArrayList<>();
        final ITask task = new ValueTask<>(op -> {
            op.update(0.5f, "hello!");
            return null;
        }, executor)
        .onUpdate((f, s) -> messages.add(f + " " + s));

        // Act
        task.execute();

        // Assert
        Assertions.assertIterableEquals(Stream.of("0.5 hello!").collect(Collectors.toList()), messages);
    }

    @Test
    public void updateCalled_withObject_ThrowsWhenTryingToAccessViaFloatAndString() {
        // Arrange
        final ArrayList<String> messages = new ArrayList<>();
        final ITask task = new ValueTask<>(op -> {
            op.update("update the things!");
            return null;
        }, executor)
        .onUpdate((f, s) -> messages.add(f + " " + s));

        // Act
        // Assert
        final TaskException exception = Assertions.assertThrows(TaskException.class, task::execute);
        Assertions.assertInstanceOf(IllegalStateException.class, exception.getCause());
    }
}