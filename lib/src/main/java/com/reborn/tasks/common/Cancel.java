package com.reborn.tasks.common;

public class Cancel {
    public static final ICancelable empty = new ICancelable() {
        @Override
        public boolean isCanceled() {
            return true;
        }
        @Override
        public void cancel() {

        }
    };
}
