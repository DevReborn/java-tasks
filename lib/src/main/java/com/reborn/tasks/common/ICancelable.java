package com.reborn.tasks.common;

public interface ICancelable {
    boolean isCanceled();
    void cancel();
}
