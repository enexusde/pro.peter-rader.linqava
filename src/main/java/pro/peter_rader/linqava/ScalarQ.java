/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package pro.peter_rader.linqava;

import java.util.Map;
import java.util.Objects;

import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.TypedQuery;

/**
 * A query selecting a single scalar/aggregate value whose Java result type is statically known (see
 * {@link Linq#COUNTㅤꁘ()}), e.g. {@code select count(*) from Order}. Started via
 * {@link Linq#SELECTㅤ(ScalarExpr)}.{@link ScalarSelectStep#ㅤFROMㅤ(Class) FROM(...)}.
 *
 * <p>
 * Unlike {@link Q}, whose {@link Q#via(EntityManager)} always returns a result list, a bare aggregate
 * with no {@code group by} is guaranteed by HQL/JPQL semantics to yield exactly one row, so
 * {@link #via(EntityManager)} returns that single value directly:
 * </p>
 *
 * <pre>{@code
 * long orphaned = SELECT(COUNTㅤꁘ()).FROM(Order.class)
 *         .WHERE(Order::customer).ISㅤNULL()
 *         .via(entityManager);
 * }</pre>
 *
 * <p>
 * Offers the same {@code AS}/{@code JOIN}/{@code WHERE}/{@code AND}/{@code OR} clauses as {@link Q}
 * (see there for the semantics of each), each delegating to an internal {@link Q} and returning
 * {@code this} so the fluent chain stays on {@code ScalarQ} all the way to {@link #via(EntityManager)}.
 * Not offered: {@code GROUP BY}/{@code HAVING}/{@code ORDER BY}/{@code UNION ALL} and fetch-joins —
 * these either produce more than one row (contradicting the single-scalar-result guarantee) or make no
 * sense without a whole-entity projection to fetch into; use {@link Q} and
 * {@link Q#via(EntityManager, Class)} for those instead.
 * </p>
 *
 * @param <T> the Java type of the scalar value
 */
public final class ScalarQ<T> {

	private final Q<T> q;
	private final Class<T> type;
	private FlushModeType flushMode;

	ScalarQ(Q<T> q, Class<T> type) {
		this.q = q;
		this.type = type;
	}

	/**
	 * Table/range-variable alias for the most recent {@code FROM}/{@code JOIN} — see
	 * {@link Q#ㅤAS(String)}.
	 *
	 * @param alias the alias name; must not be {@code null} or blank
	 * @return this builder, for chaining
	 */
	public ScalarQ<T> ㅤAS(String alias) {
		q.ㅤAS(alias);
		return this;
	}

	/**
	 * Inner-joins an entity — see {@link Q#JOIN(Class)}.
	 *
	 * @param entity the joined entity class; must not be {@code null}
	 * @return this builder, for chaining
	 */
	public ScalarQ<T> JOIN(Class<?> entity) {
		q.JOIN(entity);
		return this;
	}

	/**
	 * Inner-joins a CTE/derived table by name — see {@link Q#JOIN(String)}.
	 *
	 * @param cte the CTE/derived-table name; must not be {@code null} or blank
	 * @return this builder, for chaining
	 */
	public ScalarQ<T> JOIN(String cte) {
		q.JOIN(cte);
		return this;
	}

	/**
	 * Inner-joins along an association path — see {@link Q#JOIN(TypedCol)}.
	 *
	 * @param path the association getter (method reference); must not be {@code null}
	 * @param <A>  the owning entity type
	 * @return this builder, for chaining
	 */
	public <A> ScalarQ<T> JOIN(TypedCol<A, ?> path) {
		q.JOIN(path);
		return this;
	}

	/**
	 * Inner-joins along an alias-qualified association path — see {@link Q#JOIN(Object)}.
	 *
	 * @param path the alias-qualified path, typically {@link Linq#typedCol(String, TypedCol)}; must not be
	 *             {@code null}
	 * @return this builder, for chaining
	 */
	public ScalarQ<T> JOIN(Object path) {
		q.JOIN(path);
		return this;
	}

	/**
	 * Left-outer-joins an entity — see {@link Q#LEFTㅤJOIN(Class)}.
	 *
	 * @param entity the joined entity class; must not be {@code null}
	 * @return this builder, for chaining
	 */
	public ScalarQ<T> LEFTㅤJOIN(Class<?> entity) {
		q.LEFTㅤJOIN(entity);
		return this;
	}

	/**
	 * Left-outer-joins a CTE/derived table by name — see {@link Q#LEFTㅤJOIN(String)}.
	 *
	 * @param cte the CTE/derived-table name; must not be {@code null} or blank
	 * @return this builder, for chaining
	 */
	public ScalarQ<T> LEFTㅤJOIN(String cte) {
		q.LEFTㅤJOIN(cte);
		return this;
	}

