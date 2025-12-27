package com.fast.recipesearch;

import java.util.Arrays;
import java.util.List;

public interface Node<T> {

    @SuppressWarnings("unchecked")
    static <T> Node<T> recipe(List<Runnable> branchBuilder, Node<T> node, final T value) {
        if (node instanceof BR<T> br) {
            var arr = (T[]) new Object[2];
            arr[0] = br.r;
            arr[1] = value;
            return new BMR<>(branchBuilder, br.b, arr);
        } else if (node instanceof BMR<T> bmr) {
            var arr = (T[]) new Object[bmr.rs.length + 1];
            System.arraycopy(bmr.rs, 0, arr, 0, bmr.rs.length);
            arr[bmr.rs.length] = value;
            return new BMR<>(branchBuilder, bmr.b, arr);
        } else if (node instanceof R<T> r) {
            var arr = (T[]) new Object[2];
            arr[0] = r.r;
            arr[1] = value;
            return new MR<>(arr);
        } else if (node instanceof MR<T> mr) {
            var arr = (T[]) new Object[mr.rs.length + 1];
            System.arraycopy(mr.rs, 0, arr, 0, mr.rs.length);
            arr[mr.rs.length] = value;
            return new MR<>(arr);
        } else if (node instanceof B<T> b) {
            return new BR<>(branchBuilder, b.b, value);
        } else {
            return new R<>(value);
        }
    }

    static <T> Node<T> branch(List<Runnable> branchBuilder, Node<T> node) {
        if (node instanceof B<T> || node instanceof BR<T> || node instanceof BMR<T>) {
            return node;
        } else if (node instanceof R<T> r) {
            return new BR<>(branchBuilder, Branch.create(), r.r);
        } else if (node instanceof MR<T> mr) {
            return new BMR<>(branchBuilder, Branch.create(), mr.rs);
        } else {
            return new B<>(branchBuilder);
        }
    }

    default T get(RecipeSearcher<T> context, SearchFrame<T> frame) {
        return null;
    }

    interface BranchNode<T> extends Node<T> {

        Branch<T> branch();

        @Override
        default T get(RecipeSearcher<T> context, SearchFrame<T> frame) {
            int depth = ++context.depth;
            if (depth == context.maxDepth) expansion(context);
            context.frames[depth].push(branch(), context.ints.length - depth, frame.branch == null ? frame.skip : frame.skip | (1L << frame.index));
            return null;
        }

        private void expansion(RecipeSearcher<T> context) {
            context.maxDepth *= 2;
            context.frames = Arrays.copyOf(context.frames, context.maxDepth);
            for (int i = context.depth; i < context.maxDepth; i++) {
                context.frames[i] = new SearchFrame<>();
            }
        }
    }

    class R<T> implements Node<T> {

        final T r;

        R(final T r) {
            this.r = r;
        }

        @Override
        public T get(RecipeSearcher<T> context, SearchFrame<T> frame) {
            return context.mapFunction.apply(r);
        }
    }

    class MR<T> implements Node<T> {

        final T[] rs;
        private final int length;

        MR(final T[] rs) {
            this.rs = rs;
            this.length = rs.length;
        }

        @Override
        public T get(RecipeSearcher<T> context, SearchFrame<T> frame) {
            int index;
            while ((index = context.count++) < length) {
                var r = rs[index];
                var re = context.mapFunction.apply(r);
                if (re != null) {
                    if (index < length - 1) context.node = this;
                    return re;
                }
            }
            context.count = 0;
            context.node = null;
            return null;
        }
    }

    final class B<T> implements BranchNode<T> {

        private Branch<T> b;

        private B(List<Runnable> branchBuilder) {
            this.b = Branch.create();
            branchBuilder.add(() -> this.b = this.b.optimize());
        }

        @Override
        public Branch<T> branch() {
            return b;
        }
    }

    final class BR<T> extends R<T> implements BranchNode<T> {

        private Branch<T> b;

        private BR(List<Runnable> branchBuilder, Branch<T> b, T r) {
            super(r);
            this.b = b;
            branchBuilder.add(() -> this.b = this.b.optimize());
        }

        @Override
        public Branch<T> branch() {
            return b;
        }

        @Override
        public T get(RecipeSearcher<T> context, SearchFrame<T> frame) {
            BranchNode.super.get(context, frame);
            return context.mapFunction.apply(r);
        }
    }

    final class BMR<T> extends MR<T> implements BranchNode<T> {

        private Branch<T> b;

        private BMR(List<Runnable> branchBuilder, Branch<T> b, T[] rs) {
            super(rs);
            this.b = b;
            branchBuilder.add(() -> this.b = this.b.optimize());
        }

        @Override
        public Branch<T> branch() {
            return b;
        }

        @Override
        public T get(RecipeSearcher<T> context, SearchFrame<T> frame) {
            if (frame != null) BranchNode.super.get(context, frame);
            return super.get(context, frame);
        }
    }
}
