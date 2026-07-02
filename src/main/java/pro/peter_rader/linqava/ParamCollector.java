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

	/** Registers {@code value} under a freshly invented name and returns that name. */
	String next(Object value) {
		String name = "__p" + params.size();
		params.put(name, value);
		return name;
	}

	/** The collected {@code name -> value} bindings, in the order they were invented. */
	Map<String, Object> params() {
		return params;
	}
}
