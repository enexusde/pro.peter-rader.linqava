package linqava;

/**
 * The phase right after {@code SELECT(...)}: the only legal next call is {@code FROM}. Returning a
 * distinct type here makes {@code SELECT(...).FROM(x).FROM(y)} and a missing {@code FROM}
 * impossible to write — the resulting {@link Q} no longer offers {@code SELECT} or {@code FROM}.
 */
public final class SelectStep {

	private final Q q;

	SelectStep(Q q) {
		this.q = q;
	}

	/**
	 * Sets the root entity ({@code from Entity}). Declare an alias with {@link Q#AS(String)} right after.
	 *
	 * <p>Example: {@code FROM(User.class).AS("u")} &rarr; {@code from User u}.</p>
	 *
	 * @param root the root entity class; must not be {@code null}
	 * @return the query builder for the remaining clauses
	 */
	public Q FROM(Class<?> root) {
		return q.setFrom(root);
	}

	/**
	 * Sets the root to a CTE or derived table referenced by name.
	 *
	 * <p>Example: {@code FROM("activeUsers").AS("a")} &rarr; {@code from activeUsers a}.</p>
	 *
	 * @param cteOrDerived the CTE/derived-table name; must not be {@code null} or blank
	 * @return the query builder for the remaining clauses
	 */
	public Q FROM(String cteOrDerived) {
		return q.setFrom(cteOrDerived);
	}
}
