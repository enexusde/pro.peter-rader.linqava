/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _ 
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package pro.peter_rader.linqava;

import java.util.List;

/** Rendering context: resolves a bare getter's entity to the alias declared in the current query. */
final class RenderCtx {

	/** Each entry is {@code {alias, entitySimpleName}} for a class-backed FROM/JOIN with an alias. */
	private final List<String[]> sources;
	private final ParamCollector collector;

	RenderCtx(List<String[]> sources) {
		this(sources, null);
	}

	RenderCtx(List<String[]> sources, ParamCollector collector) {
		this.sources = sources;
		this.collector = collector;
	}

	/** Alias for the given entity within this query, or {@code null} if none was declared. */
	String aliasFor(String entitySimpleName) {
		for (String[] s : sources) {
			if (entitySimpleName.equals(s[1])) {
				return s[0];
			}
		}
		return null;
	}

	/** The active parameter collector, or {@code null} when literals should render inline. */
	ParamCollector collector() {
		return collector;
	}
}
