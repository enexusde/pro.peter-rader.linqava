/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package pro.peter_rader.linqava;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Collects bind-parameter values for {@link Q#via}, inventing a unique name for each literal
 * encountered while rendering — see {@link Expr}'s {@code LiteralExpr}. Shared across a whole query
 * tree (CTEs, unions, sub-queries) so every literal gets its own name in one parameter namespace.
 */
final class ParamCollector {

	private final Map<String, Object> params = new LinkedHashMap<>();
	private int autoCounter;

	/**
	 * Registers {@code value} under a freshly invented name and returns that name.
	 *
	 * @param hint the property name of the column this value was compared against, if known; {@code
	 *             null} falls back to a numbered name ({@code __p0}, {@code __p1}, ...). A non-{@code
	 *             null} hint yields {@code "__" + hint}, disambiguated with a numeric suffix
	 *             ({@code __total}, {@code __total2}, ...) if that name was already used in this query.
	 */
	String next(Object value, String hint) {
		String name;
		if (hint == null) {
			name = "__p" + autoCounter++;
		} else {
			name = "__" + hint;
			int suffix = 2;
			while (params.containsKey(name)) {
				name = "__" + hint + suffix++;
			}
		}
		params.put(name, value);
		return name;
	}

	/** The collected {@code name -> value} bindings, in the order they were invented. */
	Map<String, Object> params() {
		return params;
	}
}
