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
    private int[] ints;
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
    public RecipeSearcher(int expectedDepth, Branch<R> branch, int[] ints, Function<R, R> mapFunction, Iterator<R> fallback) {
        this.maxDepth = expectedDepth;
        frames = new SearchFrame[expectedDepth];
        for (int i = 0; i < expectedDepth; i++) {
            frames[i] = new SearchFrame<>();
        }
        this.ints = ints;
        this.mapFunction = mapFunction;
        this.fallback = fallback;
        hasNext = ints.length > 0;
        frames[0].branch = branch;
    }

    public RecipeSearcher() {
        this(1);
    }

    public void reset(Branch<R> branch, int[] ints, Function<R, R> mapFunction, Iterator<R> fallback) {
        this.ints = ints;
        this.mapFunction = mapFunction;
        this.fallback = fallback;
        hasNext = ints.length > 0;
        node = null;
        count = 0;
        depth = 0;
        var frame = frames[0];
        frame.branch = branch;
        frame.index = 0;
        frame.skip = 0;
    }

    public R findAny() {
        final int[] ints = this.ints;
        final int length = ints.length;
        SearchFrame<R> frame;
        int index;
        Node<R> node;
        R r;

        while (depth >= 0) {
            frame = frames[depth];
            while ((index = frame.index) < length) {
                if ((frame.skip & (1L << index)) == 0) {
                    node = frame.branch.get(ints[index]);
                    if (node != null) {
                        r = node.get(this, frame);
                        frame.index = index + 1;
                        if (r != null) {
                            return r;
                        }
                        frame = frames[depth];
                        continue;
                    }
                }
                frame.index = index + 1;
            }
            depth--;
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

            final int[] ints = this.ints;
            final int length = ints.length;
            SearchFrame<R> frame;
            int index;
            Node<R> node;

            while (depth >= 0) {
                frame = frames[depth];
                while ((index = frame.index) < length) {
                    if ((frame.skip & (1L << index)) == 0) {
                        node = frame.branch.get(ints[index]);
                        if (node != null) {
                            next = node.get(this, frame);
                            frame.index = index + 1;
                            if (next != null) {
                                return true;
                            }
                            frame = frames[depth];
                            continue;
                        }
                    }
                    frame.index = index + 1;
                }
                depth--;
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
