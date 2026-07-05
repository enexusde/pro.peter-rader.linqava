/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _ 
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package pro.peter_rader.linqava;

/**
 * Static entry points, SQL functions and predicate builders of the linqava DSL.
 *
 * <p>
 * Intended to be used with a static import so the query reads like SQL:
 * </p>
 * 
 * <pre>{@code
 * import static linqava.Linq.*;
 *
 * Q<?> q = SELECT(col(User::id))
 *           .FROM(User.class)
 *           .WHERE(User::Name).ᆖ("John");
 * q.getHql(); // "select id from User where Name = 'John'"
 * }</pre>
 *
 * <p>
 * General null policy: unless a method explicitly states otherwise, no argument
 * may be {@code null}; passing {@code null} throws {@link NullPointerException}
 * either immediately or when {@link Q#getHql()} is called. The documented
 * exception is {@link #lit(Object)}, where {@code null} is rendered as the SQL
 * {@code null} literal.
 * </p>
 */
public final class Linq {

	private Linq() {
	}

	// ===== query entry points =====

	/**
	 * Starts a {@code SELECT} query.
	 *
	 * <p>
	 * Example: {@code SELECT(col(Order::id), COUNT(col(Order::id)))} &rarr;
	 * {@code select o.id, count(o.id) ...}
	 * </p>
	 *
	 * @param cols the projected columns/expressions, in order; typically
	 *             {@link #col(Col)}, aggregates such as {@link #COUNT(Object)}, or
	 *             {@link #entity(Class)}. Must not be {@code null} and should not
	 *             contain {@code null} elements (a {@code null} element renders as
	 *             the literal text {@code null}). May be empty.
	 * @return the {@code SELECT} phase, which requires a
	 *         {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static SelectStep<Object> SELECTㅤ(Object... cols) {
		return new SelectStep<>(new Q<Object>().addSelect(cols));
	}

	/**
	 * Shorthand for the common "give me all/matching instances of one entity" query — exactly
	 * {@code select <Entity> from <Entity>}. Deliberately restricted compared to
	 * {@link #SELECTㅤ(Class) SELECT(clazz)}.{@link SelectStep#ㅤFROMㅤ(Class) FROM(clazz)}: the returned
	 * {@link EntityQ} has no {@code AS(String)} and no {@code JOIN}, because a single, unaliased
	 * source is all this shape ever needs or supports.
	 *
	 * <p>Example: {@code SELECTㅤꁘㅤFROM(Order.class).WHERE(Order::status).ᆖ("PAID")} &rarr;
	 * {@code select Order from Order where status = 'PAID'}.</p>
	 *
	 * <p>Need an alias (e.g. to correlate a sub-query) or a join? Use the full
	 * {@code SELECT(clazz).FROM(clazz)} form instead, which returns a {@link Q}.</p>
	 *
	 * @param clazz the queried entity class; must not be {@code null}
	 * @param <T>   the queried entity type
	 * @return the restricted query builder
	 */
	public static <T> EntityQ<T> SELECTㅤꁘㅤFROM(Class<T> clazz) {
		return new EntityQ<>(clazz);
	}

	/**
	 * Starts a {@code SELECT} whose first column is a bare getter reference, e.g.
	 * {@code SELECT(Order::id, CASE()...END())} — avoids wrapping the leading
	 * column in {@link #col(Col)}.
	 *
	 * @param first the first column getter (method reference); must not be
	 *              {@code null}
	 * @param rest  the remaining columns/expressions, in order; must not be
	 *              {@code null}, may be empty
	 * @param <A>   the entity type owning the first column
	 * @return the {@code SELECT} phase, which requires a
	 *         {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <A> SelectStep<Object> SELECTㅤ(Col<A> first, Object... rest) {
		return new SelectStep<>(new Q<Object>().addSelect(first, rest));
	}

	/**
	 * Starts a {@code SELECT} of a whole entity — shorthand for
	 * {@code SELECT(entity(type))}. The selected type is threaded through to the
	 * resulting {@link Q}, so {@link Q#via} needs no cast.
	 *
	 * <p>
	 * Example: {@code SELECT(Order.class).FROM(Order.class).AS("o")} &rarr;
	 * {@code select o from Order o}. Such single-entity queries can be run with
	 * {@link Q#via(jakarta.persistence.EntityManager)}.
	 * </p>
	 *
	 * @param entityType the selected entity class; must not be {@code null}, e.g.
	 *                   {@code Order.class}
	 * @param <E>        the selected entity type
	 * @return the {@code SELECT} phase, which requires a
	 *         {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <E> SelectStep<E> SELECTㅤ(Class<E> entityType) {
		return new SelectStep<>(new Q<>(entityType).addSelect(entity(entityType)));
	}

	/**
	 * Starts a {@code SELECT} of a whole (possibly {@code distinct}) entity wrapped
	 * by {@link #DISTINCTㅤ(Class)} — the selected type is threaded through just
	 * like {@link #SELECTㅤ(Class)}.
	 *
	 * @param entity the distinct entity marker, e.g.
	 *               {@code DISTINCT(Customer.class)}; must not be {@code null}
	 * @param <E>    the selected entity type
	 * @return the {@code SELECT} phase, which requires a
	 *         {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <E> SelectStep<E> SELECTㅤ(EntityExpr<E> entity) {
		return new SelectStep<>(new Q<>(entity.type).addSelect(entity));
	}

	/**
	 * Starts a query with a common table expression
	 * ({@code WITH name AS (definition)}).
	 *
	 * <p>
	 * Example:
	 * {@code WITH("activeUsers", SELECT(...).FROM(User.class)...).SELECT(...).FROM("activeUsers")}
	 * </p>
	 *
	 * @param name       the CTE name, later referenced via
	 *                   {@link SelectStep#FROM(String)}; must not be {@code null}
	 *                   or blank, e.g. {@code "activeUsers"}
	 * @param definition the sub-query defining the CTE; must not be {@code null}
	 * @return the {@code WITH} phase; chain {@link WithStep#WITH(String, Q)} for
	 *         further CTEs, then {@code SELECT}
	 */
	public static WithStep WITH(String name, Q<?> definition) {
		return new WithStep().WITH(name, definition);
	}

