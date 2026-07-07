/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _ 
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package pro.peter_rader.linqava;

/**
 * A query expression node (column, literal, function call, arithmetic result, window or alias).
 * Every node knows how to render itself to HQL given a {@link RenderCtx}.
 *
 * <p>The arithmetic operators carry Unicode glyphs that stand in for the SQL math operators (they
 * are valid Java identifiers, unlike {@code + - * /}): {@code ᐩ} (+), {@code ｰ} (-), {@code ᚷ} (*),
 * {@code ノ} (/). The comparison glyphs {@code ㅤᆖㅤ} (=), {@code ㅤᐸㅤ} (&lt;), {@code ㅤᐳㅤ} (&gt;),
 * {@code ᐸᆖ} (&lt;=), {@code ᐳᆖ} (&gt;=), {@code ᐸᐳ} (&lt;&gt;) are likewise available for building
 * boolean expressions that appear in a {@code SELECT} list rather than a {@code WHERE}/
 * {@code HAVING} predicate (see {@link Cond} for the latter); when the receiver is a
 * {@link ScalarExpr}, these are overridden to return a {@code ScalarExpr<Boolean>}, threading
 * {@code Boolean} through to {@link ScalarQ#via(jakarta.persistence.EntityManager)} — see
 * {@link ScalarExpr#ㅤANDㅤ(Object)}/{@link ScalarExpr#ㅤORㅤ(Object)} for combining several of
 * them.</p>
 */
public abstract class Expr {

	/** Creates an expression node. */
	Expr() {
	}

	abstract String render(RenderCtx ctx);

	// --- arithmetic (glyph operators) ---

	/**
	 * Addition ({@code +}), e.g. {@code col("h", "depth").ᐩ(1)} &rarr; {@code h.depth + 1}.
	 *
	 * @param other the right operand (Expr/literal); must not be {@code null}
	 * @return a new expression
	 */
	public Expr ᐩ(Object other) { return bin(this, "+", val(other)); }

	/**
	 * Subtraction ({@code -}).
	 *
	 * @param other the right operand; must not be {@code null}
	 * @return a new expression
	 */
	public Expr ｰ(Object other) { return bin(this, "-", val(other)); }

	/**
	 * Multiplication ({@code *}).
	 *
	 * @param other the right operand; must not be {@code null}
	 * @return a new expression
	 */
	public Expr ᚷ(Object other) { return bin(this, "*", val(other)); }

	/**
	 * Division ({@code /}).
	 *
	 * @param other the right operand; must not be {@code null}
	 * @return a new expression
	 */
	public Expr ノ(Object other) { return bin(this, "/", val(other)); }

	// --- comparison (glyph operator, for boolean expressions in a SELECT list) ---

	/**
	 * Equality ({@code =}) as a projectable boolean expression, e.g.
	 * {@code COUNTㅤꁘ().ㅤᆖㅤ(0)} &rarr; {@code count(*) = 0}. For {@code WHERE}/{@code HAVING}
	 * predicates use {@link Cond}'s {@code ᆖ} instead.
	 *
	 * @param other the right operand (Expr/literal); must not be {@code null}
	 * @return a new expression
	 */
	public Expr ㅤᆖㅤ(Object other) { return bin(this, "=", val(other)); }

	/**
	 * Less-than ({@code <}) as a projectable boolean expression, e.g.
	 * {@code COUNTㅤꁘ().ㅤᐸㅤ(3)} &rarr; {@code count(*) < 3}. For {@code WHERE}/{@code HAVING}
	 * predicates use {@link Cond}'s {@code ᐸ} instead.
	 *
	 * @param other the right operand (Expr/literal); must not be {@code null}
	 * @return a new expression
	 */
	public Expr ㅤᐸㅤ(Object other) { return bin(this, "<", val(other)); }

	/**
	 * Greater-than ({@code >}) as a projectable boolean expression, e.g.
	 * {@code COUNTㅤꁘ().ㅤᐳㅤ(1)} &rarr; {@code count(*) > 1}. For {@code WHERE}/{@code HAVING}
	 * predicates use {@link Cond}'s {@code ᐳ} instead.
	 *
	 * @param other the right operand (Expr/literal); must not be {@code null}
	 * @return a new expression
	 */
	public Expr ㅤᐳㅤ(Object other) { return bin(this, ">", val(other)); }

	/**
	 * Less-than-or-equal ({@code <=}) as a projectable boolean expression. For
	 * {@code WHERE}/{@code HAVING} predicates use {@link Cond}'s {@code ᐸᆖ} instead.
	 *
	 * @param other the right operand (Expr/literal); must not be {@code null}
	 * @return a new expression
	 */
	public Expr ᐸᆖ(Object other) { return bin(this, "<=", val(other)); }

	/**
	 * Greater-than-or-equal ({@code >=}) as a projectable boolean expression. For
	 * {@code WHERE}/{@code HAVING} predicates use {@link Cond}'s {@code ᐳᆖ} instead.
	 *
	 * @param other the right operand (Expr/literal); must not be {@code null}
	 * @return a new expression
	 */
	public Expr ᐳᆖ(Object other) { return bin(this, ">=", val(other)); }

	/**
	 * Not-equal ({@code <>}) as a projectable boolean expression. For
	 * {@code WHERE}/{@code HAVING} predicates use {@link Cond}'s {@code ᐸᐳ} instead.
	 *
	 * @param other the right operand (Expr/literal); must not be {@code null}
	 * @return a new expression
	 */
	public Expr ᐸᐳ(Object other) { return bin(this, "<>", val(other)); }

	// --- ordering / windowing / aliasing ---

