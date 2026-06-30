package linqava;

import java.io.Serializable;
import java.util.function.Function;

/**
 * A type-safe column reference: a getter of an entity, e.g. {@code User::Name}.
 *
 * <p>It extends {@link Serializable} so that the property name behind a method reference can be
 * recovered at runtime (via {@code SerializedLambda}) when rendering HQL — see {@link Names}.</p>
 */
@FunctionalInterface
public interface Col<T> extends Function<T, Object>, Serializable {
}