	/**
	 * Starts a query with a recursive common table expression
	 * ({@code WITH RECURSIVE name AS (...)}).
	 *
	 * <p>
	 * Example: the {@code definition} is usually an anchor query joined to the
	 * recursive part via {@link Q#UNIONㅤALL(Q)}.
	 * </p>
	 *
	 * @param name       the CTE name (also used inside the recursive part as a
	 *                   {@link Q#JOIN(String)} target); must not be {@code null} or
	 *                   blank
	 * @param definition the recursive sub-query (anchor {@code UNION ALL} recursive
	 *                   step); must not be {@code null}
	 * @return the {@code WITH} phase; continue with {@code SELECT}
	 */
	public static WithStep WITHㅤRECURSIVE(String name, Q<?> definition) {
		return new WithStep().WITHㅤRECURSIVE(name, definition);
	}

	// ===== column / value helpers =====

	/**
	 * A type-safe column reference, resolved to {@code alias.property} using the
	 * alias declared for the column's entity in the surrounding query (just
	 * {@code property} if no alias was declared).
	 *
	 * <p>
	 * Example: with {@code FROM(User.class).AS("u")}, {@code col(User::name)}
	 * &rarr; {@code u.name}.
	 * </p>
	 *
	 * @param col the entity getter, e.g. {@code User::name}; must be a method
	 *            reference (not an arbitrary lambda) and must not be {@code null}
	 * @param <T> the entity type owning the getter
	 * @return the column as an {@link Expr}
	 */
	public static <T> Expr col(Col<T> col) {
		String prop = Names.property(col);
		String entity = Names.entity(col);
		return Expr.of(ctx -> {
			String a = ctx.aliasFor(entity);
			return a == null ? prop : a + "." + prop;
		});
	}

	/**
	 * A column referenced by raw name — for derived/CTE/aliased columns that have
	 * no entity getter.
	 *
	 * <p>
	 * Example: {@code col("orderCount")} &rarr; {@code orderCount}.
	 * </p>
	 *
	 * @param derivedColumn the literal column text emitted verbatim into the HQL;
	 *                      must not be {@code null}, e.g. {@code "orderCount"}
	 * @return the column as an {@link Expr}
	 */
	public static Expr col(String derivedColumn) {
		return Expr.of(ctx -> derivedColumn);
	}

	/**
	 * A column qualified with an explicit alias and a raw field name — for
	 * CTE/derived columns that have no entity getter.
	 *
	 * <p>
	 * Example: {@code col("a", "name")} &rarr; {@code a.name}.
	 * </p>
	 *
	 * @param alias the range-variable alias; must not be {@code null}, e.g.
	 *              {@code "a"}
	 * @param field the field name; must not be {@code null}, e.g. {@code "name"}
	 * @return the aliased column as an {@link Expr}
	 */
	public static Expr col(String alias, String field) {
		return Expr.of(ctx -> alias + "." + field);
	}

	/**
	 * A type-safe column qualified with an explicit table alias — useful when the
	 * entity is not uniquely resolvable in the current scope (self-joins,
	 * correlated sub-queries, path joins).
	 *
	 * <p>
	 * Example: {@code col("o", Order::customerId)} &rarr; {@code o.customerId}.
	 * </p>
	 *
	 * @param alias the range-variable alias to qualify the column with; must not be
	 *              {@code null}, e.g. {@code "o"}
	 * @param col   the entity getter, e.g. {@code Order::customerId}; must be a
	 *              method reference and not {@code null}
	 * @param <T>   the entity type owning the getter
	 * @return the aliased column as an {@link Expr}
	 */
	public static <T> Expr col(String alias, Col<T> col) {
		String prop = Names.property(col);
		return Expr.of(ctx -> alias + "." + prop);
	}

	/**
	 * The whole entity, rendered as its alias. Package-private: users select a
	 * whole entity via the dedicated overloads {@link #SELECTㅤ(Class)},
	 * {@link #DISTINCTㅤ(Class)} and {@link #ㅤTREATㅤ(Class, Class)} instead of
	 * calling this directly.
	 */
	static <E> EntityExpr<E> entity(Class<E> type) {
		String name = type.getSimpleName();
		Expr render = Expr.of(ctx -> {
			String a = ctx.aliasFor(name);
			return a == null ? name : a;
		});
		return new EntityExpr<>(type, render);
	}

