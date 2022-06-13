package com.reborn.tasks.common;

import java.util.Arrays;
import java.util.List;

public class CompoundCancelable implements ICancelable {
    private final List<ICancelable> _cancelables;
    private boolean _isCanceled;

    public CompoundCancelable(final ICancelable... cancelables) {
        _cancelables = Arrays.asList(cancelables);
    }

    public void add(final ICancelable cancelable) {
        _cancelables.add(cancelable);
    }

    @Override
    public boolean isCanceled() {
        return _isCanceled;
    }

    @Override
    public void cancel() {
        _isCanceled = true;
        for (final ICancelable cancelable : _cancelables) {
            cancelable.cancel();
        }
    }
}
