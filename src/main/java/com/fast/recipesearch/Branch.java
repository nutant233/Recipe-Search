package com.fast.recipesearch;

import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

public interface Branch<R> {

    static <R> Branch<R> create() {
        return new RawBranch<>();
    }

    Node<R> get(int i);

    default Int2ObjectOpenHashMap<Node<R>> getNodes() {
        return null;
    }

    default Branch<R> optimize() {
        return this;
    }

    final class RawBranch<R> extends Int2ObjectOpenHashMap<Node<R>> implements Branch<R> {

        @Override
        @SuppressWarnings("unchecked")
        public Node<R> get(int k) {
            final int[] key = this.key;
            int curr;
            int pos;
            if ((curr = key[pos = HashCommon.mix(k) & mask]) != 0) {
                do if (curr == k) {
                    Object[] value = super.value;
                    return (Node<R>) value[pos];
                }
                while ((curr = key[pos = (pos + 1) & this.mask]) != 0);
            }
            return null;
        }

        @Override
        public Int2ObjectOpenHashMap<Node<R>> getNodes() {
            return this;
        }

        @Override
        public Branch<R> optimize() {
            var size = this.size;
            if (size == -1) return this;
            if (size == 0) return null;
            if (size == 1) {
                int a = 0;
                Node<R> av = null;
                for (var entry : int2ObjectEntrySet()) {
                    a = entry.getIntKey();
                    av = entry.getValue();
                }
                return new SingleBranch<>(a, av);
            }
            if (size == 2) {
                boolean assignment = false;
                int a = 0;
                int b = 0;
                Node<R> av = null;
                Node<R> bv = null;
                for (ObjectIterator<Entry<Node<R>>> it = int2ObjectEntrySet().fastIterator(); it.hasNext(); ) {
                    var entry = it.next();
                    if (assignment) {
                        b = entry.getIntKey();
                        bv = entry.getValue();
                    } else {
                        assignment = true;
                        a = entry.getIntKey();
                        av = entry.getValue();
                    }
                }
                return new DoubleBranch<>(a, b, av, bv);
            }
            if (size == 3) {
                int count = 0;
                int a = 0, b = 0, c = 0;
                Node<R> av = null, bv = null, cv = null;
                for (ObjectIterator<Entry<Node<R>>> it = int2ObjectEntrySet().fastIterator(); it.hasNext(); ) {
                    var entry = it.next();
                    switch (count) {
                        case 0 -> {
                            a = entry.getIntKey();
                            av = entry.getValue();
                        }
                        case 1 -> {
                            b = entry.getIntKey();
                            bv = entry.getValue();
                        }
                        case 2 -> {
                            c = entry.getIntKey();
                            cv = entry.getValue();
                        }
                    }
                    count++;
                }
                return new TripleBranch<>(a, b, c, av, bv, cv);
            }
            if (size == 4) {
                int count = 0;
                int a = 0, b = 0, c = 0, d = 0;
                Node<R> av = null, bv = null, cv = null, dv = null;
                for (ObjectIterator<Entry<Node<R>>> it = int2ObjectEntrySet().fastIterator(); it.hasNext(); ) {
                    var entry = it.next();
                    switch (count) {
                        case 0 -> {
                            a = entry.getIntKey();
                            av = entry.getValue();
                        }
                        case 1 -> {
                            b = entry.getIntKey();
                            bv = entry.getValue();
                        }
                        case 2 -> {
                            c = entry.getIntKey();
                            cv = entry.getValue();
                        }
                        case 3 -> {
                            d = entry.getIntKey();
                            dv = entry.getValue();
                        }
                    }
                    count++;
                }
                return new QuadBranch<>(a, b, c, d, av, bv, cv, dv);
            }
            this.size = -1;
            return this;
        }

        private record SingleBranch<R>(int a, Node<R> av) implements Branch<R> {
            @Override
            public Node<R> get(int i) {
                return i == a ? av : null;
            }
        }

        private record DoubleBranch<R>(int a, int b, Node<R> av, Node<R> bv) implements Branch<R> {
            @Override
            public Node<R> get(int i) {
                if (i == a) return av;
                if (i == b) return bv;
                return null;
            }
        }

        private record TripleBranch<R>(int a, int b, int c, Node<R> av, Node<R> bv, Node<R> cv) implements Branch<R> {
            @Override
            public Node<R> get(int i) {
                if (i == a) return av;
                if (i == b) return bv;
                if (i == c) return cv;
                return null;
            }
        }

        private record QuadBranch<R>(int a, int b, int c, int d, Node<R> av, Node<R> bv, Node<R> cv,
                                     Node<R> dv) implements Branch<R> {
            @Override
            public Node<R> get(int i) {
                if (i == a) return av;
                if (i == b) return bv;
                if (i == c) return cv;
                if (i == d) return dv;
                return null;
            }
        }
    }

}
