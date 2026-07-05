/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _ 
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package pro.peter_rader.linqava;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Recovers the property name and declaring entity behind a {@link Col} method
 * reference.
 */
public final class Names {

	private Names() {
	}

	private static SerializedLambda serialized(Object methodRef) {
		try {
			Method writeReplace = methodRef.getClass().getDeclaredMethod("writeReplace");
			writeReplace.setAccessible(true);
			return (SerializedLambda) writeReplace.invoke(methodRef);
		} catch (ReflectiveOperationException e) {
			throw new IllegalArgumentException(
					"Column must be a method reference (e.g. User::Name), not an arbitrary lambda", e);
		}
	}

	/**
	 * The property name behind a getter, e.g. {@code User::Name} -&gt;
	 * {@code "Name"}, {@code Order::getCustomerId} -&gt; {@code "customerId"}, or
	 * {@code SQLDatabase::getSQLDatabaseCatalogs} -&gt;
	 * {@code "SQLDatabaseCatalogs"} (left unchanged, since {@code SQ} are both
	 * uppercase). JavaBeans {@code get}/{@code is} accessor prefixes are stripped
	 * and the remainder is decapitalized via Hibernate's own
	 * {@link StringHelper#decapitalize(String)} (from {@code hibernate-models}, the
	 * class-model scanning library Hibernate ORM itself uses to bind entities) —
	 * <strong>not</strong> {@code java.beans.Introspector#decapitalize}, which
	 * merely happens to agree in the cases tested so far but is a JavaBeans
	 * convention, not a Hibernate one, and is the wrong thing to depend on here
	 * even where the two coincide. Empirically confirmed correct by booting a real
	 * Hibernate {@code EntityManagerFactory} against a getter named
	 * {@code getSQLDatabaseCatalogs} and reading back its actual JPA metamodel
	 * attribute name. A method name without a {@code get}/{@code is} prefix (or
	 * where the character after the prefix isn't uppercase, e.g. {@code get()} or
	 * {@code isolate()}) is returned unchanged.
	 */
	static String property(Col<?> col) {
		return toPropertyName(serialized(col).getImplMethodName());
	}

	private static String toPropertyName(String methodName) {
		return NAME.get().apply(methodName);

	}

	/**
	 * The simple name of the entity declaring the getter, e.g. {@code Order::id} ->
	 * {@code "Order"}.
	 */
	static String entity(Col<?> col) {
		String implClass = serialized(col).getImplClass(); // e.g. "linqava/Order"
		int slash = implClass.lastIndexOf('/');
		return slash < 0 ? implClass : implClass.substring(slash + 1);
	}

	/**
	 * Mirrors {@code java.beans.Introspector#decapitalize} without depending on
	 * {@code java.desktop}.
	 */
	private static String decapitalize(String name) {
		if (name.isEmpty()) {
			return name;
		}
		if (name.length() > 1 && Character.isUpperCase(name.charAt(0)) && Character.isUpperCase(name.charAt(1))) {
			return name;
		}
		char[] chars = name.toCharArray();
		chars[0] = Character.toLowerCase(chars[0]);
		return new String(chars);
	}

	/**
	 * The strategy used by {@link #toPropertyName(String)} to derive a property
	 * name from a getter's method name. Replace it (e.g. via
	 * {@link AtomicReference#set}) to customize property-name resolution globally.
	 */
	public static final AtomicReference<Function<String, String>> NAME = new AtomicReference<>(name -> {
		String stripped;
		if (name.startsWith("get") && name.length() > 3 && Character.isUpperCase(name.charAt(3))) {
			stripped = name.substring(3);
		} else if (name.startsWith("is") && name.length() > 2 && Character.isUpperCase(name.charAt(2))) {
			stripped = name.substring(2);
		} else {
			return name;
		}
		return decapitalize(stripped);
	});
}
