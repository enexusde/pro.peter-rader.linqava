/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _ 
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package pro.peter_rader.linqava;

/**
 * A predicate (condition) tree. Comparison operators are methods named with Unicode glyphs that are
 * valid Java identifiers: {@code ᆖ} (=), {@code ᐸ} (&lt;), {@code ᐳ} (&gt;), {@code ᐸᆖ} (&lt;=),
 * {@code ᐳᆖ} (&gt;=), {@code ᐸᐳ} (&lt;&gt;).
 *
 * <p>A {@code Cond} is normally started via one of {@link Linq}'s static predicate functions
 * ({@link Linq#ㅤᆖㅤ(TypedCol, Object)}, {@link Linq#ㅤᐳㅤ(TypedCol, Object)}, {@link Linq#ㅤINㅤ(TypedCol, Object)}, ...) —
 * column-first, no lambda required — and then extended in place by chaining further instance
 * predicates. Each comparison/predicate has two overloads: a bare {@link TypedCol} reference (type-checked
 * against its entity) as the left operand, or an {@link Object} ({@link Expr}, sub-query {@link Q},
 * literal or {@link Linq#param parameter}).</p>
 *
 * <p>Predicates accumulate: each call appends its predicate to the context joined by the pending
 * connector (default {@code and}; switch with {@link #ㅤANDㅤ()} / {@link #OR()}). Example:</p>
 * <pre>{@code
 * ᆖ(Order::status, "PAID").AND().ᐳ(Order::total, 100)
 * // o.status = 'PAID' and o.total > 100
 * }</pre>
 *
 * <p>Null policy: no argument may be {@code null}. For null tests use {@link #ISㅤNULL(TypedCol)} /
 * {@link #ISㅤNOTㅤNULL(TypedCol)} instead of comparing against {@code null}.</p>
 */
public final class Cond {

	/** Creates an empty condition with no predicate accumulated yet. */
	Cond() {
	}

	Expr expr;
	private String pending = "and";
	private Expr pendingLeft;

	private Cond add(Expr predicate) {
		expr = (expr == null) ? predicate : Expr.bin(expr, pending, predicate);
		pending = "and";
		return this;
	}

	private Expr takePendingLeft() {
		Expr l = pendingLeft;
		pendingLeft = null;
		return l;
	}

	private static <T> Expr typedCol(TypedCol<T, ?> c) {
		String prop = Names.property(c);
		String entity = Names.entity(c);
		return Expr.of(ctx -> {
			String a = ctx.aliasFor(entity);
			return a == null ? prop : a + "." + prop;
		});
	}

	private static Expr cmp(Expr left, String op, Object right) {
		return cmp(left, op, right, null);
	}

	private static Expr cmp(Expr left, String op, Object right, String hint) {
		Expr r = Expr.val(right, hint);
		return Expr.of(c -> left.render(c) + " " + op + " " + r.render(c));
	}

	// ===== comparison glyph operators =====

	/**
	 * Equality ({@code =}), e.g. {@code $.ᆖ(Order::status, "PAID")} &rarr; {@code o.status = 'PAID'}.
	 *
	 * @param l   the left column getter (method reference); must not be {@code null}
	 * @param r   the right operand (Expr/param/sub-query/literal); must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @return this context, for chaining
	 */
	public <T> Cond ᆖ(TypedCol<T, ?> l, Object r) { return add(cmp(typedCol(l), "=", r, Names.property(l))); }

	/**
	 * Equality ({@code =}) with bare columns on both sides, e.g. {@code $.ᆖ(Order::a, Order::b)}
	 * &rarr; {@code a = b} — no {@link Linq#typedCol(TypedCol) typedCol(...)} wrapping needed for the right operand.
	 *
	 * @param l   the left column getter; must not be {@code null}
	 * @param r   the right column getter; must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @param <R> the entity type owning the right column
	 * @return this context, for chaining
	 */
	public <T, R> Cond ᆖ(TypedCol<T, ?> l, TypedCol<R, ?> r) { return add(cmp(typedCol(l), "=", typedCol(r), Names.property(l))); }

