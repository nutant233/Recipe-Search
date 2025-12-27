package com.fast.recipesearch;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;

public class RecipeSearcher<R> implements Iterator<R>, Iterable<R> {

    Function<R, R> mapFunction;

    Node<R> node;
    int count;

    private R next;
    private boolean hasNext;
    int maxDepth;
    int depth;
    IntLongMap map;
    int[] ints;
    SearchFrame<R>[] frames;
    private Iterator<R> fallback;

    @SuppressWarnings("unchecked")
    public RecipeSearcher(int expectedDepth) {
        this.maxDepth = expectedDepth;
        frames = new SearchFrame[expectedDepth];
        for (int i = 0; i < expectedDepth; i++) {
            frames[i] = new SearchFrame<>();
        }
    }

    @SuppressWarnings("unchecked")
    public RecipeSearcher(int expectedDepth, Branch<R> branch, IntLongMap map, int[] ints, Function<R, R> mapFunction, Iterator<R> fallback) {
        this.maxDepth = expectedDepth;
        frames = new SearchFrame[expectedDepth];
        for (int i = 0; i < expectedDepth; i++) {
            frames[i] = new SearchFrame<>();
        }
        this.map = map;
        this.ints = ints;
        this.mapFunction = mapFunction;
        this.fallback = fallback;
        var length = ints.length;
        frames[0].push(branch, length, 0);
        hasNext = length > 0;
    }

    public RecipeSearcher() {
        this(1);
    }

    public void reset(Branch<R> branch, IntLongMap map, int[] ints, Function<R, R> mapFunction, Iterator<R> fallback) {
        this.map = map;
        this.ints = ints;
        this.mapFunction = mapFunction;
        this.fallback = fallback;
        node = null;
        count = 0;
        depth = 0;
        var length = ints.length;
        frames[0].push(branch, length, 0);
        hasNext = length > 0;
    }

    public R findAny() {
        R r;
        while (depth >= 0) {
            var frame = this.frames[depth];
            if (frame.branch == null) {
                r = frame.b(this, depth);
            } else {
                r = frame.a(this, depth);
            }
            if (r != null) return r;
        }
        return null;
    }

    @Override
    public boolean hasNext() {
        if (hasNext) {
            while (node != null) {
                next = node.get(this, null);
                if (next != null) {
                    return true;
                }
            }

            while (depth >= 0) {
                var frame = this.frames[depth];
                if (frame.branch == null) {
                    next = frame.b(this, depth);
                } else {
                    next = frame.a(this, depth);
                }
                if (next != null) return true;
            }

            if (fallback != null && fallback.hasNext()) {
                next = fallback.next();
                return true;
            }
            hasNext = false;
        }
        return false;
    }

    @Override
    public R next() {
        return next;
    }

    @Override
    public Iterator<R> iterator() {
        return this;
    }

    @Override
    public void forEach(Consumer<? super R> action) {
        while (hasNext()) {
            action.accept(next);
        }
    }

    @Override
    public Spliterator<R> spliterator() {
        return Spliterators.spliteratorUnknownSize(this, 0);
    }
}
