/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _ 
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package linqava;

/**
 * A whole-entity selection (e.g. {@code SELECT o}). It renders like any other expression but also
 * carries the selected entity {@link Class}, so a query projecting exactly one entity can return a
 * typed result list via {@link Q#via(jakarta.persistence.EntityManager)}.
 */
final class EntityExpr extends Expr {

	final Class<?> type;
	private final Expr delegate;

	EntityExpr(Class<?> type, Expr delegate) {
		this.type = type;
		this.delegate = delegate;
	}

	@Override
	String render(RenderCtx ctx) {
		return delegate.render(ctx);
	}
}