	/**
	 * Equality ({@code =}) with an expression left operand.
	 *
	 * @param l the left operand; must not be {@code null}
	 * @param r the right operand; must not be {@code null}
	 * @return this context, for chaining
	 */
	public Cond ᆖ(Object l, Object r) { return add(cmp(Expr.val(l), "=", r)); }

	/**
	 * Equality ({@code =}) against the left operand supplied by a preceding {@link #ㅤANDㅤ(Object)}.
	 *
	 * @param r the right operand; must not be {@code null}
	 * @return this context, for chaining
	 */
	public Cond ᆖ(Object r) { return add(cmp(takePendingLeft(), "=", r)); }

	/**
	 * Equality ({@code =}) with a bare right column, against the left operand supplied by a preceding
	 * {@link #ㅤANDㅤ(Object)}.
	 *
	 * @param r   the right column getter; must not be {@code null}
	 * @param <T> the entity type owning the right column
	 * @return this context, for chaining
	 */
	public <T> Cond ᆖ(TypedCol<T, ?> r) { return add(cmp(takePendingLeft(), "=", typedCol(r))); }

	/**
	 * Less-than ({@code <}), e.g. {@code $.ᐸ(Order::discount, 5)} &rarr; {@code o.discount < 5}.
	 *
	 * @param l   the left column getter; must not be {@code null}
	 * @param r   the right operand; must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @return this context, for chaining
	 */
	public <T> Cond ᐸ(TypedCol<T, ?> l, Object r) { return add(cmp(typedCol(l), "<", r, Names.property(l))); }

	/**
	 * Less-than ({@code <}) with bare columns on both sides — see {@link #ᆖ(TypedCol, TypedCol)}.
	 *
	 * @param l   the left column getter; must not be {@code null}
	 * @param r   the right column getter; must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @param <R> the entity type owning the right column
	 * @return this context, for chaining
	 */
	public <T, R> Cond ᐸ(TypedCol<T, ?> l, TypedCol<R, ?> r) { return add(cmp(typedCol(l), "<", typedCol(r), Names.property(l))); }

	/**
	 * Less-than ({@code <}) with an expression left operand.
	 *
	 * @param l the left operand; must not be {@code null}
	 * @param r the right operand; must not be {@code null}
	 * @return this context, for chaining
	 */
	public Cond ᐸ(Object l, Object r) { return add(cmp(Expr.val(l), "<", r)); }

	/**
	 * Less-than ({@code <}) against the left operand supplied by a preceding {@link #ㅤANDㅤ(Object)}.
	 *
	 * @param r the right operand; must not be {@code null}
	 * @return this context, for chaining
	 */
	public Cond ᐸ(Object r) { return add(cmp(takePendingLeft(), "<", r)); }

	/**
	 * Less-than ({@code <}) with a bare right column, against the left operand supplied by a
	 * preceding {@link #ㅤANDㅤ(Object)}.
	 *
	 * @param r   the right column getter; must not be {@code null}
	 * @param <T> the entity type owning the right column
	 * @return this context, for chaining
	 */
	public <T> Cond ᐸ(TypedCol<T, ?> r) { return add(cmp(takePendingLeft(), "<", typedCol(r))); }

	/**
	 * Greater-than ({@code >}), e.g. {@code $.ᐳ(Order::total, 100)} &rarr; {@code o.total > 100}.
	 *
	 * @param l   the left column getter; must not be {@code null}
	 * @param r   the right operand; must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @return this context, for chaining
	 */
	public <T> Cond ᐳ(TypedCol<T, ?> l, Object r) { return add(cmp(typedCol(l), ">", r, Names.property(l))); }

	/**
	 * Greater-than ({@code >}) with bare columns on both sides — see {@link #ᆖ(TypedCol, TypedCol)}.
	 *
	 * @param l   the left column getter; must not be {@code null}
	 * @param r   the right column getter; must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @param <R> the entity type owning the right column
	 * @return this context, for chaining
	 */
	public <T, R> Cond ᐳ(TypedCol<T, ?> l, TypedCol<R, ?> r) { return add(cmp(typedCol(l), ">", typedCol(r), Names.property(l))); }

