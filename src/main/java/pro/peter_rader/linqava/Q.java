/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package pro.peter_rader.linqava;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 * A linqava query/statement — the builder reached <em>after</em> {@code FROM}.
 * Call {@link #getHql()} on the finished statement to obtain the corresponding
 * HQL string, or {@link #via(EntityManager)} to run it.
 *
 * <p>
 * The fluent entry points enforce a valid clause order at compile time:
 * {@link Linq#SELECT} returns a {@link SelectStep} that only offers
 * {@code FROM}, and {@code FROM} returns this {@code Q}, which no longer offers
 * {@code SELECT} or {@code FROM}. Hence {@code FROM(x).FROM(y)}, a double
 * {@code SELECT} and a missing {@code FROM} cannot be written.
 * </p>
 *
 * <p>
 * The type parameter {@code E} threads the selected entity type all the way
 * from {@link Linq#SELECT(Class)}/{@link Linq#DISTINCT(Class)} through every
 * clause to {@link #via}, so a single-entity query yields a typed result list
 * with no cast anywhere. Queries that don't select a whole entity (scalar/tuple
 * projections) carry no meaningful {@code E}; {@link #via} rejects those at
 * runtime.
 * </p>
 *
 * <p>
 * SQL keywords are methods; multi-word keywords are a single method whose words
 * are joined by the connector glyph {@code ㅤ} (U+203F UNDERTIE), e.g.
 * {@code LEFTㅤJOIN}, {@code GROUPㅤBY}, {@code ORDERㅤBY}, {@code UNIONㅤALL},
 * {@code WITHㅤRECURSIVE}.
 * </p>
 *
 * @param <E> the selected entity type, or an arbitrary placeholder for
 *            scalar/tuple projections
 */
public final class Q<E> {

	private static final class Cte {
		final String name;
		final Q<?> definition;

		Cte(String name, Q<?> definition) {
			this.name = name;
			this.definition = definition;
		}
	}

	private static final class Src {
		String entity; // entity simple name or CTE/derived name
		boolean isClass; // true if backed by a Java entity class (eligible for alias resolution)
		Expr path; // non-null for a path join (e.g. c.orders)
		String alias;
	}

	private static final class Join {
		String type; // "join", "left join", "join fetch", "left join fetch"
		Src target = new Src();
		Expr on;
	}

	private final List<Cte> ctes = new ArrayList<>();
	private boolean recursive;
	private final List<Expr> select = new ArrayList<>();
	private Src from;
	private final List<Join> joins = new ArrayList<>();
	private Src lastAliasable;
	private Expr where;
	private final List<Expr> groupBy = new ArrayList<>();
	private Expr having;
	private final List<Expr> orderBy = new ArrayList<>();
	private Q<?> unionAll;
	private Class<E> entityType;

	Q() {
	}

	Q(Class<E> entityType) {
		this.entityType = entityType;
	}

	// ===== entry-phase helpers (package-private; the public entry points live on
	// SelectStep/WithStep) =====

	Q<E> addSelect(Object... cols) {
		for (Object o : cols) {
			select.add(Expr.val(o));
		}
		return this;
	}

	<A> Q<E> addSelect(Col<A> first, Object... rest) {
		select.add(Expr.col(first));
		for (Object o : rest) {
			select.add(Expr.val(o));
		}
		return this;
	}

	Q<E> setFrom(Class<?> root) {
		from = src(root.getSimpleName(), true, null);
		lastAliasable = from;
		return this;
	}

	Q<E> setFrom(String cteOrDerived) {
		from = src(cteOrDerived, false, null);
		lastAliasable = from;
		return this;
	}

	Q<E> aliasLastSelect(String alias) {
		int last = select.size() - 1;
		select.set(last, select.get(last).ㅤAS(alias));
		return this;
	}

	Q<E> addCte(String name, Q<?> definition, boolean recursiveCte) {
		if (recursiveCte) {
			recursive = true;
		}
		ctes.add(new Cte(name, definition));
		return this;
	}

	// ===== clauses (available only after FROM) =====

	/**
	 * Inner-joins an entity ({@code join Entity}); follow with {@link #ㅤAS(String)}
	 * and {@link #ㅤONㅤ(Cond)}.
	 *
	 * @param entity the joined entity class; must not be {@code null}
	 * @return this builder, for chaining
	 */
	public Q<E> JOIN(Class<?> entity) {
		return addJoin("join", src(entity.getSimpleName(), true, null));
	}

	/**
	 * Inner-joins a CTE/derived table by name ({@code join name}).
	 *
	 * @param cte the CTE/derived-table name; must not be {@code null} or blank
	 * @return this builder, for chaining
	 */
	public Q<E> JOIN(String cte) {
		return addJoin("join", src(cte, false, null));
	}

	/**
	 * Inner-joins along an association path ({@code join owner.assoc}).
	 *
	 * <p>
	 * Example: with {@code FROM(Customer.class).AS("c")},
	 * {@code JOIN(Customer::orders).AS("o")} &rarr; {@code join c.orders o}.
	 * </p>
	 *
	 * @param path the association getter (method reference); must not be
	 *             {@code null}
	 * @param <T>  the owning entity type
	 * @return this builder, for chaining
	 */
	public <T> Q<E> JOIN(Col<T> path) {
		return addJoin("join", src(null, false, pathExpr(path)));
	}

	/**
	 * Left-outer-joins an entity ({@code left join Entity}).
	 *
	 * @param entity the joined entity class; must not be {@code null}
	 * @return this builder, for chaining
	 */
	public Q<E> LEFTㅤJOIN(Class<?> entity) {
		return addJoin("left join", src(entity.getSimpleName(), true, null));
	}

	/**
	 * Left-outer-joins a CTE/derived table by name ({@code left join name}).
	 *
	 * @param cte the CTE/derived-table name; must not be {@code null} or blank
	 * @return this builder, for chaining
	 */
	public Q<E> LEFTㅤJOIN(String cte) {
		return addJoin("left join", src(cte, false, null));
	}

	/**
	 * Left-outer-joins along an association path ({@code left join owner.assoc}).
	 *
	 * @param path the association getter (method reference); must not be
	 *             {@code null}
	 * @param <T>  the owning entity type
	 * @return this builder, for chaining
	 */
	public <T> Q<E> LEFTㅤJOIN(Col<T> path) {
		return addJoin("left join", src(null, false, pathExpr(path)));
	}

	/**
	 * Inner fetch-join along a path ({@code join fetch owner.assoc}).
	 *
	 * <p>
	 * Example: {@code JOINㅤFETCH(col(Customer::orders)).AS("o")} &rarr;
	 * {@code join fetch c.orders o}.
	 * </p>
	 *
	 * @param path the fetch path; the first element is used. Must not be
	 *             {@code null} or empty; pass a single
	 *             {@link Linq#col(Col)}/{@link Linq#col(String)} expression
	 * @return this builder, for chaining
	 */
	public Q<E> JOINㅤFETCH(Object... path) {
		return addJoin("join fetch", src(null, false, Expr.val(path[0])));
	}

	/**
	 * Left-outer fetch-join along a path ({@code left join fetch owner.assoc}).
	 *
	 * @param path the fetch path; the first element is used. Must not be
	 *             {@code null} or empty
	 * @return this builder, for chaining
	 */
	public Q<E> LEFTㅤJOINㅤFETCH(Object... path) {
		return addJoin("left join fetch", src(null, false, Expr.val(path[0])));
	}

	/**
	 * Left-outer fetch-join along a path, with a type-safe bare column reference,
	 * e.g. {@code LEFTㅤJOINㅤFETCH(Customer::orders)} &rarr;
	 * {@code left join fetch c.orders}.
	 *
	 * @param path the association getter (method reference); must not be
	 *             {@code null}
	 * @param <T>  the owning entity type
	 * @return this builder, for chaining
	 */
	public <T> Q<E> LEFTㅤJOINㅤFETCH(Col<T> path) {
		return addJoin("left join fetch", src(null, false, Expr.col(path)));
	}

	/**
	 * The {@code on} condition for the most recently added join, from a pre-built
	 * predicate (see {@link Linq#ㅤANDㅤ(Cond...)} / {@link Linq#ㅤᆖㅤ(Col, Object)}).
	 *
	 * @param predicate the join condition; must not be {@code null}
	 * @return this builder, for chaining
	 * @throws IndexOutOfBoundsException if no join has been added yet
	 */
	public Q<E> ㅤONㅤ(Cond predicate) {
		joins.get(joins.size() - 1).on = predicate.expr;
		return this;
	}

	/**
	 * The {@code on} condition for the most recently added join, started from a
	 * bare column; follow with a comparison operator on the returned
	 * {@link WhereStep} to supply the right-hand value.
	 *
	 * <p>
	 * Example:
	 * {@code .JOIN(Customer.class).AS("c").ON(Customer::id).ᆖ(col("o", Order::customerId))}.
	 * </p>
	 *
	 * @param col the left column getter (method reference); must not be
	 *            {@code null}
	 * @param <T> the entity type owning the column
	 * @return the pending comparison, awaiting an operator
	 * @throws IndexOutOfBoundsException if no join has been added yet
	 */
	public <T> WhereStep<E> ㅤONㅤ(Col<T> col) {
		Join j = joins.get(joins.size() - 1);
		return new WhereStep<>(Expr.col(col), Names.property(col), "and", (predicate, connector) -> {
			j.on = predicate;
			return this;
		});
	}

	/**
	 * The {@code where} clause from a pre-built predicate — convenient for flat,
	 * lambda-free composition with {@link Linq#ㅤANDㅤ(Cond...)} /
	 * {@link Linq#ㅤORㅤ(Cond...)}.
	 *
	 * <p>
	 * Example: {@code WHERE(AND(ᆖ(Order::status, "PAID"), ᐳ(Order::total, 100)))}.
	 * </p>
	 *
	 * @param predicate the condition; must not be {@code null}
	 * @return this builder, for chaining
	 */
	public Q<E> ㅤWHEREㅤ(Cond predicate) {
		where = predicate.expr;
		return this;
	}

	/**
	 * The {@code where} clause, started from a bare column; follow with a
	 * comparison operator on the returned {@link WhereStep} to supply the
	 * right-hand value.
	 *
	 * <p>
	 * Example: {@code WHERE(Driver::id).ᐳ(0)} &rarr; {@code where id > 0}.
	 * </p>
	 *
	 * @param col the left column getter (method reference); must not be
	 *            {@code null}
	 * @param <T> the entity type owning the column
	 * @return the pending comparison, awaiting an operator
	 */
	public <T> WhereStep<E> ㅤWHEREㅤ(Col<T> col) {
		return where(Expr.col(col), Names.property(col), "and");
	}

	/**
	 * The {@code where} clause, started from an alias-qualified column; follow with
	 * a comparison operator on the returned {@link WhereStep}.
	 *
	 * <p>
	 * Example: {@code WHERE("c", Car::driver).ᐅ(Driver::id).ᐳ(0)} &rarr;
	 * {@code where c.driver.id > 0}.
	 * </p>
	 *
	 * @param alias the range-variable alias to qualify the column with; must not be
	 *              {@code null}, e.g. {@code "c"}
	 * @param col   the left column getter (method reference); must not be
	 *              {@code null}
	 * @param <T>   the entity type owning the column
	 * @return the pending comparison, awaiting an operator
	 */
	public <T> WhereStep<E> ㅤWHEREㅤ(String alias, Col<T> col) {
		return where(Linq.col(alias, col), Names.property(col), "and");
	}

	/**
	 * The {@code where} clause, started from an arbitrary left expression (e.g. an
	 * aggregate, a {@code TREAT(...)}-cast member access or an aliased column);
	 * follow with a comparison operator on the returned {@link WhereStep}.
	 *
	 * <p>
	 * Example: {@code WHERE(col("r", "rnk")).ᆖ(1)} &rarr; {@code where r.rnk = 1}.
	 * </p>
	 *
	 * @param left the left operand; must not be {@code null}
	 * @return the pending comparison, awaiting an operator
	 */
	public WhereStep<E> ㅤWHEREㅤ(Object left) {
		return where(Expr.val(left), null, "and");
	}

	/**
	 * Appends another column-led predicate to the {@code where} clause, joined with
	 * {@code and}; follow with a comparison operator on the returned
	 * {@link WhereStep}.
	 *
	 * <p>
	 * Example: {@code WHERE(Driver::id).ᐳ(0).AND(Driver::id).ᐸ(3)} &rarr;
	 * {@code where id > 0 and id < 3}.
	 * </p>
	 *
	 * @param col the left column getter (method reference); must not be
	 *            {@code null}
	 * @param <T> the entity type owning the column
	 * @return the pending comparison, awaiting an operator
	 */
	public <T> WhereStep<E> ㅤANDㅤ(Col<T> col) {
		return where(Expr.col(col), Names.property(col), "and");
	}

	/**
	 * Appends another alias-qualified column-led predicate to the {@code where}
	 * clause, joined with {@code and}; follow with a comparison operator on the
	 * returned {@link WhereStep}.
	 *
	 * @param alias the range-variable alias to qualify the column with; must not be
	 *              {@code null}, e.g. {@code "c"}
	 * @param col   the left column getter (method reference); must not be
	 *              {@code null}
	 * @param <T>   the entity type owning the column
	 * @return the pending comparison, awaiting an operator
	 */
	public <T> WhereStep<E> ㅤANDㅤ(String alias, Col<T> col) {
		return where(Linq.col(alias, col), Names.property(col), "and");
	}

	/**
	 * Appends another expression-led predicate to the {@code where} clause, joined
	 * with {@code and}.
	 *
	 * @param left the left operand; must not be {@code null}
	 * @return the pending comparison, awaiting an operator
	 */
	public WhereStep<E> ㅤANDㅤ(Object left) {
		return where(Expr.val(left), null, "and");
	}

	/**
	 * Appends another column-led predicate to the {@code where} clause, joined with
	 * {@code or}; follow with a comparison operator on the returned
	 * {@link WhereStep}.
	 *
	 * @param col the left column getter (method reference); must not be
	 *            {@code null}
	 * @param <T> the entity type owning the column
	 * @return the pending comparison, awaiting an operator
	 */
	public <T> WhereStep<E> ㅤORㅤ(Col<T> col) {
		return where(Expr.col(col), Names.property(col), "or");
	}

	/**
	 * Appends another alias-qualified column-led predicate to the {@code where}
	 * clause, joined with {@code or}; follow with a comparison operator on the
	 * returned {@link WhereStep}.
	 *
	 * @param alias the range-variable alias to qualify the column with; must not be
	 *              {@code null}, e.g. {@code "c"}
	 * @param col   the left column getter (method reference); must not be
	 *              {@code null}
	 * @param <T>   the entity type owning the column
	 * @return the pending comparison, awaiting an operator
	 */
	public <T> WhereStep<E> ㅤORㅤ(String alias, Col<T> col) {
		return where(Linq.col(alias, col), Names.property(col), "or");
	}

	/**
	 * Appends another expression-led predicate to the {@code where} clause, joined
	 * with {@code or}.
	 *
	 * @param left the left operand; must not be {@code null}
	 * @return the pending comparison, awaiting an operator
	 */
	public WhereStep<E> ㅤORㅤ(Object left) {
		return where(Expr.val(left), null, "or");
	}

	private WhereStep<E> where(Expr left, String leftHint, String connector) {
		return new WhereStep<>(left, leftHint, connector, (predicate, conn) -> {
			where = (where == null) ? predicate : Expr.bin(where, conn, predicate);
			return this;
		});
	}

	/**
	 * The {@code group by} clause.
	 *
	 * <p>
	 * Example: {@code GROUPㅤBY(col("o", Order::customerId))} &rarr;
	 * {@code group by o.customerId}.
	 * </p>
	 *
	 * @param cols the grouping expressions, in order; must not be {@code null} or
	 *             empty
	 * @return this builder, for chaining
	 */
	public Q<E> GROUPㅤBY(Object... cols) {
		for (Object o : cols) {
			groupBy.add(Expr.val(o));
		}
		return this;
	}

	/**
	 * The {@code group by} clause, with type-safe bare column references, e.g.
	 * {@code GROUPㅤBY(Order::customerId)} &rarr; {@code group by o.customerId}.
	 *
	 * @param cols the grouping columns, in order; must not be {@code null} or empty
	 * @param <T>  the entity type owning the columns
	 * @return this builder, for chaining
	 */
	@SafeVarargs
	public final <T> Q<E> GROUPㅤBY(Col<T>... cols) {
		for (Col<T> col : cols) {
			groupBy.add(Expr.col(col));
		}
		return this;
	}

	/**
	 * The {@code having} clause from a pre-built predicate — convenient for flat,
	 * lambda-free composition of multiple conditions via
	 * {@code .AND()}/{@code .OR()} on a leading {@link Linq} predicate function.
	 *
	 * <p>
	 * Example:
	 * {@code HAVING(ᐳ(COUNT(Order::id), 5).AND().ᐳ(SUM(Order::total), 1000))}.
	 * </p>
	 *
	 * @param predicate the condition; must not be {@code null}
	 * @return this builder, for chaining
	 */
	public Q<E> HAVING(Cond predicate) {
		having = predicate.expr;
		return this;
	}

	/**
	 * The {@code having} clause, started from an arbitrary left expression
	 * (typically an aggregate such as
	 * {@link Linq#COUNT(Object)}/{@link Linq#SUM(Object)}); follow with a
	 * comparison operator on the returned {@link WhereStep}.
	 *
	 * <p>
	 * Example: {@code HAVING(SUM(Order::total)).ᐳ(1000)} &rarr;
	 * {@code having sum(o.total) > 1000}.
	 * </p>
	 *
	 * @param left the left operand; must not be {@code null}
	 * @return the pending comparison, awaiting an operator
	 */
	public WhereStep<E> HAVING(Object left) {
		Expr l = Expr.val(left);
		return new WhereStep<>(l, null, "and", (predicate, connector) -> {
			having = (having == null) ? predicate : Expr.bin(having, connector, predicate);
			return this;
		});
	}

	/**
	 * The {@code order by} clause. Append {@link Expr#DESC()}/{@link Expr#ASC()} to
	 * an element for direction.
	 *
	 * <p>
	 * Example: {@code ORDERㅤBY(SUM(col(Order::total)).DESC())} &rarr;
	 * {@code order by sum(o.total) desc}.
	 * </p>
	 *
	 * @param cols the ordering expressions, in order; must not be {@code null} or
	 *             empty
	 * @return this builder, for chaining
	 */
	public Q<E> ㅤORDERㅤBYㅤ(Object... cols) {
		for (Object o : cols) {
			orderBy.add(Expr.val(o));
		}
		return this;
	}

	/**
	 * The {@code order by} clause, started from an alias-qualified column; follow
	 * with {@link OrderByStep#DESC()}/{@link OrderByStep#ASC()} to supply the
	 * direction.
	 *
	 * <p>
	 * Example: {@code ORDER‿BY("b", "total").DESC()} &rarr;
	 * {@code order by b.total desc}.
	 * </p>
	 *
	 * @param alias the range-variable alias; must not be {@code null}, e.g.
	 *              {@code "b"}
	 * @param field the field name; must not be {@code null}, e.g. {@code "total"}
	 * @return the pending ordering, awaiting a direction
	 */
	public OrderByStep<E> ㅤORDERㅤBYㅤ(String alias, String field) {
		return new OrderByStep<>(this, Linq.col(alias, field));
	}

	Q<E> addOrderBy(Expr e) {
		orderBy.add(e);
		return this;
	}

	/**
	 * Appends a {@code union all} with another query.
	 *
	 * <p>
	 * Example: {@code q1.UNIONㅤALL(q2)} &rarr; {@code <q1> union all <q2>}.
	 * </p>
	 *
	 * @param other the query to append; must not be {@code null}
	 * @return this builder, for chaining
	 */
	public Q<E> UNIONㅤALL(Q<?> other) {
		unionAll = other;
		return this;
	}

	/**
	 * Table/range-variable alias for the most recent {@code FROM}/{@code JOIN}.
	 *
	 * <p>
	 * Example: {@code FROM(User.class).AS("u")} &rarr; {@code from User u}.
	 * </p>
	 *
	 * @param alias the alias; must not be {@code null} or blank, e.g. {@code "u"}
	 * @return this builder, for chaining
	 * @throws NullPointerException if no {@code FROM}/{@code JOIN} precedes this
	 *                              call
	 */
	public Q<E> ㅤAS(String alias) {
		lastAliasable.alias = alias;
		return this;
	}

	// ===== rendering =====

	/**
	 * Renders this finished statement to its HQL string.
	 *
	 * <p>
	 * Example:
	 * {@code SELECT(col(User::id)).FROM(User.class).WHERE(User::Name).ᆖ("John").getHql()}
	 * returns {@code "select id from User where Name = 'John'"}.
	 * </p>
	 *
	 * @return the HQL text; never {@code null}. ({@code SELECT} and {@code FROM}
	 *         are guaranteed by the fluent entry points, so this instance is always
	 *         renderable.)
	 */
	public String getHql() {
		return buildHql(renderCtx(null));
	}

	/**
	 * Renders this statement using the given parameter collector — {@code null}
	 * inlines literals exactly like {@link #getHql()}; a non-{@code null} collector
	 * (shared across the whole query tree, see {@link Q#via}) turns every literal
	 * encountered while rendering into a {@code :name} bind parameter instead.
	 */
	String hqlFor(ParamCollector collector) {
		return buildHql(renderCtx(collector));
	}

	private String buildHql(RenderCtx ctx) {
		StringBuilder sb = new StringBuilder();

		if (!ctes.isEmpty()) {
			sb.append("with ");
			if (recursive) {
				sb.append("recursive ");
			}
			for (int i = 0; i < ctes.size(); i++) {
				if (i > 0) {
					sb.append(", ");
				}
				sb.append(ctes.get(i).name).append(" as (").append(ctes.get(i).definition.hqlFor(ctx.collector()))
						.append(")");
			}
			sb.append(" ");
		}

		sb.append("select ").append(Expr.list(ctx, select.toArray()));
		sb.append(" from ").append(renderSrc(from, ctx));
		for (Join j : joins) {
			sb.append(" ").append(j.type).append(" ").append(renderSrc(j.target, ctx));
			if (j.on != null) {
				sb.append(" on ").append(j.on.render(ctx));
			}
		}
		if (where != null) {
			sb.append(" where ").append(where.render(ctx));
		}
		if (!groupBy.isEmpty()) {
			sb.append(" group by ").append(Expr.list(ctx, groupBy.toArray()));
		}
		if (having != null) {
			sb.append(" having ").append(having.render(ctx));
		}
		if (!orderBy.isEmpty()) {
			sb.append(" order by ").append(Expr.list(ctx, orderBy.toArray()));
		}

		String hql = sb.toString();
		if (unionAll != null) {
			hql = hql + " union all " + unionAll.hqlFor(ctx.collector());
		}
		return hql;
	}

	/**
	 * Executes this query as a typed entity query and returns its result list. Only
	 * valid when the projection selects instances of exactly one entity, i.e. the
	 * {@code SELECT} list is a single whole-entity selection —
	 * {@link Linq#SELECTㅤ(Class)} or {@link Linq#DISTINCTㅤ(Class)}. The selected
	 * type {@code E} is threaded through from that call, so no cast is needed here
	 * or at the call site.
	 *
	 * <p>
	 * Example:
	 * </p>
	 * 
	 * <pre>{@code
	 * Q<Order> q = SELECT(Order.class).FROM(Order.class).AS("o"); // "select o from Order o"
	 * List<Order> orders = q.via(entityManager);
	 * }</pre>
	 *
	 * <p>
	 * Unlike {@link #getHql()}, every literal value in the query (anything not
	 * wrapped in {@link Linq#param(String)}) is rendered as an invented
	 * {@code :name} bind parameter and passed to the {@link TypedQuery} via
	 * {@code setParameter} instead of being inlined into the HQL text — this
	 * applies throughout the whole query tree, including CTEs, {@code UNION ALL}
	 * parts and sub-queries. Values passed via {@link Linq#param(String)} are left
	 * untouched; bind their values yourself with
	 * {@code em.createQuery(getHql(), ...).setParameter(name, value)} if needed.
	 * </p>
	 *
	 * @param em the JPA entity manager used to create and run the query; must not
	 *           be {@code null}
	 * @return the (possibly empty) list of entities; never {@code null}
	 * @throws NullPointerException  if {@code em} is {@code null}
	 * @throws IllegalStateException if the query does not select a single entity
	 *                               (e.g. it is a scalar/tuple projection); use
	 *                               {@code em.createQuery(getHql())} for those
	 */
	public Iterable<E> via(EntityManager em) {
		Objects.requireNonNull(em, "em");
		if (select.size() != 1 || !(select.get(0) instanceof EntityExpr) || entityType == null) {
			throw new IllegalStateException("via(EntityManager) requires a query selecting a single entity, "
					+ "e.g. SELECT(entity(Order.class)); for projections use em.createQuery(getHql())");
		}
		ParamCollector collector = new ParamCollector();
		TypedQuery<E> tq = em.createQuery(hqlFor(collector), entityType);
		for (Map.Entry<String, Object> e : collector.params().entrySet()) {
			tq.setParameter(e.getKey(), e.getValue());
		}
		return tq.getResultList();
	}

	public Iterable<E> via(EntityManager em, java.util.function.Supplier<E> fallbackPersist) {
		Iterable<E> x = via(em);
		if (x.iterator().hasNext()) {
			return x;
		}
		E e = fallbackPersist.get();
		if (e == null) {
			return Collections.emptySet();
		}
		em.persist(e);
		return Collections.singleton(e);
	}
	/**
	 * Like {@link #via(EntityManager, java.util.function.Supplier)}, but fills a
	 * fresh instance (built via {@code entityType}'s no-arg constructor) instead of
	 * supplying an already-built one.
	 *
	 * @param em           the JPA entity manager used to create and run the query,
	 *                     and to persist the fallback entity; must not be
	 *                     {@code null}
	 * @param fallbackFill fills the fresh instance when the query finds no match;
	 *                     must not be {@code null}
	 * @return the query result if non-empty, otherwise a singleton with the
	 *         newly-persisted, filled instance
	 * @throws IllegalStateException if {@code entityType} has no accessible no-arg
	 *                               constructor
	 */
	public Iterable<E> via(EntityManager em, Consumer<E> fallbackFill) {
		Iterable<E> x = via(em);
		if (x.iterator().hasNext()) {
			return x;
		}
		E e = newEntityInstance();
		fallbackFill.accept(e);
		em.persist(e);
		return Collections.singleton(e);
	}

	private E newEntityInstance() {
		try {
			return entityType.getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException ex) {
			throw new IllegalStateException("Cannot instantiate " + entityType.getName()
					+ " via a no-arg constructor; use via(EntityManager, Supplier) instead", ex);
		}
	}

	/**
	 * Like {@link #via(EntityManager)}, but returns only the first entity instead
	 * of the whole result list.
	 *
	 * @param em the JPA entity manager used to create and run the query; must not
	 *           be {@code null}
	 * @return the first matching entity
	 * @throws NullPointerException          if {@code em} is {@code null}
	 * @throws IllegalStateException         if the query does not select a single
	 *                                        entity
	 * @throws java.util.NoSuchElementException if the query finds no match
	 */
	public E first(EntityManager em) {
		return via(em).iterator().next();
	}

	/**
	 * Like {@link #via(EntityManager, java.util.function.Supplier)}, but returns
	 * only the first entity instead of the whole result (the query's first match,
	 * or the persisted fallback when there is no match).
	 *
	 * @param em             the JPA entity manager used to create and run the
	 *                       query, and to persist the fallback entity; must not be
	 *                       {@code null}
	 * @param fallbackPersist supplies and persists a replacement entity when the
	 *                        query finds no match; must not be {@code null}
	 * @return the first matching entity, or the newly-persisted fallback
	 */
	public E first(EntityManager em, java.util.function.Supplier<E> fallbackPersist) {
		return via(em, fallbackPersist).iterator().next();
	}

	/**
	 * Like {@link #via(EntityManager, Consumer)}, but returns only the first
	 * entity instead of the whole result (the query's first match, or the
	 * newly-persisted, filled instance when there is no match).
	 *
	 * @param em           the JPA entity manager used to create and run the
	 *                     query, and to persist the fallback entity; must not be
	 *                     {@code null}
	 * @param fallbackFill fills the fresh instance when the query finds no match;
	 *                     must not be {@code null}
	 * @return the first matching entity, or the newly-persisted, filled instance
	 */
	public E first(EntityManager em, Consumer<E> fallbackFill) {
		return via(em, fallbackFill).iterator().next();
	}

	private RenderCtx renderCtx(ParamCollector collector) {
		List<String[]> sources = new ArrayList<>();
		if (from != null && from.isClass && from.alias != null) {
			sources.add(new String[] { from.alias, from.entity });
		}
		for (Join j : joins) {
			if (j.target.isClass && j.target.alias != null) {
				sources.add(new String[] { j.target.alias, j.target.entity });
			}
		}
		return new RenderCtx(sources, collector);
	}

	private static String renderSrc(Src s, RenderCtx ctx) {
		String head = (s.path != null) ? s.path.render(ctx) : s.entity;
		return s.alias == null ? head : head + " " + s.alias;
	}

	private Q<E> addJoin(String type, Src target) {
		Join j = new Join();
		j.type = type;
		j.target = target;
		joins.add(j);
		lastAliasable = target;
		return this;
	}

	private static Src src(String entity, boolean isClass, Expr path) {
		Src s = new Src();
		s.entity = entity;
		s.isClass = isClass;
		s.path = path;
		return s;
	}

	private static <T> Expr pathExpr(Col<T> path) {
		String prop = Names.property(path);
		String entity = Names.entity(path);
		return Expr.of(ctx -> {
			String a = ctx.aliasFor(entity);
			return (a == null ? entity : a) + "." + prop;
		});
	}
}
