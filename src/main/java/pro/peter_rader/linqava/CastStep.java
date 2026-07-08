/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package pro.peter_rader.linqava;

/**
 * The phase right after {@code CAST(expr)} (see {@link Linq#ㅤCASTㅤ(Object)}): the expression to cast
 * has been named, and the only legal next call is {@link #ㅤASㅤ(Class)} to supply the target type and
 * finish the function call — {@code CAST(Order::total, String.class)} cannot be written anymore.
 *
 * <p>Example: {@code CAST(Order::total).AS(String.class)} &rarr; {@code cast(o.total as String)}.</p>
 */
public final class CastStep {

	private final Expr expr;

	CastStep(Expr expr) {
		this.expr = expr;
	}

	/**
	 * Finishes the cast with its target type.
	 *
	 * <p>
	 * {@code targetType} is rendered as its simple name (e.g. {@code String.class} &rarr;
	 * {@code String}), which Hibernate resolves against its unified type system (Java type name, or a
	 * recognized cast-type keyword).
	 * </p>
	 *
	 * @param targetType the target Java type; must not be {@code null}
	 * @return the function call as an {@link Expr}
	 */
	public<T> Expr ㅤASㅤ(Class<?> targetType) {
		Expr e = expr;
		return Expr.of(ctx -> "cast(" + e.render(ctx) + " as " + targetType.getSimpleName() + ")");
	}
}
