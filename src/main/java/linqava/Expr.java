/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _ 
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package linqava;

/**
 * A query expression node (column, literal, function call, arithmetic result, window or alias).
 * Every node knows how to render itself to HQL given a {@link RenderCtx}.
 *
 * <p>The arithmetic operators carry Unicode glyphs that stand in for the SQL math operators (they
 * are valid Java identifiers, unlike {@code + - * /}): {@code ᐩ} (+), {@code ｰ} (-), {@code ᚷ} (*),
 * {@code ノ} (/).</p>
 */
public abstract class Expr {

	abstract String render(RenderCtx ctx);

	// --- arithmetic (glyph operators) ---

	/**
	 * Addition ({@code +}), e.g. {@code c("h", "depth").ᐩ(1)} &rarr; {@code h.depth + 1}.
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

	// --- ordering / windowing / aliasing ---

	/**
	 * Descending order marker, e.g. {@code c(Order::total).DESC()} &rarr; {@code o.total desc}.
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
	 * Field/column alias, e.g. {@code c(User::id).AS("id")} or {@code SUM(...).AS("total")}.
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
	 *  String as an HQL string literal, everything else via {@code toString()}. */
	static Expr val(Object o) {
		if (o instanceof Expr) {
			return (Expr) o;
		}
		if (o instanceof Q) {
			Q q = (Q) o;
			return of(c -> "(" + q.getHql() + ")");
		}
		if (o instanceof String) {
			return of(c -> "'" + o + "'");
		}
		return of(c -> String.valueOf(o));
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
