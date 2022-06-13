package com.reborn.tasks.exceptions;

public class TaskException extends RuntimeException {
    public TaskException(final Throwable throwable) {
        super(throwable);
    }
}