	/**
	 * Greater-than ({@code >}) with an expression left operand.
	 *
	 * @param l the left operand; must not be {@code null}
	 * @param r the right operand; must not be {@code null}
	 * @return this context, for chaining
	 */
	public Cond ㅤᐳㅤ(Object l, Object r) { return add(cmp(Expr.val(l), ">", r)); }

	/**
	 * Greater-than ({@code >}) against the left operand supplied by a preceding {@link #ㅤANDㅤ(Object)},
	 * e.g. {@code AND(SUM(Order::total)).ᐳ(1000)} &rarr; {@code and sum(o.total) > 1000}.
	 *
	 * @param r the right operand; must not be {@code null}
	 * @return this context, for chaining
	 */
	public Cond ㅤᐳㅤ(Object r) { return add(cmp(takePendingLeft(), ">", r)); }

	/**
	 * Greater-than ({@code >}) with a bare right column, against the left operand supplied by a
	 * preceding {@link #ㅤANDㅤ(Object)}.
	 *
	 * @param r   the right column getter; must not be {@code null}
	 * @param <T> the entity type owning the right column
	 * @return this context, for chaining
	 */
	public <T> Cond ㅤᐳㅤ(TypedCol<T, ?> r) { return add(cmp(takePendingLeft(), ">", typedCol(r))); }

	/**
	 * Less-than-or-equal ({@code <=}), e.g. {@code $.ᐸᆖ(Order::discount, 50)} &rarr; {@code o.discount <= 50}.
	 *
	 * @param l   the left column getter; must not be {@code null}
	 * @param r   the right operand; must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @return this context, for chaining
	 */
	public <T> Cond ᐸᆖ(TypedCol<T, ?> l, Object r) { return add(cmp(typedCol(l), "<=", r, Names.property(l))); }

	/**
	 * Less-than-or-equal ({@code <=}) with bare columns on both sides — see {@link #ᆖ(TypedCol, TypedCol)}.
	 *
	 * @param l   the left column getter; must not be {@code null}
	 * @param r   the right column getter; must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @param <R> the entity type owning the right column
	 * @return this context, for chaining
	 */
	public <T, R> Cond ᐸᆖ(TypedCol<T, ?> l, TypedCol<R, ?> r) { return add(cmp(typedCol(l), "<=", typedCol(r), Names.property(l))); }

	/**
	 * Less-than-or-equal ({@code <=}) with an expression left operand.
	 *
	 * @param l the left operand; must not be {@code null}
	 * @param r the right operand; must not be {@code null}
	 * @return this context, for chaining
	 */
	public Cond ᐸᆖ(Object l, Object r) { return add(cmp(Expr.val(l), "<=", r)); }

	/**
	 * Less-than-or-equal ({@code <=}) against the left operand supplied by a preceding {@link #ㅤANDㅤ(Object)}.
	 *
	 * @param r the right operand; must not be {@code null}
	 * @return this context, for chaining
	 */
	public Cond ᐸᆖ(Object r) { return add(cmp(takePendingLeft(), "<=", r)); }

	/**
	 * Less-than-or-equal ({@code <=}) with a bare right column, against the left operand supplied by a
	 * preceding {@link #ㅤANDㅤ(Object)}.
	 *
	 * @param r   the right column getter; must not be {@code null}
	 * @param <T> the entity type owning the right column
	 * @return this context, for chaining
	 */
	public <T> Cond ᐸᆖ(TypedCol<T, ?> r) { return add(cmp(takePendingLeft(), "<=", typedCol(r))); }

	/**
	 * Greater-than-or-equal ({@code >=}), e.g. {@code $.ᐳᆖ(Order::total, 1000)} &rarr; {@code o.total >= 1000}.
	 *
	 * @param l   the left column getter; must not be {@code null}
	 * @param r   the right operand; must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @return this context, for chaining
	 */
	public <T> Cond ᐳᆖ(TypedCol<T, ?> l, Object r) { return add(cmp(typedCol(l), ">=", r, Names.property(l))); }

