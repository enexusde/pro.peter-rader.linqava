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
 * @param <R> what folding the finished predicate back into its clause yields — {@link Q} for
 *            {@link Q}'s {@code WHERE}/{@code AND}/{@code OR}/{@code ON}/{@code HAVING}, or
 *            {@link EntityQ} for {@link EntityQ}'s {@code WHERE}/{@code AND}/{@code OR}
 */
public final class WhereStep<R> {

	@FunctionalInterface
	interface Sink<R> {
		R apply(Expr predicate, String connector);
	}

	private final Expr left;
	private final String leftHint;
	private final String connector;
	private final Sink<R> sink;

	WhereStep(Expr left, String leftHint, String connector, Sink<R> sink) {
		this.left = left;
		this.leftHint = leftHint;
		this.connector = connector;
		this.sink = sink;
	}

	/**
	 * Navigates from the left operand into a nested association member, e.g.
	 * {@code AND(Car::plate).ᐧ(SerialPlate::id)} &rarr; left operand {@code c.plate.id} — shorthand for
	 * {@code AND(typedCol(Car::plate).ᐧ(SerialPlate::id))}.
	 *
	 * @param getter the member field getter (method reference); must not be {@code null}
	 * @param <T>    the type owning the getter
	 * @return the pending comparison with the extended left operand, awaiting an operator
	 */
	public <T> WhereStep<R> ㅤᐅㅤ(TypedCol<T, ?> getter) {
		return new WhereStep<>(left.ᐧ(getter), Names.property(getter), connector, sink);
	}

	/**
	 * Equality ({@code =}).
	 *
	 * @param r the right operand; must not be {@code null}
	 * @return the query builder, for chaining
	 */
	public R ㅤᆖㅤ(Object r) { return cmp("=", r); }

	/**
	 * Equality ({@code =}) with a bare right column, e.g. {@code WHERE(Order::a).ㅤᆖㅤ(Order::b)}
	 * &rarr; {@code = b} (or {@code = alias.b} if {@code Order} has a declared alias) — no
	 * {@link Linq#typedCol(TypedCol) typedCol(...)} wrapping needed for the right operand.
	 *
	 * @param r   the right column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the right column
	 * @return the query builder, for chaining
	 */
	public <T> R ㅤᆖㅤ(TypedCol<T, ?> r) { return cmp("=", Expr.typedCol(r)); }

	/**
	 * Equality ({@code =}) with an alias-qualified right column, e.g. {@code ᆖ("o", Order::customerId)}
	 * &rarr; {@code = o.customerId} — shorthand for {@code ᆖ(typedCol("o", Order::customerId))}.
	 *
	 * @param alias the range-variable alias to qualify the right column with; must not be {@code null}
	 * @param r     the right column getter (method reference); must not be {@code null}
	 * @param <T>   the entity type owning the right column
	 * @return the query builder, for chaining
	 */
	public <T> R ㅤᆖㅤ(String alias, TypedCol<T, ?> r) { return cmp("=", Linq.typedCol(alias, r)); }

	/**
	 * Less-than ({@code <}).
	 *
	 * @param r the right operand; must not be {@code null}
	 * @return the query builder, for chaining
	 */
	public R ㅤᐸㅤ(Object r) { return cmp("<", r); }

	/**
	 * Less-than ({@code <}) with a bare right column — see {@link #ㅤᆖㅤ(TypedCol)}.
	 *
	 * @param r   the right column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the right column
	 * @return the query builder, for chaining
	 */
	public <T> R ㅤᐸㅤ(TypedCol<T, ?> r) { return cmp("<", Expr.typedCol(r)); }

	/**
	 * Less-than ({@code <}) with an alias-qualified right column.
	 *
	 * @param alias the range-variable alias to qualify the right column with; must not be {@code null}
	 * @param r     the right column getter (method reference); must not be {@code null}
	 * @param <T>   the entity type owning the right column
	 * @return the query builder, for chaining
	 */
	public <T> R ㅤᐸㅤ(String alias, TypedCol<T, ?> r) { return cmp("<", Linq.typedCol(alias, r)); }

