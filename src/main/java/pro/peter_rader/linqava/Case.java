/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _ 
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package pro.peter_rader.linqava;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Fluent, compile-time-safe {@code CASE WHEN ... THEN ... [ELSE ...] END} expression builder.
 *
 * <p>The builder is a <em>step builder</em>: each phase returns a distinct type that only exposes the
 * legal next call, so malformed expressions cannot be written. In particular {@code CASE().END()},
 * a {@code WHEN} without a following {@code THEN}, a duplicate {@code THEN} and a {@code THEN} without
 * a {@code WHEN} are all rejected by the compiler.</p>
 *
 * <p>Allowed flow: {@code CASE()} &rarr; {@link #WHEN(Cond) WHEN} &rarr; {@link Then#THEN(Object) THEN}
 * &rarr; (another {@code WHEN} | {@link Body#ELSE(Object) ELSE} | {@link Body#END() END}); after
 * {@code ELSE} only {@link End#END() END} remains.</p>
 *
 * <pre>{@code
 * CASE().WHEN(ᐳᆖ(Order::total, 1000)).THEN("GOLD")
 *       .WHEN(ᐳᆖ(Order::total, 100)).THEN("SILVER")
 *       .ELSE("BRONZE").END()
 * }</pre>
 */
public final class Case {

	private final List<Expr> whens = new ArrayList<>();
	private final List<Expr> thens = new ArrayList<>();
	private Expr elseValue;

	Case() {
	}

	/**
	 * Adds the first {@code WHEN condition} branch.
	 *
	 * @param condition the branch condition, e.g. {@code ᐳᆖ(Order::total, 1000)}; must not be {@code null}
	 * @return the next step, which requires a {@link Then#THEN(Object) THEN}
	 */
	public Then WHEN(Cond condition) {
		whens.add(condition.expr);
		return new Then();
	}

	private Expr build() {
		List<Expr> w = new ArrayList<>(whens);
		List<Expr> t = new ArrayList<>(thens);
		Expr e = elseValue;
		return Expr.of(c -> {
			StringBuilder sb = new StringBuilder("case");
			for (int i = 0; i < w.size(); i++) {
				sb.append(" when ").append(w.get(i).render(c)).append(" then ").append(t.get(i).render(c));
			}
			if (e != null) {
				sb.append(" else ").append(e.render(c));
			}
			return sb.append(" end").toString();
		});
	}

	/** Step right after a {@code WHEN}: a {@code THEN} result is required. */
	public final class Then {

		private Then() {
		}

		/**
		 * Supplies the result for the preceding {@link Case#WHEN(Function) WHEN}.
		 *
		 * @param value the result expression/literal, e.g. {@code "GOLD"} or {@code col(Order::total)};
		 *              must not be {@code null}
		 * @return the next step, from which another {@code WHEN}, an {@code ELSE} or {@code END} is allowed
		 */
		public Body THEN(Object value) {
			thens.add(Expr.val(value));
			return new Body();
		}

		/**
		 * Supplies the result for the preceding {@link Case#WHEN(Cond) WHEN} as a bare column reference.
		 *
		 * @param value the result column getter (method reference); must not be {@code null}
		 * @param <T>   the entity type owning the column
		 * @return the next step, from which another {@code WHEN}, an {@code ELSE} or {@code END} is allowed
		 */
		public <T> Body THEN(Col<T> value) {
			thens.add(Expr.col(value));
			return new Body();
		}
	}

	/** Step after a complete {@code WHEN ... THEN ...} pair: add another branch, an {@code ELSE}, or finish. */
	public final class Body {

		private Body() {
		}

		/**
		 * Adds a further {@code WHEN condition} branch.
		 *
		 * @param condition the branch condition, e.g. {@code ᐳᆖ(Order::total, 100)}; must not be {@code null}
		 * @return the next step, which requires a {@link Then#THEN(Object) THEN}
		 */
		public Then WHEN(Cond condition) {
			whens.add(condition.expr);
			return new Then();
		}

		/**
		 * Sets the optional {@code ELSE value} branch.
		 *
		 * @param value the default result expression/literal, e.g. {@code "BRONZE"} or {@code 0}; must not be {@code null}
		 * @return the final step, from which only {@link End#END() END} is allowed
		 */
		public End ELSE(Object value) {
			elseValue = Expr.val(value);
			return new End();
		}

		/**
		 * Finishes the expression without an {@code ELSE} branch ({@code ... end}).
		 *
		 * @return the {@code CASE} expression as an {@link Expr}
		 */
		public Expr END() {
			return build();
		}
	}

	/** Step after an {@code ELSE}: the expression can only be finished. */
	public final class End {

		private End() {
		}

		/**
		 * Finishes the expression ({@code ... end}).
		 *
		 * @return the {@code CASE} expression as an {@link Expr}
		 */
		public Expr END() {
			return build();
		}
	}
}