	/**
	 * A named query parameter, rendered as {@code :name}.
	 *
	 * <p>
	 * Example: {@code param("since")} &rarr; {@code :since}.
	 * </p>
	 *
	 * @param name the parameter name without the leading colon; must not be
	 *             {@code null} or blank, e.g. {@code "since"}
	 * @return the parameter as an {@link Expr}
	 */
	public static Expr param(String name) {
		return Expr.of(ctx -> ":" + name);
	}

	/**
	 * A literal value. Strings are quoted ({@code "PAID"} &rarr; {@code 'PAID'});
	 * numbers and booleans are emitted verbatim ({@code 1000} &rarr; {@code 1000}).
	 *
	 * <p>
	 * Example: {@code lit(0)} &rarr; {@code 0}; {@code lit("PAID")} &rarr;
	 * {@code 'PAID'}.
	 * </p>
	 *
	 * @param value the literal value; <b>{@code null} is allowed</b> and renders as
	 *              the SQL {@code null} literal
	 * @return the literal as an {@link Expr}
	 */
	public static Expr lit(Object value) {
		return Expr.val(value);
	}

	/**
	 * Wraps a sub-query so it can be used as a scalar value or projected column (in
	 * parentheses).
	 *
	 * <p>
	 * Example:
	 * {@code sub(SELECT(COUNT(col(Order::id))).FROM(Order.class)).AS("orderCount")}
	 * &rarr; {@code (select count(o.id) from Order o) as orderCount}.
	 * </p>
	 *
	 * @param subquery the nested query; must not be {@code null}
	 * @return the sub-query as an {@link Expr}
	 */
	public static Expr sub(Q subquery) {
		return Expr.of(ctx -> "(" + subquery.getHql() + ")");
	}

	// ===== aggregate / scalar functions =====

	/**
	 * The {@code count(...)} aggregate.
	 *
	 * <p>
	 * Example: {@code COUNT(col(Order::id))} &rarr; {@code count(o.id)}.
	 * </p>
	 *
	 * @param arg the counted expression (e.g. {@link #col(Col)}); must not be
	 *            {@code null}
	 * @return the aggregate as an {@link Expr}
	 */
	public static Expr COUNT(Object arg) {
		return fn("count", arg);
	}

	/**
	 * The {@code sum(...)} aggregate.
	 *
	 * <p>
	 * Example: {@code SUM(col(Order::total))} &rarr; {@code sum(o.total)}.
	 * </p>
	 *
	 * @param arg the summed expression; must not be {@code null}
	 * @return the aggregate as an {@link Expr}
	 */
	public static Expr SUM(Object arg) {
		return fn("sum", arg);
	}

	/**
	 * The {@code avg(...)} aggregate.
	 *
	 * <p>
	 * Example: {@code AVG(col(Order::discount))} &rarr; {@code avg(o.discount)}.
	 * </p>
	 *
	 * @param arg the averaged expression; must not be {@code null}
	 * @return the aggregate as an {@link Expr}
	 */
	public static Expr AVG(Object arg) {
		return fn("avg", arg);
	}

	/**
	 * The {@code max(...)} aggregate.
	 *
	 * <p>
	 * Example: {@code MAX(col(Order::total))} &rarr; {@code max(o.total)}.
	 * </p>
	 *
	 * @param arg the expression to take the maximum of; must not be {@code null}
	 * @return the aggregate as an {@link Expr}
	 */
	public static Expr MAX(Object arg) {
		return fn("max", arg);
	}

	/**
	 * The {@code min(...)} aggregate.
	 *
	 * <p>
	 * Example: {@code MIN(col(Order::total))} &rarr; {@code min(o.total)}.
	 * </p>
	 *
	 * @param arg the expression to take the minimum of; must not be {@code null}
	 * @return the aggregate as an {@link Expr}
	 */
	public static Expr MIN(Object arg) {
		return fn("min", arg);
	}

	/**
	 * The {@code size(...)} function (cardinality of a collection association).
	 *
	 * <p>
	 * Example: {@code SIZE(col(Customer::orders))} &rarr; {@code size(c.orders)}.
	 * </p>
	 *
	 * @param arg the collection-valued expression; must not be {@code null}
	 * @return the function call as an {@link Expr}
	 */
	public static Expr SIZE(Object arg) {
		return fn("size", arg);
	}

	/**
	 * The {@code coalesce(...)} function returning its first non-null argument.
	 *
	 * <p>
	 * Example: {@code COALESCE(AVG(col(Order::discount)), 0)} &rarr;
	 * {@code coalesce(avg(o.discount), 0)}.
	 * </p>
	 *
	 * @param args the candidate expressions, in order; must not be {@code null},
	 *             should contain at least two non-{@code null} elements
	 * @return the function call as an {@link Expr}
	 */
	public static Expr COALESCE(Object... args) {
		return Expr.of(ctx -> "coalesce(" + Expr.list(ctx, args) + ")");
	}

