/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package pro.peter_rader.linqava;

/**
 * The phase right after {@code WHERE(...)}/{@code AND(...)}/{@code OR(...)}/{@code ON(...)}/
 * {@code HAVING(...)}: a left operand has been named and the next call supplies the operator (and,
 * for binary operators, the right-hand value), finishing that predicate and folding it into the
 * clause it was started from.
 *
 * <p>Example: {@code WHERE(Driver::id).ᐳ(0).AND(Driver::id).ᐸ(3)} &rarr;
 * {@code where id > 0 and id < 3}.</p>
 *
 * @param <E> the selected entity type of the {@link Q} this step folds back into
 */
public final class WhereStep<E> {

	@FunctionalInterface
	interface Sink<E> {
		Q<E> apply(Expr predicate, String connector);
	}

	private final Expr left;
	private final String connector;
	private final Sink<E> sink;

	WhereStep(Expr left, String connector, Sink<E> sink) {
		this.left = left;
		this.connector = connector;
		this.sink = sink;
	}

	/**
	 * Navigates from the left operand into a nested association member, e.g.
	 * {@code AND(Car::plate).ᐧ(SerialPlate::id)} &rarr; left operand {@code c.plate.id} — shorthand for
	 * {@code AND(col(Car::plate).ᐧ(SerialPlate::id))}.
	 *
	 * @param getter the member field getter (method reference); must not be {@code null}
	 * @param <T>    the type owning the getter
	 * @return the pending comparison with the extended left operand, awaiting an operator
	 */
	public <T> WhereStep<E> ㅤᐅㅤ(Col<T> getter) {
		return new WhereStep<>(left.ᐧ(getter), connector, sink);
	}

	/**
	 * Equality ({@code =}).
	 *
	 * @param r the right operand; must not be {@code null}
	 * @return the query builder, for chaining
	 */
	public Q<E> ㅤᆖㅤ(Object r) { return cmp("=", r); }

	/**
	 * Equality ({@code =}) with an alias-qualified right column, e.g. {@code ᆖ("o", Order::customerId)}
	 * &rarr; {@code = o.customerId} — shorthand for {@code ᆖ(col("o", Order::customerId))}.
	 *
	 * @param alias the range-variable alias to qualify the right column with; must not be {@code null}
	 * @param r     the right column getter (method reference); must not be {@code null}
	 * @param <T>   the entity type owning the right column
	 * @return the query builder, for chaining
	 */
	public <T> Q<E> ㅤᆖㅤ(String alias, Col<T> r) { return cmp("=", Linq.col(alias, r)); }

	/**
	 * Less-than ({@code <}).
	 *
	 * @param r the right operand; must not be {@code null}
	 * @return the query builder, for chaining
	 */
	public Q<E> ㅤᐸㅤ(Object r) { return cmp("<", r); }

	/**
	 * Less-than ({@code <}) with an alias-qualified right column.
	 *
	 * @param alias the range-variable alias to qualify the right column with; must not be {@code null}
	 * @param r     the right column getter (method reference); must not be {@code null}
	 * @param <T>   the entity type owning the right column
	 * @return the query builder, for chaining
	 */
	public <T> Q<E> ㅤᐸㅤ(String alias, Col<T> r) { return cmp("<", Linq.col(alias, r)); }

	/**
	 * Greater-than ({@code >}).
	 *
	 * @param r the right operand; must not be {@code null}
	 * @return the query builder, for chaining
	 */
	public Q<E> ㅤᐳㅤ(Object r) { return cmp(">", r); }

	/**
	 * Greater-than ({@code >}) with an alias-qualified right column.
	 *
	 * @param alias the range-variable alias to qualify the right column with; must not be {@code null}
	 * @param r     the right column getter (method reference); must not be {@code null}
	 * @param <T>   the entity type owning the right column
	 * @return the query builder, for chaining
	 */
	public <T> Q<E> ㅤᐳㅤ(String alias, Col<T> r) { return cmp(">", Linq.col(alias, r)); }

