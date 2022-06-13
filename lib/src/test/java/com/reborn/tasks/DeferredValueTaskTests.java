package com.reborn.tasks;

import com.reborn.tasks.common.Cancel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DeferredValueTaskTests {
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
    public void settingSucceededFinishesTaskWithResult() {
        // Arrange
        final ArrayList<String> messages = new ArrayList<>();
        final IValueTask<String> task = new DeferredValueTask<String>(t -> {
            t.setSucceeded("Hello");
            return Cancel.empty;
        }, executor)
        .onSuccess(messages::add);

        // Act
        task.execute();

        // Assert
        Assertions.assertIterableEquals(Stream.of("Hello").collect(Collectors.toList()), messages);
    }

    @Test
    public void settingSucceeded_FromOutsideLambda_FinishesTaskWithResult() {
        // Arrange
        final ArrayList<String> messages = new ArrayList<>();
        final IDeferredValueTask<String> task = new DeferredValueTask<>(executor);
        task.onSuccess(messages::add);

        // Act
        task.execute();
        task.setSucceeded("Hello");

        // Assert
        Assertions.assertIterableEquals(Stream.of("Hello").collect(Collectors.toList()), messages);
    }
}