package linqava;

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

	/** The getter name, e.g. {@code User::Name} -> {@code "Name"}. */
	static String property(Col<?> col) {
		return serialized(col).getImplMethodName();
	}

	/** The simple name of the entity declaring the getter, e.g. {@code Order::id} -> {@code "Order"}. */
	static String entity(Col<?> col) {
		String implClass = serialized(col).getImplClass(); // e.g. "linqava/Order"
		int slash = implClass.lastIndexOf('/');
		return slash < 0 ? implClass : implClass.substring(slash + 1);
	}
}
