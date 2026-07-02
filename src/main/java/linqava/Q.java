/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _ 
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package linqava;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import jakarta.persistence.EntityManager;

/**
 * A linqava query/statement — the builder reached <em>after</em> {@code FROM}. Call {@link #getHql()}
 * on the finished statement to obtain the corresponding HQL string, or {@link #via(EntityManager)} to
 * run it.
 *
 * <p>The fluent entry points enforce a valid clause order at compile time: {@link Linq#SELECT} returns
 * a {@link SelectStep} that only offers {@code FROM}, and {@code FROM} returns this {@code Q}, which no
 * longer offers {@code SELECT} or {@code FROM}. Hence {@code FROM(x).FROM(y)}, a double {@code SELECT}
 * and a missing {@code FROM} cannot be written.</p>
 *
 * <p>SQL keywords are methods; multi-word keywords are a single method whose words are joined by the
 * connector glyph {@code ‿} (U+203F UNDERTIE), e.g. {@code LEFT‿JOIN}, {@code GROUP‿BY},
 * {@code ORDER‿BY}, {@code UNION‿ALL}, {@code WITH‿RECURSIVE}.</p>
 */
public final class Q {

	private static final class Cte {
		final String name;
		final Q definition;

		Cte(String name, Q definition) {
			this.name = name;
			this.definition = definition;
		}
	}

	private static final class Src {
		String entity;   // entity simple name or CTE/derived name
		boolean isClass; // true if backed by a Java entity class (eligible for alias resolution)
		Expr path;       // non-null for a path join (e.g. c.orders)
		String alias;
	}

	private static final class Join {
		String type;     // "join", "left join", "join fetch", "left join fetch"
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
	private Q unionAll;

	// ===== entry-phase helpers (package-private; the public entry points live on SelectStep/WithStep) =====

	Q addSelect(Object... cols) {
		for (Object o : cols) {
			select.add(Expr.val(o));
		}
		return this;
	}

	<A> Q addSelect(Col<A> first, Object... rest) {
		select.add(Expr.col(first));
		for (Object o : rest) {
			select.add(Expr.val(o));
		}
		return this;
	}

	Q setFrom(Class<?> root) {
		from = src(root.getSimpleName(), true, null);
		lastAliasable = from;
		return this;
	}

	Q setFrom(String cteOrDerived) {
		from = src(cteOrDerived, false, null);
		lastAliasable = from;
		return this;
	}

	Q aliasLastSelect(String alias) {
		int last = select.size() - 1;
		select.set(last, select.get(last).ㅤASㅤ(alias));
		return this;
	}

	Q addCte(String name, Q definition, boolean recursiveCte) {
		if (recursiveCte) {
			recursive = true;
		}
		ctes.add(new Cte(name, definition));
		return this;
	}

	// ===== clauses (available only after FROM) =====

	/**
	 * Inner-joins an entity ({@code join Entity}); follow with {@link #ㅤASㅤ(String)} and {@link #ㅤONㅤ(Cond)}.
	 *
	 * @param entity the joined entity class; must not be {@code null}
	 * @return this builder, for chaining
	 */
	public Q JOIN(Class<?> entity) { return addJoin("join", src(entity.getSimpleName(), true, null)); }

	/**
	 * Inner-joins a CTE/derived table by name ({@code join name}).
	 *
	 * @param cte the CTE/derived-table name; must not be {@code null} or blank
	 * @return this builder, for chaining
	 */
	public Q JOIN(String cte) { return addJoin("join", src(cte, false, null)); }

	/**
	 * Inner-joins along an association path ({@code join owner.assoc}).
	 *
	 * <p>Example: with {@code FROM(Customer.class).AS("c")}, {@code JOIN(Customer::orders).AS("o")}
	 * &rarr; {@code join c.orders o}.</p>
	 *
	 * @param path the association getter (method reference); must not be {@code null}
	 * @param <T>  the owning entity type
	 * @return this builder, for chaining
	 */
	public <T> Q JOIN(Col<T> path) { return addJoin("join", src(null, false, pathExpr(path))); }

