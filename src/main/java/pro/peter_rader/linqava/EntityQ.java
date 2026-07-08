/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package pro.peter_rader.linqava;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.TypedQuery;

/**
 * A whole-entity query started via {@link Linq#SELECTㅤꁘㅤFROM(Class)}: exactly
 * {@code from <Entity>}, optionally filtered and ordered, deliberately restricted to
 * that shape — by type, not by a runtime check.
 *
 * <p>
 * Unlike {@link Q}, this class has no {@code AS(String)} and no {@code JOIN}/{@code LEFT JOIN}
 * variant: it does not extend or implement anything that declares them, so writing
 * {@code SELECTㅤꁘㅤFROM(Order.class).AS("o")} or adding a join simply does not compile. That is the
 * point — a plain "give me all/matching instances of one entity" query never needs a range-variable
 * alias (there is only ever one source, so {@link Linq#typedCol(TypedCol) typedCol(...)} references render as bare
 * property names) and never has a second source to join against.
 * </p>
 *
 * <p>
 * If a query genuinely needs an alias (typically to correlate a sub-query with an outer query) or a
 * join, start it the long way instead: {@link Linq#SELECTㅤ(Class) SELECT(Order.class)}.
 * {@link SelectStep#ㅤFROMㅤ(Class) FROM(Order.class)}.{@link Q#ㅤAS(String) AS("o")} returns a full
 * {@link Q}, which keeps {@code AS} and {@code JOIN} available.
 * </p>
 *
 * <p>
 * For the same reason, {@link #UNIONㅤALL(EntityQ)} only accepts another {@code EntityQ<E>} of the
 * <em>same</em> entity type {@code E} — the generic bound makes a {@code union all} across two
 * different entity types impossible to write, since both sides of a union must select the same
 * shape.
 * </p>
 *
 * @param <E> the queried entity type
 */
public final class EntityQ<E> {

	private final Class<E> entityType;
	private Expr where;
	private final List<Expr> orderBy = new ArrayList<>();
	private Expr limit;
	private Expr offset;
	private FlushModeType flushMode;
	private EntityQ<E> unionAll;

	EntityQ(Class<E> entityType) {
		this.entityType = entityType;
	}

	/**
	 * The {@code where} clause, started from a bare column; follow with a comparison operator on the
	 * returned {@link WhereStep} to supply the right-hand value.
	 *
	 * <p>Example: {@code SELECTㅤꁘㅤFROM(Driver.class).WHERE(Driver::id).ᐳ(0)} &rarr; {@code where id > 0}
	 * (no alias — there is only one source).</p>
	 *
	 * @param col the left column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return the pending comparison, awaiting an operator
	 */
	public <T> WhereStep<EntityQ<E>> ㅤWHEREㅤ(TypedCol<T, ?> col) {
		return where(Expr.typedCol(col), Names.property(col), "and");
	}

	/**
	 * The {@code where} clause from a pre-built predicate — convenient for flat, lambda-free
	 * composition with {@link Linq#ㅤANDㅤ(Cond...)} / {@link Linq#ㅤORㅤ(Cond...)}.
	 *
	 * @param predicate the condition; must not be {@code null}
	 * @return this builder, for chaining
	 */
	public EntityQ<E> ㅤWHEREㅤ(Cond predicate) {
		where = predicate.expr;
		return this;
	}

	/**
	 * Appends another column-led predicate to the {@code where} clause, joined with {@code and};
	 * follow with a comparison operator on the returned {@link WhereStep}.
	 *
	 * @param col the left column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return the pending comparison, awaiting an operator
	 */
	public <T> WhereStep<EntityQ<E>> ㅤANDㅤ(TypedCol<T, ?> col) {
		return where(Expr.typedCol(col), Names.property(col), "and");
	}

	/**
	 * Appends another column-led predicate to the {@code where} clause, joined with {@code or};
	 * follow with a comparison operator on the returned {@link WhereStep}.
	 *
	 * @param col the left column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return the pending comparison, awaiting an operator
	 */
	public <T> WhereStep<EntityQ<E>> ㅤORㅤ(TypedCol<T, ?> col) {
		return where(Expr.typedCol(col), Names.property(col), "or");
	}

	private WhereStep<EntityQ<E>> where(Expr left, String leftHint, String connector) {
		return new WhereStep<>(left, leftHint, connector, (predicate, conn) -> {
			where = (where == null) ? predicate : Expr.bin(where, conn, predicate);
			return this;
		});
	}