	/**
	 * The {@code nullif(a, b)} function (returns {@code null} when {@code a == b},
	 * else {@code a}).
	 *
	 * <p>
	 * Example: {@code NULLIF(col(Order::discount), 0)} &rarr;
	 * {@code nullif(o.discount, 0)}.
	 * </p>
	 *
	 * @param a the value to test and return; must not be {@code null}
	 * @param b the value compared against; must not be {@code null}
	 * @return the function call as an {@link Expr}
	 */
	public static Expr NULLIF(Object a, Object b) {
		Expr ea = Expr.val(a);
		Expr eb = Expr.val(b);
		return Expr.of(ctx -> "nullif(" + ea.render(ctx) + ", " + eb.render(ctx) + ")");
	}

	// --- Col overloads: take a bare getter reference directly, e.g.
	// COUNT(Order::id) instead of COUNT(col(Order::id)) ---

	/**
	 * {@code count(column)}, e.g. {@code COUNT(Order::id)} &rarr;
	 * {@code count(o.id)}.
	 *
	 * @param col the column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return the aggregate as an {@link Expr}
	 */
	public static <T> Expr COUNT(Col<T> col) {
		return fn("count", Expr.col(col));
	}

	/**
	 * {@code sum(column)}, e.g. {@code SUM(Order::total)} &rarr;
	 * {@code sum(o.total)}.
	 *
	 * @param col the column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return the aggregate as an {@link Expr}
	 */
	public static <T> Expr SUM(Col<T> col) {
		return fn("sum", Expr.col(col));
	}

	/**
	 * {@code avg(column)}, e.g. {@code AVG(Order::discount)} &rarr;
	 * {@code avg(o.discount)}.
	 *
	 * @param col the column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return the aggregate as an {@link Expr}
	 */
	public static <T> Expr AVG(Col<T> col) {
		return fn("avg", Expr.col(col));
	}

	/**
	 * {@code max(column)}, e.g. {@code MAX(Order::total)} &rarr;
	 * {@code max(o.total)}.
	 *
	 * @param col the column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return the aggregate as an {@link Expr}
	 */
	public static <T> Expr MAX(Col<T> col) {
		return fn("max", Expr.col(col));
	}

	/**
	 * {@code min(column)}, e.g. {@code MIN(Order::total)} &rarr;
	 * {@code min(o.total)}.
	 *
	 * @param col the column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return the aggregate as an {@link Expr}
	 */
	public static <T> Expr MIN(Col<T> col) {
		return fn("min", Expr.col(col));
	}

	/**
	 * {@code size(collection)}, e.g. {@code SIZE(Customer::orders)} &rarr;
	 * {@code size(c.orders)}.
	 *
	 * @param col the collection-valued column getter (method reference); must not
	 *            be {@code null}
	 * @param <T> the entity type owning the column
	 * @return the function call as an {@link Expr}
	 */
	public static <T> Expr SIZE(Col<T> col) {
		return fn("size", Expr.col(col));
	}

	/**
	 * {@code nullif(column, b)}, e.g. {@code NULLIF(Order::discount, 0)} &rarr;
	 * {@code nullif(o.discount, 0)}.
	 *
	 * @param a   the column getter (method reference); must not be {@code null}
	 * @param b   the comparand; must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return the function call as an {@link Expr}
	 */
	public static <T> Expr NULLIF(Col<T> a, Object b) {
		return NULLIF(Expr.col(a), b);
	}

	/**
	 * The {@code row_number()} window function.
	 *
	 * <p>
	 * Example: {@code ROW_NUMBER().OVER(PARTITIONㅤBY(col(Order::customerId)))}
	 * &rarr; {@code row_number() over (partition by o.customerId)}.
	 * </p>
	 *
	 * @return the function call as an {@link Expr}; combine with
	 *         {@link Expr#OVER(Expr)}
	 */
	public static Expr ROW_NUMBER() {
		return Expr.of(ctx -> "row_number()");
	}

	/**
	 * The {@code rank()} window function.
	 *
	 * <p>
	 * Example:
	 * {@code RANK().OVER(PARTITIONㅤBY(col(Order::customerId)).ORDERㅤBY(Order::total).DESC())}.
	 * </p>
	 *
	 * @return the function call as an {@link Expr}; combine with
	 *         {@link Expr#OVER(Expr)}
	 */
	public static Expr RANK() {
		return Expr.of(ctx -> "rank()");
	}

	/**
	 * A {@code distinct} projection modifier for a {@code SELECT} list.
	 *
	 * <p>
	 * Example: {@code SELECT(DISTINCT(col(Order::customerId)))} &rarr;
	 * {@code select distinct o.customerId}.
	 * </p>
	 *
	 * @param cols the distinct expressions; must not be {@code null}, typically a
	 *             single column or entity
	 * @return the modified projection as an {@link Expr}
	 */
	public static Expr DISTINCT(Object... cols) {
		Expr base = Expr.of(ctx -> "distinct " + Expr.list(ctx, cols));
		// Preserve the single-entity marker so SELECT(DISTINCT(entity)) still yields a
		// typed result list.
		if (cols.length == 1 && cols[0] instanceof EntityExpr) {
			return preserveEntity((EntityExpr<?>) cols[0], base);
		}
		return base;
	}

