/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package linqava;

/**
 * The phase right after {@code ORDER BY(alias, field)}: the ordering column has been named; follow
 * with {@link #DESC()}/{@link #ASC()} to supply the direction and finish the clause.
 *
 * <p>Example: {@code ORDER‿BY("b", "total").DESC()} &rarr; {@code order by b.total desc}.</p>
 *
 * @param <E> the selected entity type of the {@link Q} this step folds back into
 */
public final class OrderByStep<E> {

	private final Q<E> q;
	private final Expr col;

	OrderByStep(Q<E> q, Expr col) {
		this.q = q;
		this.col = col;
	}

	/**
	 * Descending order ({@code desc}).
	 *
	 * @return the query builder, for chaining
	 */
	public Q<E> DESC() { return q.addOrderBy(col.DESC()); }

	/**
	 * Ascending order ({@code asc}).
	 *
	 * @return the query builder, for chaining
	 */
	public Q<E> ASC() { return q.addOrderBy(col.ASC()); }
}