	/**
	 * Left-outer-joins an entity ({@code left join Entity}).
	 *
	 * @param entity the joined entity class; must not be {@code null}
	 * @return this builder, for chaining
	 */
	public Q LEFT‿JOIN(Class<?> entity) { return addJoin("left join", src(entity.getSimpleName(), true, null)); }

	/**
	 * Left-outer-joins a CTE/derived table by name ({@code left join name}).
	 *
	 * @param cte the CTE/derived-table name; must not be {@code null} or blank
	 * @return this builder, for chaining
	 */
	public Q LEFT‿JOIN(String cte) { return addJoin("left join", src(cte, false, null)); }

	/**
	 * Left-outer-joins along an association path ({@code left join owner.assoc}).
	 *
	 * @param path the association getter (method reference); must not be {@code null}
	 * @param <T>  the owning entity type
	 * @return this builder, for chaining
	 */
	public <T> Q LEFT‿JOIN(Col<T> path) { return addJoin("left join", src(null, false, pathExpr(path))); }

	/**
	 * Inner fetch-join along a path ({@code join fetch owner.assoc}).
	 *
	 * <p>Example: {@code JOIN‿FETCH(c(Customer::orders)).AS("o")} &rarr; {@code join fetch c.orders o}.</p>
	 *
	 * @param path the fetch path; the first element is used. Must not be {@code null} or empty;
	 *             pass a single {@link Linq#c(Col)}/{@link Linq#c(String)} expression
	 * @return this builder, for chaining
	 */
	public Q JOIN‿FETCH(Object... path) { return addJoin("join fetch", src(null, false, Expr.val(path[0]))); }

	/**
	 * Left-outer fetch-join along a path ({@code left join fetch owner.assoc}).
	 *
	 * @param path the fetch path; the first element is used. Must not be {@code null} or empty
	 * @return this builder, for chaining
	 */
	public Q LEFT‿JOIN‿FETCH(Object... path) { return addJoin("left join fetch", src(null, false, Expr.val(path[0]))); }

	/**
	 * The {@code on} condition for the most recently added join, built via a lambda.
	 *
	 * <p>Example: {@code .JOIN(Customer.class).AS("c").ON($ -> $.ᆖ(Customer::id, c("o", Order::customerId)))}.</p>
	 *
	 * @param predicate builds the condition from a fresh {@link Cond}; must not be {@code null}
	 * @return this builder, for chaining
	 * @throws IndexOutOfBoundsException if no join has been added yet
	 */
	public Q ㅤONㅤ(Function<Cond, Cond> predicate) {
		joins.get(joins.size() - 1).on = predicate.apply(new Cond()).expr;
		return this;
	}

	/**
	 * The {@code on} condition for the most recently added join, from a pre-built predicate
	 * (see {@link Linq#ㅤANDㅤ(Cond...)} / {@link Linq#ᆖ(Col, Object)}).
	 *
	 * @param predicate the join condition; must not be {@code null}
	 * @return this builder, for chaining
	 * @throws IndexOutOfBoundsException if no join has been added yet
	 */
	public Q ㅤONㅤ(Cond predicate) {
		joins.get(joins.size() - 1).on = predicate.expr;
		return this;
	}

	/**
	 * The {@code where} clause, built via a lambda on a fresh condition context.
	 *
	 * <p>Example: {@code WHERE($ -> $.ᆖ(User::Name, "John"))} &rarr; {@code where Name = 'John'}.</p>
	 *
	 * @param predicate builds the condition from a fresh {@link Cond}; must not be {@code null}
	 * @return this builder, for chaining
	 */
	public Q WHEREㅤ(Function<Cond, Cond> predicate) {
		where = predicate.apply(new Cond()).expr;
		return this;
	}

