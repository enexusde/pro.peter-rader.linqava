/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package pro.peter_rader.linqava;

/**
 * A single scalar/aggregate selection whose Java result type is statically known (e.g.
 * {@code count(*)} always yields a {@link Long}). Carries that type alongside the rendered
 * expression, mirroring {@link EntityExpr}, so a query selecting exactly one such value can return a
 * typed result list via {@link ScalarQ#via(jakarta.persistence.EntityManager)} without a cast or an
 * explicit {@code resultType} argument.
 *
 * <p>
 * The comparison operators inherited from {@link Expr} ({@code ㅤᆖㅤ}, {@code ㅤᐸㅤ}, {@code ㅤᐳㅤ},
 * {@code ᐸᆖ}, {@code ᐳᆖ}, {@code ᐸᐳ}) are overridden here to return a {@code ScalarExpr<Boolean>}
 * instead of a bare {@link Expr}, so a comparison built from a scalar keeps threading its (now
 * {@code Boolean}) type through {@link #ㅤAS(String)} and {@link Linq#SELECTㅤ(ScalarExpr)} all the
 * way to {@link ScalarQ#via(jakarta.persistence.EntityManager)}:
 * </p>
 *
 * <pre>{@code
 * boolean compound = SELECT(COUNTㅤꁘ().ㅤᐳㅤ(1).ㅤANDㅤ(COUNTㅤꁘ()).ㅤᐸㅤ(1)).FROM(SQLDatabaseColumn.class)
 *         .WHERE(...).via(entityManager);
 * }</pre>
 *
 * @param <T> the Java type of the scalar value
 */
public final class ScalarExpr<T> extends Expr {

	final Class<T> type;
	private final Expr delegate;

	ScalarExpr(Class<T> type, Expr delegate) {
		this.type = type;
		this.delegate = delegate;
	}

	@Override
	String render(RenderCtx ctx) {
		return delegate.render(ctx);
	}

	@Override
	public ScalarExpr<Boolean> ㅤᆖㅤ(Object other) {
		return bool(super.ㅤᆖㅤ(other));
	}

	@Override
	public ScalarExpr<Boolean> ㅤᐸㅤ(Object other) {
		return bool(super.ㅤᐸㅤ(other));
	}

	@Override
	public ScalarExpr<Boolean> ㅤᐳㅤ(Object other) {
		return bool(super.ㅤᐳㅤ(other));
	}

	@Override
	public ScalarExpr<Boolean> ᐸᆖ(Object other) {
		return bool(super.ᐸᆖ(other));
	}

	@Override
	public ScalarExpr<Boolean> ᐳᆖ(Object other) {
		return bool(super.ᐳᆖ(other));
	}

	@Override
	public ScalarExpr<Boolean> ᐸᐳ(Object other) {
		return bool(super.ᐸᐳ(other));
	}

	@Override
	public ScalarExpr<T> ㅤAS(String alias) {
		return new ScalarExpr<>(type, super.ㅤAS(alias));
	}

	/**
	 * Combines this boolean expression with another, joined by {@code and}; follow with a comparison
	 * operator on the returned {@link WhereStep} to supply the right-hand predicate, e.g.
	 * {@code COUNTㅤꁘ().ㅤᐳㅤ(1).ㅤANDㅤ(COUNTㅤꁘ()).ㅤᐸㅤ(1)} &rarr; {@code count(*) > 1 and count(*) < 1}.
	 * Only meaningful when {@code T} is {@link Boolean} (i.e. {@code this} is itself the result of a
	 * comparison); for {@code WHERE}/{@code HAVING} predicates use {@link Cond}'s {@code ㅤANDㅤ}
	 * instead.
	 *
	 * @param left the left operand of the next comparison; must not be {@code null}
	 * @return the pending comparison, awaiting an operator
	 */
	public WhereStep<ScalarExpr<Boolean>> ㅤANDㅤ(Object left) {
		return combine(left, "and");
	}

	/**
	 * Combines this boolean expression with another, joined by {@code or}; follow with a comparison
	 * operator on the returned {@link WhereStep} to supply the right-hand predicate. Only meaningful
	 * when {@code T} is {@link Boolean}; for {@code WHERE}/{@code HAVING} predicates use
	 * {@link Cond}'s {@code ㅤORㅤ} instead.
	 *
	 * @param left the left operand of the next comparison; must not be {@code null}
	 * @return the pending comparison, awaiting an operator
	 */
	public WhereStep<ScalarExpr<Boolean>> ㅤORㅤ(Object left) {
		return combine(left, "or");
	}

	private WhereStep<ScalarExpr<Boolean>> combine(Object left, String connector) {
		Expr self = this;
		return new WhereStep<>(Expr.val(left), null, connector,
				(predicate, conn) -> new ScalarExpr<>(Boolean.class, Expr.bin(self, conn, predicate)));
	}

	private static ScalarExpr<Boolean> bool(Expr rendered) {
		return new ScalarExpr<>(Boolean.class, rendered);
	}
}
