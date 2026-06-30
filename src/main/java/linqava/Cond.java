/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _ 
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package linqava;

import java.util.function.Function;

/**
 * The condition context (the {@code $} of a {@code WHERE}/{@code ON}/{@code HAVING}/{@code WHEN}
 * lambda). Comparison operators are methods named with Unicode glyphs that are valid Java
 * identifiers: {@code ᆖ} (=), {@code ᐸ} (&lt;), {@code ᐳ} (&gt;), {@code ᐸᆖ} (&lt;=),
 * {@code ᐳᆖ} (&gt;=), {@code ᐸᐳ} (&lt;&gt;).
 *
 * <p>Each comparison/predicate has two overloads: a bare {@link Col} reference (type-checked against
 * its entity) as the left operand, or an {@link Object} ({@link Expr}, sub-query {@link Q}, literal
 * or {@link Linq#param parameter}).</p>
 *
 * <p>Predicates accumulate: each call appends its predicate to the context joined by the pending
 * connector (default {@code and}; switch with {@link #AND()} / {@link #OR()}). Example:</p>
 * <pre>{@code
 * $ -> $.ᆖ(Order::status, "PAID").AND().ᐳ(Order::total, 100)
 * // o.status = 'PAID' and o.total > 100
 * }</pre>
 *
 * <p>Null policy: no argument may be {@code null}. For null tests use {@link #IS‿NULL(Col)} /
 * {@link #IS‿NOT‿NULL(Col)} instead of comparing against {@code null}.</p>
 */
public final class Cond {

	Expr expr;
	private String pending = "and";

	private Cond add(Expr predicate) {
		expr = (expr == null) ? predicate : Expr.bin(expr, pending, predicate);
		pending = "and";
		return this;
	}

	private static <T> Expr col(Col<T> c) {
		String prop = Names.property(c);
		String entity = Names.entity(c);
		return Expr.of(ctx -> {
			String a = ctx.aliasFor(entity);
			return a == null ? prop : a + "." + prop;
		});
	}

