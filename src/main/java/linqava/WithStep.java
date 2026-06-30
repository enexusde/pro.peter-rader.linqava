package linqava;

/**
 * The {@code WITH} phase: add further common table expressions, then start the main query with
 * {@code SELECT}. Only after {@code SELECT} does {@code FROM} become available (via {@link SelectStep}).
 */
public final class WithStep {

	private final Q q;

	WithStep(Q q) {
		this.q = q;
	}

	/**
	 * Adds another common table expression ({@code , name AS (definition)}).
	 *
	 * @param name       the CTE name; must not be {@code null} or blank
	 * @param definition the defining sub-query; must not be {@code null}
	 * @return this {@code WITH} phase, for chaining
	 */
	public WithStep WITH(String name, Q definition) {
		q.addCte(name, definition, false);
		return this;
	}

	/**
	 * Adds a recursive common table expression and marks the {@code WITH} clause recursive.
	 *
	 * @param name       the CTE name; must not be {@code null} or blank
	 * @param definition the recursive defining sub-query (anchor {@code UNION ALL} step); must not be {@code null}
	 * @return this {@code WITH} phase, for chaining
	 */
	public WithStep WITH‿RECURSIVE(String name, Q definition) {
		q.addCte(name, definition, true);
		return this;
	}

	/**
	 * Starts the main query's projection ({@code select ...}).
	 *
	 * @param cols the projected columns/expressions, in order; must not be {@code null}, may be empty
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#FROM(Class) FROM} next
	 */
	public SelectStep SELECT(Object... cols) {
		q.addSelect(cols);
		return new SelectStep(q);
	}

	/**
	 * Starts the main query's projection with a bare leading getter reference.
	 *
	 * @param first the first column getter (method reference); must not be {@code null}
	 * @param rest  the remaining columns/expressions, in order; must not be {@code null}, may be empty
	 * @param <A>   the entity type owning the first column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#FROM(Class) FROM} next
	 */
	public <A> SelectStep SELECT(Col<A> first, Object... rest) {
		q.addSelect(first, rest);
		return new SelectStep(q);
	}

	/**
	 * Starts the main query as a whole-entity selection — shorthand for {@code SELECT(entity(type))}.
	 *
	 * @param entityType the selected entity class; must not be {@code null}
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#FROM(Class) FROM} next
	 */
	public SelectStep SELECT(Class<?> entityType) {
		q.addSelect(Linq.entity(entityType));
		return new SelectStep(q);
	}
}