	private static <E> EntityExpr<E> preserveEntity(EntityExpr<E> e, Expr base) {
		return new EntityExpr<>(e.type, base);
	}

	/**
	 * {@code distinct} of a whole entity — shorthand for
	 * {@code DISTINCT(entity(type))}. The selected type is preserved, so
	 * {@code SELECT(DISTINCT(...))} still threads through to a cast-free
	 * {@link Q#via}.
	 *
	 * <p>
	 * Example:
	 * {@code SELECT(DISTINCT(Customer.class)).FROM(Customer.class).AS("c")} &rarr;
	 * {@code select distinct c from Customer c}. The result still works with
	 * {@link Q#via(jakarta.persistence.EntityManager)}.
	 * </p>
	 *
	 * @param entityType the distinct entity class; must not be {@code null}, e.g.
	 *                   {@code Customer.class}
	 * @param <E>        the distinct entity type
	 * @return the distinct entity projection
	 */
	public static <E> EntityExpr<E> DISTINCTㅤ(Class<E> entityType) {
		EntityExpr<E> e = entity(entityType);
		Expr base = Expr.of(ctx -> "distinct " + e.render(ctx));
		return new EntityExpr<>(entityType, base);
	}

	/**
	 * A constructor (DTO) projection: {@code new fully.qualified.Dto(args...)}.
	 *
	 * <p>
	 * Example:
	 * {@code NEW(CustomerSummary.class, Customer::id, COUNT(col("o", Order::id)))}
	 * &rarr; {@code new linqava.CustomerSummary(c.id, count(o.id))}.
	 * </p>
	 *
	 * @param dto  the DTO class whose constructor is invoked; must not be
	 *             {@code null}. The fully-qualified name ({@link Class#getName()})
	 *             is emitted.
	 * @param args the constructor arguments, in order; must not be {@code null}
	 * @return the constructor projection as an {@link Expr}
	 */
	public static Expr NEW(Class<?> dto, Object... args) {
		return Expr.of(ctx -> "new " + dto.getName() + "(" + Expr.list(ctx, args) + ")");
	}

	/**
	 * A constructor (DTO) projection whose first argument is a bare getter
	 * reference, e.g.
	 * {@code NEW(CustomerSummary.class, Customer::id, col(Customer::name))} —
	 * avoids wrapping the leading argument in {@link #col(Col)}.
	 *
	 * @param dto   the DTO class whose constructor is invoked; must not be
	 *              {@code null}. The fully-qualified name ({@link Class#getName()})
	 *              is emitted.
	 * @param first the first constructor argument (method reference); must not be
	 *              {@code null}
	 * @param rest  the remaining constructor arguments, in order; must not be
	 *              {@code null}, may be empty
	 * @param <T>   the entity type owning the first argument's column
	 * @return the constructor projection as an {@link Expr}
	 */
	public static <T> Expr NEW(Class<?> dto, Col<T> first, Object... rest) {
		Object[] args = new Object[rest.length + 1];
		args[0] = Expr.col(first);
		System.arraycopy(rest, 0, args, 1, rest.length);
		return Expr.of(ctx -> "new " + dto.getName() + "(" + Expr.list(ctx, args) + ")");
	}

	/**
	 * A {@code treat(expr as Subtype)} down-cast for polymorphic associations;
	 * follow with {@link Expr#ᐧ(Col)} to access a subtype field.
	 *
	 * <p>
	 * Example:
	 * {@code TREAT(expr, CreditCardPayment.class).ᐧ(CreditCardPayment::cardType)}
	 * &rarr; {@code treat(... as CreditCardPayment).cardType}.
	 * </p>
	 *
	 * @param expr the expression to cast; must not be {@code null}
	 * @param type the target subtype; must not be {@code null}. Its simple name is
	 *             emitted.
	 * @return the cast as an {@link Expr}
	 */
	public static Expr ㅤTREATㅤ(Object expr, Class<?> type) {
		Expr e = Expr.val(expr);
		return Expr.of(ctx -> "treat(" + e.render(ctx) + " as " + type.getSimpleName() + ")");
	}

	/**
	 * A {@code treat(rootEntity as Subtype)} down-cast — shorthand for
	 * {@code TREAT(entity(rootType), subtype)}; follow with {@link Expr#ᐧ(Col)} to
	 * access a subtype field.
	 *
	 * <p>
	 * Example:
	 * {@code TREAT(Payment.class, CreditCardPayment.class).ᐧ(CreditCardPayment::cardType)}
	 * &rarr; {@code treat(p as CreditCardPayment).cardType}.
	 * </p>
	 *
	 * @param rootType the entity being cast (its alias is emitted); must not be
	 *                 {@code null}, e.g. {@code Payment.class}
	 * @param subtype  the target subtype; must not be {@code null}. Its simple name
	 *                 is emitted.
	 * @return the cast as an {@link Expr}
	 */
	public static Expr ㅤTREATㅤ(Class<?> rootType, Class<?> subtype) {
		return ㅤTREATㅤ(entity(rootType), subtype);
	}

	// ===== window helper =====