	/**
	 * Left-outer-joins along an association path — see {@link Q#LEFTㅤJOIN(TypedCol)}.
	 *
	 * @param path the association getter (method reference); must not be {@code null}
	 * @param <A>  the owning entity type
	 * @return this builder, for chaining
	 */
	public <A> ScalarQ<T> LEFTㅤJOIN(TypedCol<A, ?> path) {
		q.LEFTㅤJOIN(path);
		return this;
	}

	/**
	 * Left-outer-joins along an alias-qualified association path — see
	 * {@link Q#LEFTㅤJOIN(Object)}.
	 *
	 * @param path the alias-qualified path, typically {@link Linq#typedCol(String, TypedCol)}; must not be
	 *             {@code null}
	 * @return this builder, for chaining
	 */
	public ScalarQ<T> LEFTㅤJOIN(Object path) {
		q.LEFTㅤJOIN(path);
		return this;
	}

	/**
	 * The {@code on} condition for the most recently added join, from a pre-built predicate — see
	 * {@link Q#ㅤONㅤ(Cond)}.
	 *
	 * @param predicate the join condition; must not be {@code null}
	 * @return this builder, for chaining
	 * @throws IndexOutOfBoundsException if no join has been added yet
	 */
	public ScalarQ<T> ㅤONㅤ(Cond predicate) {
		q.ㅤONㅤ(predicate);
		return this;
	}

	/**
	 * The {@code where} clause from a pre-built predicate — see {@link Q#ㅤWHEREㅤ(Cond)}.
	 *
	 * @param predicate the condition; must not be {@code null}
	 * @return this builder, for chaining
	 */
	public ScalarQ<T> ㅤWHEREㅤ(Cond predicate) {
		q.ㅤWHEREㅤ(predicate);
		return this;
	}

	/**
	 * The {@code where} clause, started from a bare column — see {@link Q#ㅤWHEREㅤ(TypedCol)}.
	 *
	 * @param col the left column getter (method reference); must not be {@code null}
	 * @param <A> the entity type owning the column
	 * @return the pending comparison, awaiting an operator
	 */
	public <A> WhereStep<ScalarQ<T>> ㅤWHEREㅤ(TypedCol<A, ?> col) {
		return where(Expr.typedCol(col), Names.property(col), "and");
	}

	/**
	 * The {@code where} clause, started from an alias-qualified column — see
	 * {@link Q#ㅤWHEREㅤ(String, TypedCol)}.
	 *
	 * @param alias the range-variable alias to qualify the column with; must not be {@code null}
	 * @param col   the left column getter (method reference); must not be {@code null}
	 * @param <A>   the entity type owning the column
	 * @return the pending comparison, awaiting an operator
	 */
	public <A> WhereStep<ScalarQ<T>> ㅤWHEREㅤ(String alias, TypedCol<A, ?> col) {
		return where(Linq.typedCol(alias, col), Names.property(col), "and");
	}

	/**
	 * The {@code where} clause, started from an arbitrary left expression — see
	 * {@link Q#ㅤWHEREㅤ(Object)}.
	 *
	 * @param left the left operand; must not be {@code null}
	 * @return the pending comparison, awaiting an operator
	 */
	public WhereStep<ScalarQ<T>> ㅤWHEREㅤ(Object left) {
		return where(Expr.val(left), null, "and");
	}

	/**
	 * Appends another column-led predicate to the {@code where} clause, joined with {@code and} —
	 * see {@link Q#ㅤANDㅤ(TypedCol)}.
	 *
	 * @param col the left column getter (method reference); must not be {@code null}
	 * @param <A> the entity type owning the column
	 * @return the pending comparison, awaiting an operator
	 */
	public <A> WhereStep<ScalarQ<T>> ㅤANDㅤ(TypedCol<A, ?> col) {
		return where(Expr.typedCol(col), Names.property(col), "and");
	}

	/**
	 * Appends another alias-qualified column-led predicate, joined with {@code and} — see
	 * {@link Q#ㅤANDㅤ(String, TypedCol)}.
	 *
	 * @param alias the range-variable alias to qualify the column with; must not be {@code null}
	 * @param col   the left column getter (method reference); must not be {@code null}
	 * @param <A>   the entity type owning the column
	 * @return the pending comparison, awaiting an operator
	 */
	public <A> WhereStep<ScalarQ<T>> ㅤANDㅤ(String alias, TypedCol<A, ?> col) {
		return where(Linq.typedCol(alias, col), Names.property(col), "and");
	}