	/**
	 * Descending order marker, e.g. {@code col(Order::total).DESC()} &rarr; {@code o.total desc}.
	 *
	 * @return a new expression
	 */
	public Expr DESC() { Expr s = this; return of(c -> s.render(c) + " desc"); }

	/**
	 * Ascending order marker ({@code asc}).
	 *
	 * @return a new expression
	 */
	public Expr ASC() { Expr s = this; return of(c -> s.render(c) + " asc"); }

	/**
	 * Field/column alias, e.g. {@code col(User::id).AS("id")} or {@code SUM(...).AS("total")}.
	 *
	 * @param alias the alias name; must not be {@code null} or blank
	 * @return a new expression
	 */
	public Expr ㅤAS(String alias) { Expr s = this; return of(c -> s.render(c) + " as " + alias); }

	/**
	 * Window clause, e.g. {@code ROW_NUMBER().OVER(PARTITIONㅤBY(...))} &rarr; {@code row_number() over (...)}.
	 *
	 * @param window the window specification (see {@link Linq#ㅤPARTITIONㅤBYㅤ(Object...)}); must not be {@code null}
	 * @return a new expression
	 */
	public Expr OVER(Expr window) { Expr s = this; return of(c -> s.render(c) + " over (" + window.render(c) + ")"); }

	/**
	 * Window/inline ordering by arbitrary keys ({@code order by ...}).
	 *
	 * @param keys the ordering expressions, in order; must not be {@code null} or empty
	 * @return a new expression
	 */
	public Expr ORDERㅤBY(Object... keys) { Expr s = this; return of(c -> s.render(c) + " order by " + list(c, keys)); }

	/**
	 * Window ordering by a bare column reference, e.g.
	 * {@code PARTITIONㅤBY(...).ORDERㅤBY(Order::total).DESC()} &rarr; {@code partition by ... order by o.total desc}.
	 *
	 * @param key the column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return a new expression; chain {@link #DESC()}/{@link #ASC()} for direction
	 */
	public <T> Expr ORDERㅤBY(Col<T> key) { Expr s = this; Expr k = col(key); return of(c -> s.render(c) + " order by " + k.render(c)); }

	/**
	 * Member access on a {@code TREAT(...)} result, e.g.
	 * {@code TREAT(p, CreditCardPayment.class).ᐧ(CreditCardPayment::cardType)} &rarr; {@code treat(...).cardType}.
	 *
	 * @param getter the subtype field getter (method reference); must not be {@code null}
	 * @param <T>    the subtype owning the getter
	 * @return a new expression
	 */
	public <T> Expr ᐧ(Col<T> getter) {
		Expr s = this;
		String prop = Names.property(getter);
		return of(c -> s.render(c) + "." + prop);
	}

	// ===== internal helpers (package-private, shared with Linq/Cond/Case) =====

	interface R {
		String render(RenderCtx ctx);
	}

	static Expr of(R r) {
		return new Expr() {
			@Override
			String render(RenderCtx ctx) {
				return r.render(ctx);
			}
		};
	}

	static Expr bin(Expr a, String op, Expr b) {
		return of(c -> a.render(c) + " " + op + " " + b.render(c));
	}

	/** A column reference resolved to {@code alias.property} in the current query. */
	static Expr col(Col<?> ref) {
		String prop = Names.property(ref);
		String entity = Names.entity(ref);
		return of(c -> {
			String a = c.aliasFor(entity);
			return a == null ? prop : a + "." + prop;
		});
	}

	/** Coerce an arbitrary value into an expression: Expr as-is, sub-query in parentheses,
	 *  everything else a {@link LiteralExpr}. */
	static Expr val(Object o) {
		return val(o, null);
	}

	/**
	 * Like {@link #val(Object)}, but attaches a bind-parameter name hint for {@link Q#via} — see
	 * {@link LiteralExpr}. Callers that know which column {@code o} is being compared against (e.g.
	 * {@code WHERE(Order::total).ᐳ(100)}) pass that column's property name; everyone else passes
	 * {@code null} and gets the numbered fallback.
	 */
	static Expr val(Object o, String hint) {
		if (o instanceof Expr) {
			return (Expr) o;
		}
		if (o instanceof Q) {
			Q q = (Q) o;
			return of(c -> "(" + q.hqlFor(c.collector()) + ")");
		}
		return new LiteralExpr(o, hint);
	}

	/**
	 * A literal value. Renders as a {@code :name} bind parameter when the active {@link RenderCtx}
	 * carries a {@link ParamCollector} (i.e. while {@link Q#via} builds its parameterized query) and
	 * the value isn't {@code null}; otherwise renders inline exactly like {@code getHql()} always has
	 * (quoted for {@link String}, verbatim via {@link String#valueOf(Object)} otherwise — {@code null}
	 * always renders as the SQL {@code null} literal). {@code hint}, when non-{@code null}, names the
	 * invented bind parameter after the compared-against column instead of a bare counter.
	 */
	static final class LiteralExpr extends Expr {

		private final Object value;
		private final String hint;

		LiteralExpr(Object value, String hint) {
			this.value = value;
			this.hint = hint;
		}

		@Override
		String render(RenderCtx ctx) {
			ParamCollector collector = ctx.collector();
			if (value != null && collector != null) {
				return ":" + collector.next(value, hint);
			}
			if (value instanceof String) {
				return "'" + value + "'";
			}
			return String.valueOf(value);
		}
	}

	static String list(RenderCtx ctx, Object[] items) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < items.length; i++) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append(val(items[i]).render(ctx));
		}
		return sb.toString();
	}
}