	/**
	 * The {@code partition by ...} clause of a window; chain
	 * {@link Expr#ORDERㅤBY(Col)} and {@link Expr#DESC()} for ordering.
	 *
	 * <p>
	 * Example:
	 * {@code PARTITIONㅤBY(col(Order::customerId)).ORDERㅤBY(Order::total).DESC()}
	 * &rarr; {@code partition by o.customerId order by o.total desc}.
	 * </p>
	 *
	 * @param cols the partitioning columns, in order; must not be {@code null}
	 * @return the window specification as an {@link Expr}, to be passed to
	 *         {@link Expr#OVER(Expr)}
	 */
	public static Expr ㅤPARTITIONㅤBYㅤ(Object... cols) {
		return Expr.of(ctx -> "partition by " + Expr.list(ctx, cols));
	}

	/**
	 * The {@code partition by ...} clause of a window, with type-safe bare column
	 * references, e.g. {@code PARTITIONㅤBY(Order::customerId)} &rarr;
	 * {@code partition by o.customerId}.
	 *
	 * @param cols the partitioning columns, in order; must not be {@code null}
	 * @param <T>  the entity type owning the columns
	 * @return the window specification as an {@link Expr}, to be passed to
	 *         {@link Expr#OVER(Expr)}
	 */
	@SafeVarargs
	public static <T> Expr ㅤPARTITIONㅤBYㅤ(Col<T>... cols) {
		Object[] exprs = new Object[cols.length];
		for (int i = 0; i < cols.length; i++) {
			exprs[i] = Expr.col(cols[i]);
		}
		return Expr.of(ctx -> "partition by " + Expr.list(ctx, exprs));
	}

	// ===== boolean predicate builders (flat, lambda-free composition) =====

	/**
	 * Equality predicate ({@code =}) with a type-safe left column.
	 *
	 * <p>
	 * Example: {@code ᆖ(Order::status, "PAID")} &rarr; {@code o.status = 'PAID'}.
	 * </p>
	 *
	 * @param l   the left column getter (method reference); must not be
	 *            {@code null}
	 * @param r   the right operand: an {@link Expr}, a {@link #param(String)}, a
	 *            sub-query {@link Q} or a literal. To test for null use
	 *            {@link Cond#ISㅤNULL(Col)} — passing {@code null} here renders the
	 *            literal text {@code null}.
	 * @param <T> the entity type owning the left column
	 * @return a leaf predicate; combine with {@link #ㅤANDㅤ(Cond...)} /
	 *         {@link #ㅤORㅤ(Cond...)}
	 */
	public static <T> Cond ㅤᆖㅤ(Col<T> l, Object r) {
		return new Cond().ᆖ(l, r);
	} // =

	/**
	 * Equality predicate ({@code =}) with an expression left operand.
	 *
	 * @param l the left operand (e.g. an aggregate {@link Expr}); must not be
	 *          {@code null}
	 * @param r the right operand (see {@link #ㅤᆖㅤ(Col, Object)}); {@code null}
	 *          renders as literal {@code null}
	 * @return a leaf predicate
	 */
	public static Cond ㅤᆖㅤ(Object l, Object r) {
		return new Cond().ᆖ(l, r);
	}

	/**
	 * Less-than predicate ({@code <}) with a type-safe left column.
	 *
	 * <p>
	 * Example: {@code ᐸ(Order::discount, 5)} &rarr; {@code o.discount < 5}.
	 * </p>
	 *
	 * @param l   the left column getter; must not be {@code null}
	 * @param r   the right operand; must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @return a leaf predicate
	 */
	public static <T> Cond ㅤᐸㅤ(Col<T> l, Object r) {
		return new Cond().ᐸ(l, r);
	} // <

	/**
	 * Less-than predicate ({@code <}) with an expression left operand.
	 *
	 * @param l the left operand; must not be {@code null}
	 * @param r the right operand; must not be {@code null}
	 * @return a leaf predicate
	 */
	public static Cond ᐸ(Object l, Object r) {
		return new Cond().ᐸ(l, r);
	}

	/**
	 * Greater-than predicate ({@code >}) with a type-safe left column.
	 *
	 * <p>
	 * Example: {@code ᐳ(Order::total, 100)} &rarr; {@code o.total > 100}.
	 * </p>
	 *
	 * @param l   the left column getter; must not be {@code null}
	 * @param r   the right operand; must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @return a leaf predicate
	 */
	public static <T> Cond ㅤᐳㅤ(Col<T> l, Object r) {
		return new Cond().ᐳ(l, r);
	} // >

	/**
	 * Greater-than predicate ({@code >}) with an expression left operand.
	 *
	 * @param l the left operand; must not be {@code null}
	 * @param r the right operand; must not be {@code null}
	 * @return a leaf predicate
	 */
	public static Cond ㅤᐳㅤ(Object l, Object r) {
		return new Cond().ㅤᐳㅤ(l, r);
	}

	/**
	 * Less-than-or-equal predicate ({@code <=}) with a type-safe left column.
	 *
	 * <p>
	 * Example: {@code ᐸᆖ(Order::discount, 50)} &rarr; {@code o.discount <= 50}.
	 * </p>
	 *
	 * @param l   the left column getter; must not be {@code null}
	 * @param r   the right operand; must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @return a leaf predicate
	 */
	public static <T> Cond ㅤᐸᆖㅤ(Col<T> l, Object r) {
		return new Cond().ᐸᆖ(l, r);
	} // <=

