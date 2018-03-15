package eu.agileeng.util;

/**
 * @author vvatov
 *
 * @param <F>
 * @param <T>
 */
public interface Function<F, T> {
    T apply(F input);

    @Override
    boolean equals(Object object);
}