	/**
	 * The {@code order by} clause. Append {@link Expr#DESC()}/{@link Expr#ASC()} to an element for
	 * direction.
	 *
	 * @param cols the ordering expressions, in order; must not be {@code null} or empty
	 * @return this builder, for chaining
	 */
	public EntityQ<E> ㅤORDERㅤBYㅤ(Object... cols) {
		for (Object o : cols) {
			orderBy.add(Expr.val(o));
		}
		return this;
	}

	/**
	 * The {@code order by} clause, with type-safe bare column references.
	 *
	 * @param cols the ordering columns, in order; must not be {@code null} or empty
	 * @param <T>  the entity type owning the columns
	 * @return this builder, for chaining
	 */
	@SafeVarargs
	public final <T> EntityQ<E> ㅤORDERㅤBYㅤ(TypedCol<T, ?>... cols) {
		for (TypedCol<T, ?> col : cols) {
			orderBy.add(Expr.typedCol(col));
		}
		return this;
	}

	/**
	 * The {@code limit} clause, capping the number of returned rows.
	 *
	 * @param maxResults the maximum number of rows to return; must not be negative
	 * @return this builder, for chaining
	 */
	public EntityQ<E> LIMIT(int maxResults) {
		limit = Expr.val(maxResults, "limit");
		return this;
	}

	/**
	 * The {@code offset} clause, skipping this many rows before the first returned row — typically
	 * combined with {@link #LIMIT(int)} for pagination.
	 *
	 * @param firstResult the zero-based index of the first row to return; must not be negative
	 * @return this builder, for chaining
	 */
	public EntityQ<E> OFFSET(int firstResult) {
		offset = Expr.val(firstResult, "offset");
		return this;
	}

	/**
	 * Overrides the {@link TypedQuery}'s flush mode for every {@code via}/{@code first} call on this
	 * query (all of them ultimately create their {@link TypedQuery} through
	 * {@link #via(EntityManager)}), instead of leaving it at the persistence context's default.
	 *
	 * @param mode the flush mode to apply; must not be {@code null}
	 * @return this builder, for chaining
	 */
	public EntityQ<E> FLUSHㅤMODE(FlushModeType mode) {
		flushMode = Objects.requireNonNull(mode, "mode");
		return this;
	}

	/**
	 * Appends a {@code union all} with another query selecting the <em>same</em> entity type.
	 *
	 * @param other the query to append; must not be {@code null}
	 * @return this builder, for chaining
	 */
	public EntityQ<E> UNIONㅤALL(EntityQ<E> other) {
		unionAll = other;
		return this;
	}

	/**
	 * Renders this finished statement to its HQL string, with every literal value inlined directly
	 * into the text rather than bound as a parameter.
	 *
	 * <p>
	 * <strong>Unsafe with untrusted input</strong> — inlining a value that came from outside the
	 * application risks HQL injection; see {@link Q#getUnsafeHql()} for the full rationale. Prefer
	 * {@link #via(jakarta.persistence.EntityManager)}, which always parameterizes literals instead.
	 * </p>
	 *
	 * @return the HQL text; never {@code null}
	 */
	public String getUnsafeHql() {
		return buildHql(renderCtx(null));
	}

	String hqlFor(ParamCollector collector) {
		return buildHql(renderCtx(collector));
	}

	private RenderCtx renderCtx(ParamCollector collector) {
		return new RenderCtx(Collections.emptyList(), collector);
	}

	private String buildHql(RenderCtx ctx) {
		String name = entityType.getName();
		StringBuilder sb = new StringBuilder();
		// Deliberately "from X", not "select X from X": an unaliased "select X" is parsed by
		// Hibernate as an entity TYPE LITERAL (relevant for polymorphic queries), not as a
		// shorthand for the implicit whole-entity projection that a bare "from X" already is.
		sb.append("from ").append(name);
		if (where != null) {
			sb.append(" where ").append(where.render(ctx));
		}
		if (!orderBy.isEmpty()) {
			sb.append(" order by ").append(Expr.list(ctx, orderBy.toArray()));
		}
		if (limit != null) {
			sb.append(" limit ").append(limit.render(ctx));
		}
		if (offset != null) {
			sb.append(" offset ").append(offset.render(ctx));
		}
		String hql = sb.toString();
		if (unionAll != null) {
			hql = hql + " union all " + unionAll.hqlFor(ctx.collector());
		}
		return hql;
	}

