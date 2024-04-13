package dev.redicloud.libloader.boot;

public class Pair<F, S> {
    private final F first;
    private final S second;

    public Pair(final F first, final S second) {
        this.first = first;
        this.second = second;
    }

    public F first() {
        return this.first;
    }

    public S second() {
        return this.second;
    }
}
