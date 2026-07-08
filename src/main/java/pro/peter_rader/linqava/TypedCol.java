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
 * A type-safe column reference whose Java result type is preserved, e.g. {@code Order::getId} typed
 * as {@code Function<Order, Long>} rather than erased to {@code Object} like {@link Col}. Used via
 * {@link Linq#typedCol(TypedCol)} so a bare-column {@code SELECT} can infer its result type from the
 * getter itself, without an explicit {@code Class} argument at {@link Q#via(jakarta.persistence.EntityManager)}.
 *
 * @param <T> the entity type the column is read from
 * @param <R> the Java type of the column
 */
@FunctionalInterface
public interface TypedCol<T, R> extends Function<T, R>, Serializable {
}
