/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package pro.peter_rader.linqava;

/**
 * The phase right after {@code SELECT(scalarExpr)} (see {@link Linq#SELECTㅤ(ScalarExpr)}): the only
 * legal next call is {@code FROM}, which yields a {@link ScalarQ} rather than a {@link Q} — mirroring
 * {@link SelectStep}, but for a projection whose single scalar/aggregate result type is statically
 * known (e.g. {@link Linq#COUNTㅤꁘ()}).
 *
 * @param <T> the Java type of the scalar value, threaded through to the resulting {@link ScalarQ}
 */
public final class ScalarSelectStep<T> {

	private final Q<T> q;
	private final Class<T> type;

	ScalarSelectStep(Q<T> q, Class<T> type) {
		this.q = q;
		this.type = type;
	}

	/**
	 * Sets the root entity ({@code from Entity}). Declare an alias with
	 * {@link ScalarQ#ㅤAS(String)} right after if needed.
	 *
	 * @param root the root entity class; must not be {@code null}
	 * @return the query builder for the remaining clauses
	 */
	public ScalarQ<T> ㅤFROMㅤ(Class<?> root) {
		return new ScalarQ<>(q.setFrom(root), type);
	}

	/**
	 * Sets the root to a CTE or derived table referenced by name.
	 *
	 * @param cteOrDerived the CTE/derived-table name; must not be {@code null} or blank
	 * @return the query builder for the remaining clauses
	 */
	public ScalarQ<T> FROM(String cteOrDerived) {
		return new ScalarQ<>(q.setFrom(cteOrDerived), type);
	}
}
