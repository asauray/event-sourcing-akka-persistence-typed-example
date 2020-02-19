package net.sauray.domain;

public class OffsetPromise {

    public final long offset;
    public final Runnable callback;

    public OffsetPromise(long offset, Runnable callback) {

        this.offset = offset;
        this.callback = callback;
    }
}
