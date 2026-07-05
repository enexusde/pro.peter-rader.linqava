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
 * A type-safe column reference: a getter of an entity, e.g. {@code User::Name}.
 *
 * <p>It extends {@link Serializable} so that the property name behind a method reference can be
 * recovered at runtime (via {@code SerializedLambda}) when rendering HQL — see {@link Names}.</p>
 *
 * @param <T> the entity type the column is read from
 */
@FunctionalInterface
public interface Col<T> extends Function<T, Object>, Serializable {
}