	/**
	 * Greater-than ({@code >}).
	 *
	 * @param r the right operand; must not be {@code null}
	 * @return the query builder, for chaining
	 */
	public R ㅤᐳㅤ(Object r) { return cmp(">", r); }

	/**
	 * Greater-than ({@code >}) with a bare right column — see {@link #ㅤᆖㅤ(TypedCol)}.
	 *
	 * @param r   the right column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the right column
	 * @return the query builder, for chaining
	 */
	public <T> R ㅤᐳㅤ(TypedCol<T, ?> r) { return cmp(">", Expr.typedCol(r)); }

	/**
	 * Greater-than ({@code >}) with an alias-qualified right column.
	 *
	 * @param alias the range-variable alias to qualify the right column with; must not be {@code null}
	 * @param r     the right column getter (method reference); must not be {@code null}
	 * @param <T>   the entity type owning the right column
	 * @return the query builder, for chaining
	 */
	public <T> R ㅤᐳㅤ(String alias, TypedCol<T, ?> r) { return cmp(">", Linq.typedCol(alias, r)); }

	/**
	 * Less-than-or-equal ({@code <=}).
	 *
	 * @param r the right operand; must not be {@code null}
	 * @return the query builder, for chaining
	 */
	public R ᐸᆖ(Object r) { return cmp("<=", r); }

	/**
	 * Less-than-or-equal ({@code <=}) with a bare right column — see {@link #ㅤᆖㅤ(TypedCol)}.
	 *
	 * @param r   the right column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the right column
	 * @return the query builder, for chaining
	 */
	public <T> R ᐸᆖ(TypedCol<T, ?> r) { return cmp("<=", Expr.typedCol(r)); }

	/**
	 * Less-than-or-equal ({@code <=}) with an alias-qualified right column.
	 *
	 * @param alias the range-variable alias to qualify the right column with; must not be {@code null}
	 * @param r     the right column getter (method reference); must not be {@code null}
	 * @param <T>   the entity type owning the right column
	 * @return the query builder, for chaining
	 */
	public <T> R ᐸᆖ(String alias, TypedCol<T, ?> r) { return cmp("<=", Linq.typedCol(alias, r)); }

	/**
	 * Greater-than-or-equal ({@code >=}).
	 *
	 * @param r the right operand; must not be {@code null}
	 * @return the query builder, for chaining
	 */
	public R ᐳᆖ(Object r) { return cmp(">=", r); }

	/**
	 * Greater-than-or-equal ({@code >=}) with a bare right column — see {@link #ㅤᆖㅤ(TypedCol)}.
	 *
	 * @param r   the right column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the right column
	 * @return the query builder, for chaining
	 */
	public <T> R ᐳᆖ(TypedCol<T, ?> r) { return cmp(">=", Expr.typedCol(r)); }

	/**
	 * Greater-than-or-equal ({@code >=}) with an alias-qualified right column.
	 *
	 * @param alias the range-variable alias to qualify the right column with; must not be {@code null}
	 * @param r     the right column getter (method reference); must not be {@code null}
	 * @param <T>   the entity type owning the right column
	 * @return the query builder, for chaining
	 */
	public <T> R ᐳᆖ(String alias, TypedCol<T, ?> r) { return cmp(">=", Linq.typedCol(alias, r)); }

	/**
	 * Not-equal ({@code <>}).
	 *
	 * @param r the right operand; must not be {@code null}
	 * @return the query builder, for chaining
	 */
	public R ᐸᐳ(Object r) { return cmp("<>", r); }

	/**
	 * Not-equal ({@code <>}) with a bare right column — see {@link #ㅤᆖㅤ(TypedCol)}.
	 *
	 * @param r   the right column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the right column
	 * @return the query builder, for chaining
	 */
	public <T> R ᐸᐳ(TypedCol<T, ?> r) { return cmp("<>", Expr.typedCol(r)); }