	/**
	 * Appends another expression-led predicate, joined with {@code and} — see
	 * {@link Q#ㅤANDㅤ(Object)}.
	 *
	 * @param left the left operand; must not be {@code null}
	 * @return the pending comparison, awaiting an operator
	 */
	public WhereStep<ScalarQ<T>> ㅤANDㅤ(Object left) {
		return where(Expr.val(left), null, "and");
	}

	/**
	 * Appends another column-led predicate to the {@code where} clause, joined with {@code or} —
	 * see {@link Q#ㅤORㅤ(TypedCol)}.
	 *
	 * @param col the left column getter (method reference); must not be {@code null}
	 * @param <A> the entity type owning the column
	 * @return the pending comparison, awaiting an operator
	 */
	public <A> WhereStep<ScalarQ<T>> ㅤORㅤ(TypedCol<A, ?> col) {
		return where(Expr.typedCol(col), Names.property(col), "or");
	}

	/**
	 * Appends another alias-qualified column-led predicate, joined with {@code or} — see
	 * {@link Q#ㅤORㅤ(String, TypedCol)}.
	 *
	 * @param alias the range-variable alias to qualify the column with; must not be {@code null}
	 * @param col   the left column getter (method reference); must not be {@code null}
	 * @param <A>   the entity type owning the column
	 * @return the pending comparison, awaiting an operator
	 */
	public <A> WhereStep<ScalarQ<T>> ㅤORㅤ(String alias, TypedCol<A, ?> col) {
		return where(Linq.typedCol(alias, col), Names.property(col), "or");
	}

	/**
	 * Appends another expression-led predicate, joined with {@code or} — see
	 * {@link Q#ㅤORㅤ(Object)}.
	 *
	 * @param left the left operand; must not be {@code null}
	 * @return the pending comparison, awaiting an operator
	 */
	public WhereStep<ScalarQ<T>> ㅤORㅤ(Object left) {
		return where(Expr.val(left), null, "or");
	}

	private WhereStep<ScalarQ<T>> where(Expr left, String leftHint, String connector) {
		return new WhereStep<>(left, leftHint, connector, (predicate, conn) -> {
			q.appendWhere(predicate, conn);
			return this;
		});
	}

	/**
	 * Overrides the {@link TypedQuery}'s flush mode for the {@link #via(EntityManager)} call on this
	 * query, instead of leaving it at the persistence context's default.
	 *
	 * @param mode the flush mode to apply; must not be {@code null}
	 * @return this builder, for chaining
	 */
	public ScalarQ<T> FLUSHㅤMODE(FlushModeType mode) {
		flushMode = Objects.requireNonNull(mode, "mode");
		return this;
	}

	/**
	 * The rendered HQL for this query, with every literal inlined — see {@link Q#getUnsafeHql()} for
	 * why that's unsafe with untrusted input and when to prefer
	 * {@link #via(jakarta.persistence.EntityManager)} instead.
	 *
	 * @return the HQL string
	 */
	public String getUnsafeHql() {
		return q.getUnsafeHql();
	}

	/**
	 * Executes this query and returns its single scalar result. Safe because a bare aggregate (no
	 * {@code group by}) always yields exactly one row under HQL/JPQL semantics, even over zero
	 * matching underlying rows — e.g. {@code count(*)} then yields {@code 0}.
	 *
	 * <p>
	 * Unlike {@link Q#getUnsafeHql()}, every literal value in the query (anything not wrapped in
	 * {@link Linq#param(String)}) is rendered as an invented {@code :name} bind parameter and passed
	 * to the {@link TypedQuery} via {@code setParameter} instead of being inlined into the HQL text —
	 * see {@link Q#via(EntityManager)} for the full rationale.
	 * </p>
	 *
	 * @param em the JPA entity manager used to create and run the query; must not be {@code null}
	 * @return the scalar result; never {@code null} for an aggregate such as {@code count(*)}
	 * @throws NullPointerException if {@code em} is {@code null}
	 */
	public T via(EntityManager em) {
		Objects.requireNonNull(em, "em");
		ParamCollector collector = new ParamCollector();
		String hql = q.hqlFor(collector);
		try {
			TypedQuery<T> tq = em.createQuery(hql, type);
			if (flushMode != null) {
				tq.setFlushMode(flushMode);
			}
			for (Map.Entry<String, Object> e : collector.params().entrySet()) {
				tq.setParameter(e.getKey(), e.getValue());
			}
			return tq.getSingleResult();
		} catch (RuntimeException e) {
			throw new RuntimeException(hql, e);
		}
	}
}
