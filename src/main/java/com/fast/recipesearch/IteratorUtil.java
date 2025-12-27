package com.fast.recipesearch;

import java.util.Iterator;
import java.util.function.Function;

public class IteratorUtil {

    public static <T> Iterable<T> wrap(Iterator<T> iterator) {
        return () -> iterator;
    }

    public static <T> Iterator<T> map(Iterator<T> iterator, Function<T, T> mapFunction) {
        return new Iterator<>() {

            private T next;

            @Override
            public boolean hasNext() {
                while (iterator.hasNext()) {
                    next = mapFunction.apply(iterator.next());
                    if (next != null) return true;
                }
                return false;
            }

            @Override
            public T next() {
                return next;
            }
        };
    }

    public static <T> Iterator<T> concat(Iterator<T> first, Iterator<T> second) {
        return new Iterator<>() {
            private Iterator<? extends T> current = first;

            @Override
            public boolean hasNext() {
                if (current.hasNext()) return true;
                if (current == first) current = second;
                return current.hasNext();
            }

            @Override
            public T next() {
                return current.next();
            }

            @Override
            public void remove() {
                current.remove();
            }
        };
    }
}
