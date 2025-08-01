package com.alife.tolsim.utils;

import java.util.Iterator;
import java.util.Random;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

public class Utils {

    public static Random rand = new Random(5);

    /**
     * from <a href="http://stackoverflow.com/questions/363681/generating-random-integers-in-a-range-with-java">this page</a>
     * and <a href="http://stackoverflow.com/questions/3680637/how-to-generate-a-random-double-in-a-given-range">this page</a>
     * Returns a pseudo-random number between min and max, exclusive.
     * Uniform distribution.
     * The difference between min and max can be at most
     * <code>Integer.MAX_VALUE - 1</code>.
     *
     * @param min Minimum value
     * @param max Maximum value.  Must be greater than min.
     * @return Integer between min and max, exclusive.
     * @see Random#nextInt(int)
     */
    public static double uniformDouble(double min, double max) {
        return min + (max - min) * rand.nextDouble();
    }

    /**
     * Uniform distribution between 0 and 1
     * @return random number
     */
    public static double uniformDouble() {
        return rand.nextDouble();
    }

    /**
     * Filters an iterator.
     *
     * @param iterator the iterator to filter
     * @param filter the filter to apply
     * @return the filtered iterator
     * @param <E> the type parameter of the iterator and of the filter
     */
    public static <E> Iterator<E> filterIterator(Iterator<E> iterator, Predicate<E> filter) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
                .filter(filter)
                .iterator();
    }
}