	/**
	 * The {@code where} clause from a pre-built predicate — convenient for flat, lambda-free
	 * composition with {@link Linq#ㅤANDㅤ(Cond...)} / {@link Linq#OR(Cond...)}.
	 *
	 * <p>Example: {@code WHERE(AND(ᆖ(Order::status, "PAID"), ᐳ(Order::total, 100)))}.</p>
	 *
	 * @param predicate the condition; must not be {@code null}
	 * @return this builder, for chaining
	 */
	public Q ㅤWHEREㅤ(Cond predicate) {
		where = predicate.expr;
		return this;
	}

	/**
	 * The {@code where} clause, started from a bare column; follow with a comparison operator on the
	 * returned {@link WhereStep} to supply the right-hand value.
	 *
	 * <p>Example: {@code WHERE(Driver::id).ᐳ(0)} &rarr; {@code where id > 0}.</p>
	 *
	 * @param col the left column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return the pending comparison, awaiting an operator
	 */
	public <T> WhereStep<T> ㅤWHEREㅤ(Col<T> col) {
		return new WhereStep<>(this, col, "and");
	}

	/**
	 * Appends another column-led predicate to the {@code where} clause, joined with {@code and};
	 * follow with a comparison operator on the returned {@link WhereStep}.
	 *
	 * <p>Example: {@code WHERE(Driver::id).ᐳ(0).AND(Driver::id).ᐸ(3)} &rarr; {@code where id > 0 and id < 3}.</p>
	 *
	 * @param col the left column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return the pending comparison, awaiting an operator
	 */
	public <T> WhereStep<T> ㅤANDㅤ(Col<T> col) {
		return new WhereStep<>(this, col, "and");
	}

	/**
	 * Finishes a {@link WhereStep} comparison by appending {@code col op r} to the {@code where} clause.
	 */
	Q appendWhere(Col<?> col, String op, Object r, String connector) {
		Expr left = Expr.col(col);
		Expr predicate = Expr.of(c -> left.render(c) + " " + op + " " + Expr.val(r).render(c));
		where = (where == null) ? predicate : Expr.bin(where, connector, predicate);
		return this;
	}

	/**
	 * The {@code group by} clause.
	 *
	 * <p>Example: {@code GROUP‿BY(c(Order::customerId))} &rarr; {@code group by o.customerId}.</p>
	 *
	 * @param cols the grouping expressions, in order; must not be {@code null} or empty
	 * @return this builder, for chaining
	 */
	public Q GROUP‿BY(Object... cols) {
		for (Object o : cols) {
			groupBy.add(Expr.val(o));
		}
		return this;
	}

	/**
	 * The {@code having} clause, built via a lambda on a fresh condition context.
	 *
	 * <p>Example: {@code HAVING($ -> $.ᐳ(COUNT(c(Order::id)), 5))} &rarr; {@code having count(o.id) > 5}.</p>
	 *
	 * @param predicate builds the condition from a fresh {@link Cond}; must not be {@code null}
	 * @return this builder, for chaining
	 */
	public Q HAVING(Function<Cond, Cond> predicate) {
		having = predicate.apply(new Cond()).expr;
		return this;
	}

	/**
	 * The {@code having} clause from a pre-built predicate.
	 *
	 * @param predicate the condition; must not be {@code null}
	 * @return this builder, for chaining
	 */
	public Q HAVING(Cond predicate) {
		having = predicate.expr;
		return this;
	}

	/**
	 * The {@code order by} clause. Append {@link Expr#DESC()}/{@link Expr#ASC()} to an element for direction.
	 *
	 * <p>Example: {@code ORDER‿BY(SUM(c(Order::total)).DESC())} &rarr; {@code order by sum(o.total) desc}.</p>
	 *
	 * @param cols the ordering expressions, in order; must not be {@code null} or empty
	 * @return this builder, for chaining
	 */
	public Q ㅤORDER‿BYㅤ(Object... cols) {
		for (Object o : cols) {
			orderBy.add(Expr.val(o));
		}
		return this;
	}