	/**
	 * Greater-than-or-equal ({@code >=}) with bare columns on both sides — see {@link #ᆖ(TypedCol, TypedCol)}.
	 *
	 * @param l   the left column getter; must not be {@code null}
	 * @param r   the right column getter; must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @param <R> the entity type owning the right column
	 * @return this context, for chaining
	 */
	public <T, R> Cond ᐳᆖ(TypedCol<T, ?> l, TypedCol<R, ?> r) { return add(cmp(typedCol(l), ">=", typedCol(r), Names.property(l))); }

	/**
	 * Greater-than-or-equal ({@code >=}) with an expression left operand.
	 *
	 * @param l the left operand; must not be {@code null}
	 * @param r the right operand; must not be {@code null}
	 * @return this context, for chaining
	 */
	public Cond ᐳᆖ(Object l, Object r) { return add(cmp(Expr.val(l), ">=", r)); }

	/**
	 * Greater-than-or-equal ({@code >=}) against the left operand supplied by a preceding {@link #ㅤANDㅤ(Object)}.
	 *
	 * @param r the right operand; must not be {@code null}
	 * @return this context, for chaining
	 */
	public Cond ᐳᆖ(Object r) { return add(cmp(takePendingLeft(), ">=", r)); }

	/**
	 * Greater-than-or-equal ({@code >=}) with a bare right column, against the left operand supplied by
	 * a preceding {@link #ㅤANDㅤ(Object)}.
	 *
	 * @param r   the right column getter; must not be {@code null}
	 * @param <T> the entity type owning the right column
	 * @return this context, for chaining
	 */
	public <T> Cond ᐳᆖ(TypedCol<T, ?> r) { return add(cmp(takePendingLeft(), ">=", typedCol(r))); }

	/**
	 * Not-equal ({@code <>}), e.g. {@code $.ᐸᐳ(Order::status, "CANCELLED")} &rarr; {@code o.status <> 'CANCELLED'}.
	 *
	 * @param l   the left column getter; must not be {@code null}
	 * @param r   the right operand; must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @return this context, for chaining
	 */
	public <T> Cond ᐸᐳ(TypedCol<T, ?> l, Object r) { return add(cmp(typedCol(l), "<>", r, Names.property(l))); }

	/**
	 * Not-equal ({@code <>}) with bare columns on both sides — see {@link #ᆖ(TypedCol, TypedCol)}.
	 *
	 * @param l   the left column getter; must not be {@code null}
	 * @param r   the right column getter; must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @param <R> the entity type owning the right column
	 * @return this context, for chaining
	 */
	public <T, R> Cond ᐸᐳ(TypedCol<T, ?> l, TypedCol<R, ?> r) { return add(cmp(typedCol(l), "<>", typedCol(r), Names.property(l))); }

	/**
	 * Not-equal ({@code <>}) with an expression left operand.
	 *
	 * @param l the left operand; must not be {@code null}
	 * @param r the right operand; must not be {@code null}
	 * @return this context, for chaining
	 */
	public Cond ᐸᐳ(Object l, Object r) { return add(cmp(Expr.val(l), "<>", r)); }

	/**
	 * Not-equal ({@code <>}) against the left operand supplied by a preceding {@link #ㅤANDㅤ(Object)}.
	 *
	 * @param r the right operand; must not be {@code null}
	 * @return this context, for chaining
	 */
	public Cond ᐸᐳ(Object r) { return add(cmp(takePendingLeft(), "<>", r)); }

	/**
	 * Not-equal ({@code <>}) with a bare right column, against the left operand supplied by a
	 * preceding {@link #ㅤANDㅤ(Object)}.
	 *
	 * @param r   the right column getter; must not be {@code null}
	 * @param <T> the entity type owning the right column
	 * @return this context, for chaining
	 */
	public <T> Cond ᐸᐳ(TypedCol<T, ?> r) { return add(cmp(takePendingLeft(), "<>", typedCol(r))); }

