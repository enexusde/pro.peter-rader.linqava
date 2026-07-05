package pro.peter_rader.linqava;

/**
 * Input to {@link Names#NAME}: the getter method name a property name should be derived from.
 *
 * @param methodName the getter's method name, e.g. {@code "getName"}
 */
public record PropertyNameSearch(String methodName) {

}
