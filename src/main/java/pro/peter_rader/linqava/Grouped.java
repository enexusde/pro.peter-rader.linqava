/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package pro.peter_rader.linqava;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;

/**
 * The phase right after {@code GROUPㅤBY(...)} (see {@link Q#GROUPㅤBY(Object...)}): the columns to
 * group by have been named, and only the clauses that may legally follow a {@code group by} in HQL —
 * {@code HAVING}, {@code ORDER BY}, {@code LIMIT}, {@code OFFSET}, {@code FLUSHㅤMODE},
 * {@code UNION ALL} — remain available. Deliberately does <em>not</em> re-expose {@code GROUPㅤBY}:
 * a query has exactly one {@code group by} clause, so a second, accidental call — easy to write by
 * mistake, e.g. {@code GROUPㅤBY(a).GROUPㅤBY(b)} instead of {@code GROUPㅤBY(a, b)} — fails to
 * compile instead of silently merging into the same clause.
 *
 * <p>
 * Each method here delegates to the wrapped {@link Q}, so {@link Q#getHql()}/
 * {@link Q#via(EntityManager) via} render and execute exactly as if the clauses had been called on
 * {@code Q} directly — only the fluent chain's static type changes.
 * </p>
 *
 * @param <E> the selected entity type, threaded through from the wrapped {@link Q}
 */
public final class Grouped<E> {

	private final Q<E> q;

	Grouped(Q<E> q) {
		this.q = q;
	}

	/** The wrapped {@link Q}, for code that needs to treat this as a plain {@code Q<?>} (e.g. as a
	 *  CTE definition or scalar sub-query) — see {@link WithStep#WITH(String, Grouped)}. */
	Q<E> unwrap() {
		return q;
	}

	/**
	 * The {@code having} clause from a pre-built predicate — see {@link Q#HAVING(Cond)}.
	 *
	 * @param predicate the condition; must not be {@code null}
	 * @return this builder, for chaining
	 */
	public Grouped<E> HAVING(Cond predicate) {
		q.HAVING(predicate);
		return this;
	}

	/**
	 * The {@code having} clause, started from an arbitrary left expression — see
	 * {@link Q#HAVING(Object)}.
	 *
	 * @param left the left operand; must not be {@code null}
	 * @return the pending comparison, awaiting an operator
	 */
	public WhereStep<Grouped<E>> HAVING(Object left) {
		Expr l = Expr.val(left);
		return new WhereStep<>(l, null, "and", (predicate, connector) -> {
			q.appendHaving(predicate, connector);
			return this;
		});
	}

	/**
	 * The {@code order by} clause — see {@link Q#ㅤORDERㅤBYㅤ(Object...)}.
	 *
	 * @param cols the ordering expressions, in order; must not be {@code null} or empty
	 * @return this builder, for chaining
	 */
	public Grouped<E> ㅤORDERㅤBYㅤ(Object... cols) {
		q.ㅤORDERㅤBYㅤ(cols);
		return this;
	}

	/**
	 * The {@code limit} clause — see {@link Q#LIMIT(int)}.
	 *
	 * @param maxResults the maximum number of rows to return; must not be negative
	 * @return this builder, for chaining
	 */
	public Grouped<E> LIMIT(int maxResults) {
		q.LIMIT(maxResults);
		return this;
	}

	/**
	 * The {@code offset} clause — see {@link Q#OFFSET(int)}.
	 *
	 * @param firstResult the zero-based index of the first row to return; must not be negative
	 * @return this builder, for chaining
	 */
	public Grouped<E> OFFSET(int firstResult) {
		q.OFFSET(firstResult);
		return this;
	}

	/**
	 * Overrides the flush mode for {@link #via(EntityManager)}/{@link #via(EntityManager, Class)} — see
	 * {@link Q#FLUSHㅤMODE(FlushModeType)}.
	 *
	 * @param mode the flush mode to apply; must not be {@code null}
	 * @return this builder, for chaining
	 */
	public Grouped<E> FLUSHㅤMODE(FlushModeType mode) {
		q.FLUSHㅤMODE(mode);
		return this;
	}

	/**
	 * Appends a {@code union all} with another query — see {@link Q#UNIONㅤALL(Q)}.
	 *
	 * @param other the query to append; must not be {@code null}
	 * @return this builder, for chaining
	 */
	public Grouped<E> UNIONㅤALL(Q<?> other) {
		q.UNIONㅤALL(other);
		return this;
	}

	/**
	 * The rendered HQL for this query — see {@link Q#getHql()}.
	 *
	 * @return the HQL string
	 */
	public String getHql() {
		return q.getHql();
	}

	String hqlFor(ParamCollector collector) {
		return q.hqlFor(collector);
	}

	/**
	 * Executes this query as a typed entity query and returns its result list — see
	 * {@link Q#via(EntityManager)}.
	 *
	 * @param em the JPA entity manager used to create and run the query; must not be {@code null}
	 * @return the (possibly empty) list of entities; never {@code null}
	 */
	public Iterable<E> via(EntityManager em) {
		return q.via(em);
	}

	/**
	 * Executes this query as a typed scalar/tuple/DTO projection and returns its result list — see
	 * {@link Q#via(EntityManager, Class)}. This is the common case for a grouped query, whose
	 * projection is almost always a tuple/aggregate rather than a whole entity.
	 *
	 * @param em         the JPA entity manager used to create and run the query; must not be
	 *                   {@code null}
	 * @param resultType the expected shape of each result row; must not be {@code null}
	 * @param <T>        the result row type
	 * @return the (possibly empty) list of projected rows; never {@code null}
	 */
	public <T> List<T> via(EntityManager em, Class<T> resultType) {
		return q.via(em, resultType);
	}
}