	/**
	 * Less-than-or-equal predicate ({@code <=}) with an expression left operand.
	 *
	 * @param l the left operand; must not be {@code null}
	 * @param r the right operand; must not be {@code null}
	 * @return a leaf predicate
	 */
	public static Cond ㅤᐸᆖㅤ(Object l, Object r) {
		return new Cond().ᐸᆖ(l, r);
	}

	/**
	 * Greater-than-or-equal predicate ({@code >=}) with a type-safe left column.
	 *
	 * <p>
	 * Example: {@code ᐳᆖ(Order::total, 1000)} &rarr; {@code o.total >= 1000}.
	 * </p>
	 *
	 * @param l   the left column getter; must not be {@code null}
	 * @param r   the right operand; must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @return a leaf predicate
	 */
	public static <T> Cond ㅤᐳᆖㅤ(Col<T> l, Object r) {
		return new Cond().ᐳᆖ(l, r);
	} // >=

	/**
	 * Greater-than-or-equal predicate ({@code >=}) with an expression left operand.
	 *
	 * @param l the left operand; must not be {@code null}
	 * @param r the right operand; must not be {@code null}
	 * @return a leaf predicate
	 */
	public static Cond ㅤᐳᆖㅤ(Object l, Object r) {
		return new Cond().ᐳᆖ(l, r);
	}

	/**
	 * Not-equal predicate ({@code <>}) with a type-safe left column.
	 *
	 * <p>
	 * Example: {@code ᐸᐳ(Order::status, "CANCELLED")} &rarr;
	 * {@code o.status <> 'CANCELLED'}.
	 * </p>
	 *
	 * @param l   the left column getter; must not be {@code null}
	 * @param r   the right operand; must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @return a leaf predicate
	 */
	public static <T> Cond ㅤᐸᐳㅤ(Col<T> l, Object r) {
		return new Cond().ᐸᐳ(l, r);
	} // <>

	/**
	 * Not-equal predicate ({@code <>}) with an expression left operand.
	 *
	 * @param l the left operand; must not be {@code null}
	 * @param r the right operand; must not be {@code null}
	 * @return a leaf predicate
	 */
	public static Cond ㅤᐸᐳㅤ(Object l, Object r) {
		return new Cond().ᐸᐳ(l, r);
	}

	/**
	 * Membership predicate ({@code in (...)}) with a type-safe left column.
	 *
	 * <p>
	 * Example: {@code IN(Product::categoryId, subquery)} &rarr;
	 * {@code p.categoryId in (select ...)}.
	 * </p>
	 *
	 * @param l   the left column getter (method reference); must not be
	 *            {@code null}
	 * @param r   the right side, typically a sub-query {@link Q}; must not be
	 *            {@code null}
	 * @param <T> the entity type owning the left column
	 * @return a leaf predicate
	 */
	public static <T> Cond ㅤINㅤ(Col<T> l, Object r) {
		return new Cond().IN(l, r);
	}

	/**
	 * Membership predicate ({@code in (...)}) with an expression left operand.
	 *
	 * @param l the left operand; must not be {@code null}
	 * @param r the right side (typically a sub-query); must not be {@code null}
	 * @return a leaf predicate
	 */
	public static Cond ㅤINㅤ(Object l, Object r) {
		return new Cond().IN(l, r);
	}

	/**
	 * Pattern-match predicate ({@code like}) with a type-safe left column.
	 *
	 * <p>
	 * Example: {@code LIKE(Supplier::iban, param("p"))} &rarr;
	 * {@code iban like :p}.
	 * </p>
	 *
	 * @param l   the left column getter (method reference); must not be
	 *            {@code null}
	 * @param r   the pattern (literal/{@link #param(String)}); must not be
	 *            {@code null}
	 * @param <T> the entity type owning the left column
	 * @return a leaf predicate
	 */
	public static <T> Cond ㅤLIKEㅤ(Col<T> l, Object r) {
		return new Cond().LIKE(l, r);
	}

	/**
	 * Pattern-match predicate ({@code like}) with an expression left operand.
	 *
	 * @param l the left operand; must not be {@code null}
	 * @param r the pattern; must not be {@code null}
	 * @return a leaf predicate
	 */
	public static Cond ㅤLIKEㅤ(Object l, Object r) {
		return new Cond().LIKE(l, r);
	}

	/**
	 * Existence predicate ({@code exists (...)}).
	 *
	 * <p>
	 * Example: {@code EXISTS(SELECT(lit(1)).FROM(Order.class)...)} &rarr;
	 * {@code exists (select 1 ...)}.
	 * </p>
	 *
	 * @param subquery the sub-query (a {@link Q}); must not be {@code null}
	 * @return a leaf predicate
	 */
	public static Cond ㅤEXISTSㅤ(Object subquery) {
		return new Cond().EXISTS(subquery);
	}

	/**
	 * Null-test predicate ({@code is null}), e.g.
	 * {@code ISㅤNULL(Employee::managerId)} &rarr; {@code e.managerId is null}.
	 *
	 * @param c   the column getter; must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return a leaf predicate
	 */
	public static <T> Cond ㅤISㅤNULLㅤ(Col<T> c) {
		return new Cond().ISㅤNULL(c);
	}

