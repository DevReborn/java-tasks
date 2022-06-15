package com.reborn.tasks.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompoundCancelableTests {
    @Test
    public void canAddCancelablesToCompoundCancelable() {
        // Arrange
        final CompoundCancelable cancelable = new CompoundCancelable();
        final ArrayList<Integer> messages = new ArrayList<>();
        cancelable.add(new ICancelable() {
            @Override
            public boolean isCanceled() {
                return false;
            }
            @Override
            public void cancel() {
                messages.add(0);
            }
        });
        cancelable.add(new ICancelable() {
            @Override
            public boolean isCanceled() {
                return false;
            }
            @Override
            public void cancel() {
                messages.add(1);
            }
        });

        // Act
        cancelable.cancel();

        // Assert
        Assertions.assertIterableEquals(Stream.of(0, 1).collect(Collectors.toList()), messages);
    }
}
