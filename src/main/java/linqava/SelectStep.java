/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _ 
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
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
	 * Aliases the most recently added {@code SELECT} expression.
	 *
	 * <p>Example: {@code SELECT(User::id).AS("idx").FROM(User.class)} &rarr; {@code select id as idx from User}.</p>
	 *
	 * @param alias the alias name; must not be {@code null} or blank
	 * @return this phase, for chaining further {@code AS}/{@code FROM}
	 */
	public SelectStep AS(String alias) {
		q.aliasLastSelect(alias);
		return this;
	}

	/**
	 * Sets the root entity ({@code from Entity}). Declare an alias with {@link Q#ㅤASㅤ(String)} right after.
	 *
	 * <p>Example: {@code FROM(User.class).AS("u")} &rarr; {@code from User u}.</p>
	 *
	 * @param root the root entity class; must not be {@code null}
	 * @return the query builder for the remaining clauses
	 */
	public Q ㅤFROMㅤ(Class<?> root) {
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
