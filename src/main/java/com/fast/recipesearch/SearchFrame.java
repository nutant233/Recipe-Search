package com.fast.recipesearch;

public final class SearchFrame<R> {

    boolean branchProbe;
    int index;
    long skip;

    Branch<R> branch;

    void push(Branch<R> branch, int size, long skip) {
        this.branchProbe = size > branch.size();
        this.index = 0;
        this.skip = skip;
        this.branch = branch;
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
                    if (result != null || searcher.depth != depth) return result;
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
        final int[] key = branch.key();
        final Node<R>[] value = branch.value();
        final int size = key.length;
        int index;
        while ((index = this.index++) < size) {
            if (map.containsKey(key[index])) {
                R result = value[index].get(searcher, this);
                if (result != null || searcher.depth != depth) return result;
            }
        }
        searcher.depth--;
        return null;
    }

}