	/**
	 * Less-than-or-equal ({@code <=}).
	 *
	 * @param r the right operand; must not be {@code null}
	 * @return the query builder, for chaining
	 */
	public Q<E> ᐸᆖ(Object r) { return cmp("<=", r); }

	/**
	 * Less-than-or-equal ({@code <=}) with an alias-qualified right column.
	 *
	 * @param alias the range-variable alias to qualify the right column with; must not be {@code null}
	 * @param r     the right column getter (method reference); must not be {@code null}
	 * @param <T>   the entity type owning the right column
	 * @return the query builder, for chaining
	 */
	public <T> Q<E> ᐸᆖ(String alias, Col<T> r) { return cmp("<=", Linq.col(alias, r)); }

	/**
	 * Greater-than-or-equal ({@code >=}).
	 *
	 * @param r the right operand; must not be {@code null}
	 * @return the query builder, for chaining
	 */
	public Q<E> ᐳᆖ(Object r) { return cmp(">=", r); }

	/**
	 * Greater-than-or-equal ({@code >=}) with an alias-qualified right column.
	 *
	 * @param alias the range-variable alias to qualify the right column with; must not be {@code null}
	 * @param r     the right column getter (method reference); must not be {@code null}
	 * @param <T>   the entity type owning the right column
	 * @return the query builder, for chaining
	 */
	public <T> Q<E> ᐳᆖ(String alias, Col<T> r) { return cmp(">=", Linq.col(alias, r)); }

	/**
	 * Not-equal ({@code <>}).
	 *
	 * @param r the right operand; must not be {@code null}
	 * @return the query builder, for chaining
	 */
	public Q<E> ᐸᐳ(Object r) { return cmp("<>", r); }

	/**
	 * Not-equal ({@code <>}) with an alias-qualified right column.
	 *
	 * @param alias the range-variable alias to qualify the right column with; must not be {@code null}
	 * @param r     the right column getter (method reference); must not be {@code null}
	 * @param <T>   the entity type owning the right column
	 * @return the query builder, for chaining
	 */
	public <T> Q<E> ᐸᐳ(String alias, Col<T> r) { return cmp("<>", Linq.col(alias, r)); }

	/**
	 * Membership test ({@code in (...)}), e.g. {@code WHERE(Product::categoryId).IN(subquery)}.
	 *
	 * @param r the right side, typically a sub-query {@link Q}; must not be {@code null}
	 * @return the query builder, for chaining
	 */
	public Q<E> IN(Object r) { return cmp("in", r); }

	/**
	 * Pattern match ({@code like}), e.g. {@code WHERE(Supplier::iban).LIKE(param("p"))}.
	 *
	 * @param r the pattern (literal/{@link Linq#param(String)}); must not be {@code null}
	 * @return the query builder, for chaining
	 */
	public Q<E> LIKE(Object r) { return cmp("like", r); }

	/**
	 * Null test ({@code is null}), e.g. {@code WHERE(Employee::managerId).ISㅤNULL()}.
	 *
	 * @return the query builder, for chaining
	 */
	public Q<E> ISㅤNULL() { return unary("is null"); }

	/**
	 * Not-null test ({@code is not null}).
	 *
	 * @return the query builder, for chaining
	 */
	public Q<E> ISㅤNOTㅤNULL() { return unary("is not null"); }

	/**
	 * Empty-collection test ({@code is empty}).
	 *
	 * @return the query builder, for chaining
	 */
	public Q<E> ISㅤEMPTY() { return unary("is empty"); }

	/**
	 * Non-empty-collection test ({@code is not empty}), e.g. {@code WHERE(Customer::orders).ISㅤNOTㅤEMPTY()}.
	 *
	 * @return the query builder, for chaining
	 */
	public Q<E> ISㅤNOTㅤEMPTY() { return unary("is not empty"); }

	private Q<E> cmp(String op, Object r) {
		Expr rr = Expr.val(r);
		Expr l = left;
		return sink.apply(Expr.of(c -> l.render(c) + " " + op + " " + rr.render(c)), connector);
	}

	private Q<E> unary(String suffix) {
		Expr l = left;
		return sink.apply(Expr.of(c -> l.render(c) + " " + suffix), connector);
	}
}