	/**
	 * Not-null-test predicate ({@code is not null}).
	 *
	 * @param c   the column getter; must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return a leaf predicate
	 */
	public static <T> Cond ㅤISㅤNOTㅤNULLㅤ(Col<T> c) {
		return new Cond().ISㅤNOTㅤNULL(c);
	}

	/**
	 * Empty-collection predicate ({@code is empty}).
	 *
	 * @param c   the collection-valued column getter; must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return a leaf predicate
	 */
	public static <T> Cond ㅤISㅤEMPTYㅤ(Col<T> c) {
		return new Cond().ISㅤEMPTY(c);
	}

	/**
	 * Non-empty-collection predicate ({@code is not empty}), e.g.
	 * {@code ISㅤNOTㅤEMPTY(Customer::orders)} &rarr; {@code c.orders is not empty}.
	 *
	 * @param c   the collection-valued column getter; must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return a leaf predicate
	 */
	public static <T> Cond ㅤISㅤNOTㅤEMPTYㅤ(Col<T> c) {
		return new Cond().ISㅤNOTㅤEMPTY(c);
	}

	/**
	 * Non-empty-collection predicate ({@code is not empty}) with an expression
	 * operand.
	 *
	 * @param c the collection-valued expression; must not be {@code null}
	 * @return a leaf predicate
	 */
	public static Cond ㅤISㅤNOTㅤEMPTYㅤ(Object c) {
		return new Cond().ISㅤNOTㅤEMPTY(c);
	}

	/**
	 * Collection-membership predicate ({@code value member of collection}), e.g.
	 * {@code MEMBERㅤOF(param("product"), col(Customer::wishlist))} &rarr;
	 * {@code :product member of c.wishlist}.
	 *
	 * @param value      the element expression (literal/{@link #param(String)});
	 *                   must not be {@code null}
	 * @param collection the collection-valued expression; must not be {@code null}
	 * @return a leaf predicate
	 */
	public static Cond ㅤMEMBERㅤOFㅤ(Object value, Object collection) {
		return new Cond().MEMBERㅤOF(value, collection);
	}

	/**
	 * Collection-membership predicate ({@code value member of collection}) with a
	 * type-safe collection column, e.g.
	 * {@code MEMBERㅤOF(param("product"), Customer::wishlist)} &rarr;
	 * {@code :product member of c.wishlist}.
	 *
	 * @param value      the element expression (literal/{@link #param(String)});
	 *                   must not be {@code null}
	 * @param collection the collection-valued column getter (method reference);
	 *                   must not be {@code null}
	 * @param <T>        the entity type owning the column
	 * @return a leaf predicate
	 */
	public static <T> Cond ㅤMEMBERㅤOFㅤ(Object value, Col<T> collection) {
		return new Cond().MEMBERㅤOF(value, Expr.col(collection));
	}

	/**
	 * Conjunction of predicates ({@code and}), parenthesized as a group.
	 *
	 * <p>
	 * Example: {@code AND(ᆖ(Order::status, "PAID"), ᐳ(Order::total, 100))} &rarr;
	 * {@code (o.status = 'PAID' and o.total > 100)}.
	 * </p>
	 *
	 * @param parts the predicates to combine, in order; must not be {@code null}
	 *              and must contain at least one non-{@code null} element
	 * @return a combined predicate
	 */
	public static Cond ㅤANDㅤ(Cond... parts) {
		return Cond.combine("and", parts);
	}

	/**
	 * Disjunction of predicates ({@code or}), parenthesized as a group.
	 *
	 * <p>
	 * Example: {@code OR(ᐳ(Order::total, 100), ᐸ(Order::discount, 5))} &rarr;
	 * {@code (o.total > 100 or o.discount < 5)}.
	 * </p>
	 *
	 * @param parts the predicates to combine, in order; must not be {@code null}
	 *              and must contain at least one non-{@code null} element
	 * @return a combined predicate
	 */
	public static Cond ㅤORㅤ(Cond... parts) {
		return Cond.combine("or", parts);
	}

	/**
	 * Negation of a predicate ({@code not (...)}).
	 *
	 * <p>
	 * Example: {@code NOT(ᆖ(Order::status, "PAID"))} &rarr;
	 * {@code not (o.status = 'PAID')}.
	 * </p>
	 *
	 * @param predicate the predicate to negate; must not be {@code null}
	 * @return the negated predicate
	 */
	public static Cond ㅤNOTㅤ(Cond predicate) {
		return Cond.negate(predicate);
	}

	// ===== CASE expression =====

	/**
	 * Starts a {@code CASE WHEN ... THEN ... [ELSE ...] END} expression.
	 *
	 * <p>
	 * Example:
	 * {@code CASE().WHEN(ᐳᆖ(Order::total, 1000)).THEN("GOLD").ELSE("BRONZE").END()}.
	 * </p>
	 *
	 * @return a new {@link Case} builder
	 */
	public static Case CASE() {
		return new Case();
	}

	private static Expr fn(String name, Object arg) {
		Expr a = Expr.val(arg);
		return Expr.of(ctx -> name + "(" + a.render(ctx) + ")");
	}
}
