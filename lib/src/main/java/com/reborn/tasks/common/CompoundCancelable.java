package com.reborn.tasks.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CompoundCancelable extends Cancel {
    private final List<ICancelable> _cancelables;

    public CompoundCancelable(final ICancelable... cancelables) {
        _cancelables = new ArrayList<>();
        Collections.addAll(_cancelables, cancelables);
    }

    public void add(final ICancelable cancelable) {
        _cancelables.add(cancelable);
    }

    @Override
    protected void onCanceled() {
        for (final ICancelable cancelable : _cancelables) {
            cancelable.cancel();
        }
    }
}