	private static Expr cmp(Expr left, String op, Object right) {
		Expr r = Expr.val(right);
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
	public <T> Cond ᆖ(Col<T> l, Object r) { return add(cmp(col(l), "=", r)); }

	/**
	 * Equality ({@code =}) with an expression left operand.
	 *
	 * @param l the left operand; must not be {@code null}
	 * @param r the right operand; must not be {@code null}
	 * @return this context, for chaining
	 */
	public Cond ᆖ(Object l, Object r) { return add(cmp(Expr.val(l), "=", r)); }

	/**
	 * Less-than ({@code <}), e.g. {@code $.ᐸ(Order::discount, 5)} &rarr; {@code o.discount < 5}.
	 *
	 * @param l   the left column getter; must not be {@code null}
	 * @param r   the right operand; must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @return this context, for chaining
	 */
	public <T> Cond ᐸ(Col<T> l, Object r) { return add(cmp(col(l), "<", r)); }

	/**
	 * Less-than ({@code <}) with an expression left operand.
	 *
	 * @param l the left operand; must not be {@code null}
	 * @param r the right operand; must not be {@code null}
	 * @return this context, for chaining
	 */
	public Cond ᐸ(Object l, Object r) { return add(cmp(Expr.val(l), "<", r)); }

	/**
	 * Greater-than ({@code >}), e.g. {@code $.ᐳ(Order::total, 100)} &rarr; {@code o.total > 100}.
	 *
	 * @param l   the left column getter; must not be {@code null}
	 * @param r   the right operand; must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @return this context, for chaining
	 */
	public <T> Cond ᐳ(Col<T> l, Object r) { return add(cmp(col(l), ">", r)); }

	/**
	 * Greater-than ({@code >}) with an expression left operand.
	 *
	 * @param l the left operand; must not be {@code null}
	 * @param r the right operand; must not be {@code null}
	 * @return this context, for chaining
	 */
	public Cond ᐳ(Object l, Object r) { return add(cmp(Expr.val(l), ">", r)); }

	/**
	 * Less-than-or-equal ({@code <=}), e.g. {@code $.ᐸᆖ(Order::discount, 50)} &rarr; {@code o.discount <= 50}.
	 *
	 * @param l   the left column getter; must not be {@code null}
	 * @param r   the right operand; must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @return this context, for chaining
	 */
	public <T> Cond ᐸᆖ(Col<T> l, Object r) { return add(cmp(col(l), "<=", r)); }

	/**
	 * Less-than-or-equal ({@code <=}) with an expression left operand.
	 *
	 * @param l the left operand; must not be {@code null}
	 * @param r the right operand; must not be {@code null}
	 * @return this context, for chaining
	 */
	public Cond ᐸᆖ(Object l, Object r) { return add(cmp(Expr.val(l), "<=", r)); }

	/**
	 * Greater-than-or-equal ({@code >=}), e.g. {@code $.ᐳᆖ(Order::total, 1000)} &rarr; {@code o.total >= 1000}.
	 *
	 * @param l   the left column getter; must not be {@code null}
	 * @param r   the right operand; must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @return this context, for chaining
	 */
	public <T> Cond ᐳᆖ(Col<T> l, Object r) { return add(cmp(col(l), ">=", r)); }

	/**
	 * Greater-than-or-equal ({@code >=}) with an expression left operand.
	 *
	 * @param l the left operand; must not be {@code null}
	 * @param r the right operand; must not be {@code null}
	 * @return this context, for chaining
	 */
	public Cond ᐳᆖ(Object l, Object r) { return add(cmp(Expr.val(l), ">=", r)); }

	/**
	 * Not-equal ({@code <>}), e.g. {@code $.ᐸᐳ(Order::status, "CANCELLED")} &rarr; {@code o.status <> 'CANCELLED'}.
	 *
	 * @param l   the left column getter; must not be {@code null}
	 * @param r   the right operand; must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @return this context, for chaining
	 */
	public <T> Cond ᐸᐳ(Col<T> l, Object r) { return add(cmp(col(l), "<>", r)); }

	/**
	 * Not-equal ({@code <>}) with an expression left operand.
	 *
	 * @param l the left operand; must not be {@code null}
	 * @param r the right operand; must not be {@code null}
	 * @return this context, for chaining
	 */
	public Cond ᐸᐳ(Object l, Object r) { return add(cmp(Expr.val(l), "<>", r)); }

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
	public <T> Cond IN(Col<T> l, Object r) { return add(cmp(col(l), "in", r)); }

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
	public <T> Cond LIKE(Col<T> l, Object r) { return add(cmp(col(l), "like", r)); }

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
	 * Null test ({@code is null}), e.g. {@code $.IS‿NULL(Employee::managerId)} &rarr; {@code e.managerId is null}.
	 *
	 * @param c   the column getter; must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return this context, for chaining
	 */
	public <T> Cond IS‿NULL(Col<T> c) { Expr e = col(c); return add(Expr.of(x -> e.render(x) + " is null")); }

	/**
	 * Not-null test ({@code is not null}).
	 *
	 * @param c   the column getter; must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return this context, for chaining
	 */
	public <T> Cond IS‿NOT‿NULL(Col<T> c) { Expr e = col(c); return add(Expr.of(x -> e.render(x) + " is not null")); }

	/**
	 * Empty-collection test ({@code is empty}).
	 *
	 * @param c   the collection-valued column getter; must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return this context, for chaining
	 */
	public <T> Cond IS‿EMPTY(Col<T> c) { Expr e = col(c); return add(Expr.of(x -> e.render(x) + " is empty")); }

	/**
	 * Non-empty-collection test ({@code is not empty}), e.g. {@code $.IS‿NOT‿EMPTY(Customer::orders)}
	 * &rarr; {@code c.orders is not empty}.
	 *
	 * @param c   the collection-valued column getter; must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return this context, for chaining
	 */
	public <T> Cond IS‿NOT‿EMPTY(Col<T> c) { Expr e = col(c); return add(Expr.of(x -> e.render(x) + " is not empty")); }

	/**
	 * Non-empty-collection test ({@code is not empty}) with an expression operand.
	 *
	 * @param c the collection-valued expression; must not be {@code null}
	 * @return this context, for chaining
	 */
	public Cond IS‿NOT‿EMPTY(Object c) { Expr e = Expr.val(c); return add(Expr.of(x -> e.render(x) + " is not empty")); }

	/**
	 * Collection membership ({@code value member of collection}), e.g.
	 * {@code $.MEMBER‿OF(param("product"), c(Customer::wishlist))} &rarr; {@code :product member of c.wishlist}.
	 *
	 * @param value      the element expression (literal/{@link Linq#param(String)}); must not be {@code null}
	 * @param collection the collection-valued expression; must not be {@code null}
	 * @return this context, for chaining
	 */
	public Cond MEMBER‿OF(Object value, Object collection) {
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
	public Cond AND() { pending = "and"; return this; }

	/**
	 * Sets the connector for the next predicate to {@code or}.
	 *
	 * @return this context, for chaining
	 */
	public Cond OR() { pending = "or"; return this; }

	/**
	 * Appends a parenthesized sub-group joined with {@code and}.
	 *
	 * <p>For deeply nested boolean trees prefer the flat {@link Linq#AND(Cond...)} / {@link Linq#OR(Cond...)}.</p>
	 *
	 * @param group builds the sub-condition from a fresh {@link Cond}; must not be {@code null}
	 * @return this context, for chaining
	 */
	public Cond AND(Function<Cond, Cond> group) { pending = "and"; return add(grouped(group)); }

	/**
	 * Appends a parenthesized sub-group joined with {@code or}.
	 *
	 * @param group builds the sub-condition from a fresh {@link Cond}; must not be {@code null}
	 * @return this context, for chaining
	 */
	public Cond OR(Function<Cond, Cond> group) { pending = "or"; return add(grouped(group)); }

	/**
	 * Appends a negated parenthesized sub-group ({@code not (...)}).
	 *
	 * @param group builds the sub-condition from a fresh {@link Cond}; must not be {@code null}
	 * @return this context, for chaining
	 */
	public Cond NOT(Function<Cond, Cond> group) {
		Expr inner = grouped(group);
		return add(Expr.of(c -> "not " + inner.render(c)));
	}

	private static Expr grouped(Function<Cond, Cond> group) {
		Expr inner = group.apply(new Cond()).expr;
		return Expr.of(c -> "(" + inner.render(c) + ")");
	}

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
