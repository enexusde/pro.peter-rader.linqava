/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package pro.peter_rader.linqava;

import java.io.Serializable;
import java.util.function.Function;

/**
 * A type-safe column reference, e.g. a getter of an entity such as {@code Order::getId}, typed as
 * {@code Function<Order, Long>} — unlike a raw method reference used as {@code Object}, its Java
 * result type {@code R} is preserved. This is the DSL's sole column-reference type, accepted bare
 * (as a method reference, no wrapping needed) throughout {@code SELECT}, {@code WHERE}, joins,
 * comparisons and aggregate functions.
 *
 * <p>It extends {@link Serializable} so that the property name and declaring entity behind a method
 * reference can be recovered at runtime (via {@code SerializedLambda}) when rendering HQL — see
 * {@link Names}.</p>
 *
 * @param <T> the entity type the column is read from
 * @param <R> the Java type of the column
 */
@FunctionalInterface
public interface TypedCol<T, R> extends Function<T, R>, Serializable {
}
