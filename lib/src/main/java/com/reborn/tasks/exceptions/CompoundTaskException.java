package com.reborn.tasks.exceptions;

import java.util.List;
import java.util.stream.Collectors;

public class CompoundTaskException extends RuntimeException {
    private final List<Throwable> _errors;

    public CompoundTaskException(final List<Throwable> errors) {
        super("Exception(s) occurred whilst trying process task:\n\n" + errors.stream()
                .map(Throwable::toString)
                .collect(Collectors.joining("\n\n")));
        _errors = errors;
    }

    public List<Throwable> getErrors() {
        return _errors;
    }
}
