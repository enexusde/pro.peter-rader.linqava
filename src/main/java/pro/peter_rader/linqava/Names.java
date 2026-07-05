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

/** Recovers the property name and declaring entity behind a {@link Col} method reference. */
final class Names {

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
	 * The property name behind a getter, e.g. {@code User::Name} -&gt; {@code "Name"} or
	 * {@code Order::getCustomerId} -&gt; {@code "customerId"}. JavaBeans {@code get}/{@code is}
	 * accessor prefixes are stripped and the remainder is decapitalized (following
	 * {@code java.beans.Introspector#decapitalize}); a method name without such a prefix (or
	 * where the character after the prefix isn't uppercase, e.g. {@code get()} or
	 * {@code isolate()}) is returned unchanged.
	 */
	static String property(Col<?> col) {
		return toPropertyName(serialized(col).getImplMethodName());
	}

	private static String toPropertyName(String methodName) {
		String stripped;
		if (methodName.startsWith("get") && methodName.length() > 3 && Character.isUpperCase(methodName.charAt(3))) {
			stripped = methodName.substring(3);
		} else if (methodName.startsWith("is") && methodName.length() > 2
				&& Character.isUpperCase(methodName.charAt(2))) {
			stripped = methodName.substring(2);
		} else {
			return methodName;
		}
		return decapitalize(stripped);
	}

	/** Mirrors {@code java.beans.Introspector#decapitalize} without depending on {@code java.desktop}. */
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

	/** The simple name of the entity declaring the getter, e.g. {@code Order::id} -> {@code "Order"}. */
	static String entity(Col<?> col) {
		String implClass = serialized(col).getImplClass(); // e.g. "linqava/Order"
		int slash = implClass.lastIndexOf('/');
		return slash < 0 ? implClass : implClass.substring(slash + 1);
	}
}