	/**
	 * Appends a {@code union all} with another query.
	 *
	 * <p>Example: {@code q1.UNION‿ALL(q2)} &rarr; {@code <q1> union all <q2>}.</p>
	 *
	 * @param other the query to append; must not be {@code null}
	 * @return this builder, for chaining
	 */
	public Q UNION‿ALL(Q other) {
		unionAll = other;
		return this;
	}

	/**
	 * Table/range-variable alias for the most recent {@code FROM}/{@code JOIN}.
	 *
	 * <p>Example: {@code FROM(User.class).AS("u")} &rarr; {@code from User u}.</p>
	 *
	 * @param alias the alias; must not be {@code null} or blank, e.g. {@code "u"}
	 * @return this builder, for chaining
	 * @throws NullPointerException if no {@code FROM}/{@code JOIN} precedes this call
	 */
	public Q ㅤASㅤ(String alias) {
		lastAliasable.alias = alias;
		return this;
	}

	// ===== rendering =====

	/**
	 * Renders this finished statement to its HQL string.
	 *
	 * <p>Example: {@code SELECT(c(User::id)).FROM(User.class).WHERE($ -> $.ᆖ(User::Name, "John")).getHql()}
	 * returns {@code "select id from User where Name = 'John'"}.</p>
	 *
	 * @return the HQL text; never {@code null}. ({@code SELECT} and {@code FROM} are guaranteed by the
	 *         fluent entry points, so this instance is always renderable.)
	 */
	public String getHql() {
		RenderCtx ctx = renderCtx();
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
				sb.append(ctes.get(i).name).append(" as (").append(ctes.get(i).definition.getHql()).append(")");
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
			hql = hql + " union all " + unionAll.getHql();
		}
		return hql;
	}

	/**
	 * Executes this query as a typed entity query and returns its result list. Only valid when the
	 * projection selects instances of exactly one entity, i.e. the {@code SELECT} list is a single
	 * whole-entity selection — {@link Linq#SELECTㅤ(Class)} or {@link Linq#DISTINCTㅤ(Class)}.
	 *
	 * <p>Example:</p>
	 * <pre>{@code
	 * Q q = SELECT(entity(Order.class)).FROM(Order.class).AS("o");   // "select o from Order o"
	 * List<Order> orders = q.via(entityManager);
	 * }</pre>
	 *
	 * @param em  the JPA entity manager used to create and run the query; must not be {@code null}
	 * @param <T> the selected entity type, inferred from the assignment target
	 * @return the (possibly empty) list of entities; never {@code null}
	 * @throws NullPointerException  if {@code em} is {@code null}
	 * @throws IllegalStateException if the query does not select a single entity (e.g. it is a
	 *                               scalar/tuple projection); use {@code em.createQuery(getHql())} for those
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> via(EntityManager em) {
		Objects.requireNonNull(em, "em");
		if (select.size() != 1 || !(select.get(0) instanceof EntityExpr)) {
			throw new IllegalStateException(
					"via(EntityManager) requires a query selecting a single entity, "
							+ "e.g. SELECT(entity(Order.class)); for projections use em.createQuery(getHql())");
		}
		Class<T> type = (Class<T>) ((EntityExpr) select.get(0)).type;
		return em.createQuery(getHql(), type).getResultList();
	}

	private RenderCtx renderCtx() {
		List<String[]> sources = new ArrayList<>();
		if (from != null && from.isClass && from.alias != null) {
			sources.add(new String[] { from.alias, from.entity });
		}
		for (Join j : joins) {
			if (j.target.isClass && j.target.alias != null) {
				sources.add(new String[] { j.target.alias, j.target.entity });
			}
		}
		return new RenderCtx(sources);
	}

	private static String renderSrc(Src s, RenderCtx ctx) {
		String head = (s.path != null) ? s.path.render(ctx) : s.entity;
		return s.alias == null ? head : head + " " + s.alias;
	}

	private Q addJoin(String type, Src target) {
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