	// ===== predicates =====

	/**
	 * Membership test ({@code in (...)}), e.g. {@code $.IN(Product::categoryId, subquery)}
	 * &rarr; {@code p.categoryId in (select ...)}.
	 *
	 * @param l   the left column getter; must not be {@code null}
	 * @param r   the right side, typically a sub-query {@link Q}; must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @return this context, for chaining
	 */
	public <T> Cond IN(TypedCol<T, ?> l, Object r) { return add(cmp(typedCol(l), "in", r, Names.property(l))); }

	/**
	 * Membership test ({@code in (...)}) with bare columns on both sides — see {@link #ᆖ(TypedCol, TypedCol)}.
	 *
	 * @param l   the left column getter; must not be {@code null}
	 * @param r   the right column getter (typically collection-valued); must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @param <R> the entity type owning the right column
	 * @return this context, for chaining
	 */
	public <T, R> Cond IN(TypedCol<T, ?> l, TypedCol<R, ?> r) { return add(cmp(typedCol(l), "in", typedCol(r), Names.property(l))); }

	/**
	 * Membership test ({@code in (...)}) with an expression left operand.
	 *
	 * @param l the left operand; must not be {@code null}
	 * @param r the right side (typically a sub-query); must not be {@code null}
	 * @return this context, for chaining
	 */
	public Cond IN(Object l, Object r) { return add(cmp(Expr.val(l), "in", r)); }

	/**
	 * Pattern match ({@code like}), e.g. {@code $.LIKE(Supplier::iban, param("p"))} &rarr; {@code iban like :p}.
	 *
	 * @param l   the left column getter; must not be {@code null}
	 * @param r   the pattern (literal/{@link Linq#param(String)}); must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @return this context, for chaining
	 */
	public <T> Cond LIKE(TypedCol<T, ?> l, Object r) { return add(cmp(typedCol(l), "like", r, Names.property(l))); }

	/**
	 * Pattern match ({@code like}) with bare columns on both sides — see {@link #ᆖ(TypedCol, TypedCol)}.
	 *
	 * @param l   the left column getter; must not be {@code null}
	 * @param r   the right column getter (the pattern); must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @param <R> the entity type owning the right column
	 * @return this context, for chaining
	 */
	public <T, R> Cond LIKE(TypedCol<T, ?> l, TypedCol<R, ?> r) { return add(cmp(typedCol(l), "like", typedCol(r), Names.property(l))); }

	/**
	 * Pattern match ({@code like}) with an expression left operand.
	 *
	 * @param l the left operand; must not be {@code null}
	 * @param r the pattern; must not be {@code null}
	 * @return this context, for chaining
	 */
	public Cond LIKE(Object l, Object r) { return add(cmp(Expr.val(l), "like", r)); }

	/**
	 * Existence test ({@code exists (...)}).
	 *
	 * <p>Example: {@code $.EXISTS(SELECT(lit(1)).FROM(Order.class)...)} &rarr; {@code exists (select 1 ...)}.</p>
	 *
	 * @param subquery the sub-query (a {@link Q}); must not be {@code null}
	 * @return this context, for chaining
	 */
	public Cond EXISTS(Object subquery) {
		Expr s = Expr.val(subquery);
		return add(Expr.of(c -> "exists " + s.render(c)));
	}

	/**
	 * Null test ({@code is null}), e.g. {@code $.ISㅤNULL(Employee::managerId)} &rarr; {@code e.managerId is null}.
	 *
	 * @param c   the column getter; must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return this context, for chaining
	 */
	public <T> Cond ISㅤNULL(TypedCol<T, ?> c) { Expr e = typedCol(c); return add(Expr.of(x -> e.render(x) + " is null")); }

	/**
	 * Not-null test ({@code is not null}).
	 *
	 * @param c   the column getter; must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return this context, for chaining
	 */
	public <T> Cond ISㅤNOTㅤNULL(TypedCol<T, ?> c) { Expr e = typedCol(c); return add(Expr.of(x -> e.render(x) + " is not null")); }

