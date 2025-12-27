package com.fast.recipesearch;

public final class SearchFrame<R> {

    int index;
    long skip;

    Branch<R> branch;

    private int[] key;
    private Node<R>[] value;

    void push(Branch<R> branch, int size, long skip) {
        this.index = 0;
        this.skip = skip;
        if (size > branch.size()) {
            this.branch = null;
            this.key = branch.key();
            this.value = branch.value();
        } else {
            this.branch = branch;
        }
    }

    R a(RecipeSearcher<R> searcher, int depth) {
        final int[] ints = searcher.ints;
        final int size = ints.length;
        final long skip = this.skip;
        final var branch = this.branch;
        while (index < size) {
            if ((skip & (1L << index)) == 0) {
                var node = branch.get(ints[index]);
                if (node != null) {
                    R result = node.get(searcher, this);
                    index++;
                    if (result != null) {
                        return result;
                    } else if (searcher.depth != depth) {
                        return null;
                    }
                    continue;
                }
            }
            index++;
        }
        searcher.depth--;
        return null;
    }

    R b(RecipeSearcher<R> searcher, int depth) {
        final var map = searcher.map;
        final int[] key = this.key;
        final Node<R>[] value = this.value;
        final int size = key.length;
        int index;
        while ((index = this.index++) < size) {
            if (map.containsKey(key[index])) {
                var n = value[index].get(searcher, this);
                if (n != null) return n;
                else if (searcher.depth != depth) return null;
            }
        }
        searcher.depth--;
        return null;
    }

}