	/**
	 * Executes this query and returns its (possibly empty) result list. Every literal value in the
	 * query is rendered as an invented {@code :name} bind parameter rather than inlined, exactly like
	 * {@link Q#via(EntityManager)}.
	 *
	 * @param em the JPA entity manager used to create and run the query; must not be {@code null}
	 * @return the (possibly empty) list of entities; never {@code null}
	 */
	public Iterable<E> via(EntityManager em) {
		Objects.requireNonNull(em, "em");
		ParamCollector collector = new ParamCollector();
		TypedQuery<E> tq = em.createQuery(hqlFor(collector), entityType);
		if (flushMode != null) {
			tq.setFlushMode(flushMode);
		}
		for (Map.Entry<String, Object> e : collector.params().entrySet()) {
			tq.setParameter(e.getKey(), e.getValue());
		}
		return tq.getResultList();
	}

	/**
	 * Like {@link #via(EntityManager)}, but supplies and persists a replacement entity when the query
	 * finds no match.
	 *
	 * @param em              the JPA entity manager used to create and run the query, and to persist
	 *                        the fallback entity; must not be {@code null}
	 * @param fallbackPersist supplies and persists a replacement entity when the query finds no match;
	 *                        must not be {@code null}
	 * @return the query result if non-empty, otherwise a singleton with the newly-persisted fallback
	 */
	public Iterable<E> via(EntityManager em, Supplier<E> fallbackPersist) {
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
	 * Like {@link #via(EntityManager, Supplier)}, but fills a fresh instance (built via
	 * {@code entityType}'s no-arg constructor) instead of supplying an already-built one. Passing
	 * {@code null} for {@code fallbackFill} disables the fallback.
	 *
	 * @param em           the JPA entity manager used to create and run the query, and to persist the
	 *                     fallback entity; must not be {@code null}
	 * @param fallbackFill fills the fresh instance when the query finds no match; {@code null} to
	 *                     return a singleton holding {@code null} instead of falling back
	 * @return the query result if non-empty, otherwise a singleton with the newly-persisted, filled
	 *         instance, or with {@code null} if {@code fallbackFill} is {@code null}
	 */
	public Iterable<E> via(EntityManager em, Consumer<E> fallbackFill) {
		Iterable<E> x = via(em);
		if (x.iterator().hasNext()) {
			return x;
		}
		if (fallbackFill == null) {
			return Collections.singleton(null);
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
	 * Like {@link #via(EntityManager)}, but returns only the first entity instead of the whole result
	 * list.
	 *
	 * @param em the JPA entity manager used to create and run the query; must not be {@code null}
	 * @return the first matching entity
	 * @throws NoSuchElementException if the query finds no match
	 */
	public E first(EntityManager em) throws NoSuchElementException {
		return via(em).iterator().next();
	}

	/**
	 * Like {@link #via(EntityManager, Supplier)}, but returns only the first entity instead of the
	 * whole result.
	 *
	 * @param em              the JPA entity manager used to create and run the query, and to persist
	 *                        the fallback entity; must not be {@code null}
	 * @param fallbackPersist supplies and persists a replacement entity when the query finds no match;
	 *                        must not be {@code null}
	 * @return the first matching entity, or the newly-persisted fallback
	 */
	public E first(EntityManager em, Supplier<E> fallbackPersist) {
		Iterable<E> x = via(em);
		if (x.iterator().hasNext()) {
			return x.iterator().next();
		}
		E result = fallbackPersist.get();
		if (result != null) {
			em.persist(result);
		}
		return result;
	}

	/**
	 * Like {@link #via(EntityManager, Consumer)}, but returns only the first entity instead of the
	 * whole result.
	 *
	 * @param em           the JPA entity manager used to create and run the query, and to persist the
	 *                     fallback entity; must not be {@code null}
	 * @param fallbackFill fills the fresh instance when the query finds no match; {@code null} to
	 *                     return {@code null} instead of falling back
	 * @return the first matching entity, the newly-persisted, filled instance, or {@code null} if
	 *         there is no match and {@code fallbackFill} is {@code null}
	 */
	public E first(EntityManager em, Consumer<E> fallbackFill) {
		return via(em, fallbackFill).iterator().next();
	}
}