	/**
	 * Empty-collection test ({@code is empty}).
	 *
	 * @param c   the collection-valued column getter; must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return this context, for chaining
	 */
	public <T> Cond ISㅤEMPTY(TypedCol<T, ?> c) { Expr e = typedCol(c); return add(Expr.of(x -> e.render(x) + " is empty")); }

	/**
	 * Non-empty-collection test ({@code is not empty}), e.g. {@code $.ISㅤNOTㅤEMPTY(Customer::orders)}
	 * &rarr; {@code c.orders is not empty}.
	 *
	 * @param c   the collection-valued column getter; must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return this context, for chaining
	 */
	public <T> Cond ISㅤNOTㅤEMPTY(TypedCol<T, ?> c) { Expr e = typedCol(c); return add(Expr.of(x -> e.render(x) + " is not empty")); }

	/**
	 * Non-empty-collection test ({@code is not empty}) with an expression operand.
	 *
	 * @param c the collection-valued expression; must not be {@code null}
	 * @return this context, for chaining
	 */
	public Cond ISㅤNOTㅤEMPTY(Object c) { Expr e = Expr.val(c); return add(Expr.of(x -> e.render(x) + " is not empty")); }

	/**
	 * Collection membership ({@code value member of collection}), e.g.
	 * {@code $.MEMBERㅤOF(param("product"), typedCol(Customer::wishlist))} &rarr; {@code :product member of c.wishlist}.
	 *
	 * @param value      the element expression (literal/{@link Linq#param(String)}); must not be {@code null}
	 * @param collection the collection-valued expression; must not be {@code null}
	 * @return this context, for chaining
	 */
	public Cond MEMBERㅤOF(Object value, Object collection) {
		Expr v = Expr.val(value);
		Expr coll = Expr.val(collection);
		return add(Expr.of(c -> v.render(c) + " member of " + coll.render(c)));
	}

	// ===== logical connectors =====

	/**
	 * Sets the connector for the next predicate to {@code and} (the default).
	 *
	 * @return this context, for chaining
	 */
	public Cond ㅤANDㅤ() { pending = "and"; return this; }

	/**
	 * Sets the connector for the next predicate to {@code and} and supplies the left operand,
	 * to be finished by a bare comparison operator, e.g. {@code AND(SUM(Order::total)).ᐳ(1000)}
	 * &rarr; {@code and sum(o.total) > 1000}.
	 *
	 * @param left the left operand of the next predicate; must not be {@code null}
	 * @return this context, for chaining
	 */
	public Cond ㅤANDㅤ(Object left) { pending = "and"; pendingLeft = Expr.val(left); return this; }

	/**
	 * Sets the connector for the next predicate to {@code and} and supplies a bare column as the left
	 * operand — see {@link #ㅤANDㅤ(Object)}.
	 *
	 * @param left the left operand's column getter (method reference); must not be {@code null}
	 * @param <T>  the entity type owning the column
	 * @return this context, for chaining
	 */
	public <T> Cond ㅤANDㅤ(TypedCol<T, ?> left) { pending = "and"; pendingLeft = typedCol(left); return this; }

	/**
	 * Sets the connector for the next predicate to {@code or}.
	 *
	 * @return this context, for chaining
	 */
	public Cond OR() { pending = "or"; return this; }

	// ===== flat combinators (used by Linq.AND/OR/NOT) =====
	static Cond combine(String op, Cond[] parts) {
		Expr combined = null;
		for (Cond p : parts) {
			combined = (combined == null) ? p.expr : Expr.bin(combined, op, p.expr);
		}
		Expr inner = combined;
		Cond result = new Cond();
		result.expr = (inner == null) ? null : Expr.of(c -> "(" + inner.render(c) + ")");
		return result;
	}

	static Cond negate(Cond c) {
		Expr e = c.expr;
		Cond result = new Cond();
		result.expr = Expr.of(ctx -> "not (" + e.render(ctx) + ")");
		return result;
	}
}
