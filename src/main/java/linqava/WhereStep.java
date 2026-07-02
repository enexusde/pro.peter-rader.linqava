/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package linqava;

/**
 * The phase right after {@code WHERE(Col)}/{@code AND(Col)}: a column has been named and the next
 * call supplies the comparison operator and right-hand value, finishing that predicate.
 *
 * <p>Example: {@code WHERE(Driver::id).ᐳ(0).AND(Driver::id).ᐸ(3)} &rarr;
 * {@code where id > 0 and id < 3}.</p>
 *
 * @param <T> the entity type owning the column
 */
public final class WhereStep<T> {

	private final Q q;
	private final Col<T> col;
	private final String connector;

	WhereStep(Q q, Col<T> col, String connector) {
		this.q = q;
		this.col = col;
		this.connector = connector;
	}

	/**
	 * Equality ({@code =}).
	 *
	 * @param r the right operand; must not be {@code null}
	 * @return the query builder, for chaining
	 */
	public Q ᆖ(Object r) { return apply("=", r); }

	/**
	 * Less-than ({@code <}).
	 *
	 * @param r the right operand; must not be {@code null}
	 * @return the query builder, for chaining
	 */
	public Q ᐸ(Object r) { return apply("<", r); }

	/**
	 * Greater-than ({@code >}).
	 *
	 * @param r the right operand; must not be {@code null}
	 * @return the query builder, for chaining
	 */
	public Q ㅤᐳㅤ(Object r) { return apply(">", r); }

	/**
	 * Less-than-or-equal ({@code <=}).
	 *
	 * @param r the right operand; must not be {@code null}
	 * @return the query builder, for chaining
	 */
	public Q ᐸᆖ(Object r) { return apply("<=", r); }

	/**
	 * Greater-than-or-equal ({@code >=}).
	 *
	 * @param r the right operand; must not be {@code null}
	 * @return the query builder, for chaining
	 */
	public Q ᐳᆖ(Object r) { return apply(">=", r); }

	/**
	 * Not-equal ({@code <>}).
	 *
	 * @param r the right operand; must not be {@code null}
	 * @return the query builder, for chaining
	 */
	public Q ᐸᐳ(Object r) { return apply("<>", r); }

	private Q apply(String op, Object r) {
		return q.appendWhere(col, op, r, connector);
	}
}
