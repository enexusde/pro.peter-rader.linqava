package linqava;

import java.util.List;

/** Rendering context: resolves a bare getter's entity to the alias declared in the current query. */
final class RenderCtx {

	/** Each entry is {@code {alias, entitySimpleName}} for a class-backed FROM/JOIN with an alias. */
	private final List<String[]> sources;

	RenderCtx(List<String[]> sources) {
		this.sources = sources;
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
}