	/**
	 * Not-equal ({@code <>}) with an alias-qualified right column.
	 *
	 * @param alias the range-variable alias to qualify the right column with; must not be {@code null}
	 * @param r     the right column getter (method reference); must not be {@code null}
	 * @param <T>   the entity type owning the right column
	 * @return the query builder, for chaining
	 */
	public <T> R ᐸᐳ(String alias, TypedCol<T, ?> r) { return cmp("<>", Linq.typedCol(alias, r)); }

	/**
	 * Membership test ({@code in (...)}), e.g. {@code WHERE(Product::categoryId).IN(subquery)}.
	 *
	 * @param r the right side, typically a sub-query {@link Q}; must not be {@code null}
	 * @return the query builder, for chaining
	 */
	public R IN(Object r) { return cmp("in", r); }

	/**
	 * Membership test ({@code in (...)}) with a bare right column — see {@link #ㅤᆖㅤ(TypedCol)}.
	 *
	 * @param r   the right column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the right column
	 * @return the query builder, for chaining
	 */
	public <T> R IN(TypedCol<T, ?> r) { return cmp("in", Expr.typedCol(r)); }

	/**
	 * Membership test ({@code in (...)}) with an alias-qualified right column.
	 *
	 * @param alias the range-variable alias to qualify the right column with; must not be {@code null}
	 * @param r     the right column getter (method reference); must not be {@code null}
	 * @param <T>   the entity type owning the right column
	 * @return the query builder, for chaining
	 */
	public <T> R IN(String alias, TypedCol<T, ?> r) { return cmp("in", Linq.typedCol(alias, r)); }

	/**
	 * Pattern match ({@code like}), e.g. {@code WHERE(Supplier::iban).LIKE(param("p"))}.
	 *
	 * @param r the pattern (literal/{@link Linq#param(String)}); must not be {@code null}
	 * @return the query builder, for chaining
	 */
	public R LIKE(Object r) { return cmp("like", r); }

	/**
	 * Pattern match ({@code like}) with a bare right column — see {@link #ㅤᆖㅤ(TypedCol)}.
	 *
	 * @param r   the right column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the right column
	 * @return the query builder, for chaining
	 */
	public <T> R LIKE(TypedCol<T, ?> r) { return cmp("like", Expr.typedCol(r)); }

	/**
	 * Pattern match ({@code like}) with an alias-qualified right column.
	 *
	 * @param alias the range-variable alias to qualify the right column with; must not be {@code null}
	 * @param r     the right column getter (method reference); must not be {@code null}
	 * @param <T>   the entity type owning the right column
	 * @return the query builder, for chaining
	 */
	public <T> R LIKE(String alias, TypedCol<T, ?> r) { return cmp("like", Linq.typedCol(alias, r)); }

	/**
	 * Null test ({@code is null}), e.g. {@code WHERE(Employee::managerId).ISㅤNULL()}.
	 *
	 * @return the query builder, for chaining
	 */
	public R ISㅤNULL() { return unary("is null"); }

	/**
	 * Not-null test ({@code is not null}).
	 *
	 * @return the query builder, for chaining
	 */
	public R ISㅤNOTㅤNULL() { return unary("is not null"); }

	/**
	 * Empty-collection test ({@code is empty}).
	 *
	 * @return the query builder, for chaining
	 */
	public R ISㅤEMPTY() { return unary("is empty"); }

	/**
	 * Non-empty-collection test ({@code is not empty}), e.g. {@code WHERE(Customer::orders).ISㅤNOTㅤEMPTY()}.
	 *
	 * @return the query builder, for chaining
	 */
	public R ISㅤNOTㅤEMPTY() { return unary("is not empty"); }

	private R cmp(String op, Object r) {
		Expr rr = Expr.val(r, leftHint);
		Expr l = left;
		return sink.apply(Expr.of(c -> l.render(c) + " " + op + " " + rr.render(c)), connector);
	}

	private R unary(String suffix) {
		Expr l = left;
		return sink.apply(Expr.of(c -> l.render(c) + " " + suffix), connector);
	}
}
