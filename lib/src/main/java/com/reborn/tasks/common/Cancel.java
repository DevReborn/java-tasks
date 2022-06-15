package com.reborn.tasks.common;

public abstract class Cancel implements ICancelable {
    public static final ICancelable empty = new ICancelable() {
        @Override
        public boolean isCanceled() {
            return true;
        }
        @Override
        public void cancel() {

        }
    };
    private boolean _isCanceled;

    @Override
    public boolean isCanceled() {
        return _isCanceled;
    }

    @Override
    public void cancel() {
        onCanceled();
        _isCanceled = true;
    }

    protected abstract void onCanceled();
}
