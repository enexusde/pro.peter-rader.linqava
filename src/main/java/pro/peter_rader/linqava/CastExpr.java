/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package pro.peter_rader.linqava;

/**
 * A single, non-aggregate {@code cast(expr as targetType)} column whose Java result type is exactly
 * {@code targetType} itself — known statically at the {@link CastStep#ㅤASㅤ(Class)} call site, no
 * reflection needed. Mirrors {@link EntityExpr}, so a query projecting exactly this one cast column
 * can return a typed result list via {@link Q#via(jakarta.persistence.EntityManager)} without an
 * explicit {@code Class} argument, even though (unlike a bare aggregate) the query may return any
 * number of rows.
 *
 * @param <T> the Java type the cast expression is cast to
 */
final class CastExpr<T> extends Expr {

	final Class<T> type;
	private final Expr delegate;

	CastExpr(Class<T> type, Expr delegate) {
		this.type = type;
		this.delegate = delegate;
	}

	@Override
	String render(RenderCtx ctx) {
		return delegate.render(ctx);
	}
}
