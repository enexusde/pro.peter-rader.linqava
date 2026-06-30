package linqava;

/**
 * Static entry points, SQL functions and predicate builders of the linqava DSL.
 *
 * <p>Intended to be used with a static import so the query reads like SQL:</p>
 * <pre>{@code
 * import static linqava.Linq.*;
 *
 * Q q = SELECT(c(User::id))
 *           .FROM(User.class)
 *           .WHERE($ -> $.二(User::Name, "John"));
 * q.getHql(); // "select id from User where Name = 'John'"
 * }</pre>
 *
 * <p>General null policy: unless a method explicitly states otherwise, no argument may be
 * {@code null}; passing {@code null} throws {@link NullPointerException} either immediately or when
 * {@link Q#getHql()} is called. The documented exception is {@link #lit(Object)}, where {@code null}
 * is rendered as the SQL {@code null} literal.</p>
 */
public final class Linq {

	private Linq() {
	}

	// ===== query entry points =====

	/**
	 * Starts a {@code SELECT} query.
	 *
	 * <p>Example: {@code SELECT(c(Order::id), COUNT(c(Order::id)))} &rarr; {@code select o.id, count(o.id) ...}</p>
	 *
	 * @param cols the projected columns/expressions, in order; typically {@link #c(Col)},
	 *             aggregates such as {@link #COUNT(Object)}, or {@link #entity(Class)}. Must not be
	 *             {@code null} and should not contain {@code null} elements (a {@code null} element
	 *             renders as the literal text {@code null}). May be empty.
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#FROM(Class) FROM} next
	 */
	public static SelectStep SELECT(Object... cols) { return new SelectStep(new Q().addSelect(cols)); }

	/**
	 * Starts a {@code SELECT} whose first column is a bare getter reference, e.g.
	 * {@code SELECT(Order::id, CASE()...END())} — avoids wrapping the leading column in {@link #c(Col)}.
	 *
	 * @param first the first column getter (method reference); must not be {@code null}
	 * @param rest  the remaining columns/expressions, in order; must not be {@code null}, may be empty
	 * @param <A>   the entity type owning the first column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#FROM(Class) FROM} next
	 */
	public static <A> SelectStep SELECT(Col<A> first, Object... rest) { return new SelectStep(new Q().addSelect(first, rest)); }

	/**
	 * Starts a {@code SELECT} of a whole entity — shorthand for {@code SELECT(entity(type))}.
	 *
	 * <p>Example: {@code SELECT(Order.class).FROM(Order.class).AS("o")} &rarr; {@code select o from Order o}.
	 * Such single-entity queries can be run with {@link Q#via(jakarta.persistence.EntityManager)}.</p>
	 *
	 * @param entityType the selected entity class; must not be {@code null}, e.g. {@code Order.class}
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#FROM(Class) FROM} next
	 */
	public static SelectStep SELECT(Class<?> entityType) { return new SelectStep(new Q().addSelect(entity(entityType))); }

	/**
	 * Starts a query with a common table expression ({@code WITH name AS (definition)}).
	 *
	 * <p>Example: {@code WITH("activeUsers", SELECT(...).FROM(User.class)...).SELECT(...).FROM("activeUsers")}</p>
	 *
	 * @param name       the CTE name, later referenced via {@link SelectStep#FROM(String)}; must not be
	 *                   {@code null} or blank, e.g. {@code "activeUsers"}
	 * @param definition the sub-query defining the CTE; must not be {@code null}
	 * @return the {@code WITH} phase; chain {@link WithStep#WITH(String, Q)} for further CTEs, then {@code SELECT}
	 */
	public static WithStep WITH(String name, Q definition) { return new WithStep(new Q().addCte(name, definition, false)); }

	/**
	 * Starts a query with a recursive common table expression ({@code WITH RECURSIVE name AS (...)}).
	 *
	 * <p>Example: the {@code definition} is usually an anchor query joined to the recursive part via
	 * {@link Q#UNION‿ALL(Q)}.</p>
	 *
	 * @param name       the CTE name (also used inside the recursive part as a {@link Q#JOIN(String)}
	 *                   target); must not be {@code null} or blank
	 * @param definition the recursive sub-query (anchor {@code UNION ALL} recursive step); must not be {@code null}
	 * @return the {@code WITH} phase; continue with {@code SELECT}
	 */
	public static WithStep WITH‿RECURSIVE(String name, Q definition) { return new WithStep(new Q().addCte(name, definition, true)); }

	// ===== column / value helpers =====

	/**
	 * A type-safe column reference, resolved to {@code alias.property} using the alias declared for
	 * the column's entity in the surrounding query (just {@code property} if no alias was declared).
	 *
	 * <p>Example: with {@code FROM(User.class).AS("u")}, {@code c(User::name)} &rarr; {@code u.name}.</p>
	 *
	 * @param col the entity getter, e.g. {@code User::name}; must be a method reference (not an
	 *            arbitrary lambda) and must not be {@code null}
	 * @param <T> the entity type owning the getter
	 * @return the column as an {@link Expr}
	 */
	public static <T> Expr c(Col<T> col) {
		String prop = Names.property(col);
		String entity = Names.entity(col);
		return Expr.of(ctx -> {
			String a = ctx.aliasFor(entity);
			return a == null ? prop : a + "." + prop;
		});
	}

	/**
	 * A column referenced by raw name — for derived/CTE/aliased columns that have no entity getter.
	 *
	 * <p>Example: {@code c("a.name")} &rarr; {@code a.name}; {@code c("orderCount")} &rarr; {@code orderCount}.</p>
	 *
	 * @param derivedColumn the literal column text emitted verbatim into the HQL; must not be
	 *                      {@code null}, e.g. {@code "a.name"}
	 * @return the column as an {@link Expr}
	 */
	public static Expr c(String derivedColumn) {
		return Expr.of(ctx -> derivedColumn);
	}

	/**
	 * A type-safe column qualified with an explicit table alias — useful when the entity is not
	 * uniquely resolvable in the current scope (self-joins, correlated sub-queries, path joins).
	 *
	 * <p>Example: {@code c("o", Order::customerId)} &rarr; {@code o.customerId}.</p>
	 *
	 * @param alias the range-variable alias to qualify the column with; must not be {@code null}, e.g. {@code "o"}
	 * @param col   the entity getter, e.g. {@code Order::customerId}; must be a method reference and not {@code null}
	 * @param <T>   the entity type owning the getter
	 * @return the aliased column as an {@link Expr}
	 */
	public static <T> Expr c(String alias, Col<T> col) {
		String prop = Names.property(col);
		return Expr.of(ctx -> alias + "." + prop);
	}

	/**
	 * The whole entity, rendered as its alias. Package-private: users select a whole entity via the
	 * dedicated overloads {@link #SELECT(Class)}, {@link #DISTINCT(Class)} and {@link #TREAT(Class, Class)}
	 * instead of calling this directly.
	 */
	static Expr entity(Class<?> type) {
		String name = type.getSimpleName();
		Expr render = Expr.of(ctx -> {
			String a = ctx.aliasFor(name);
			return a == null ? name : a;
		});
		return new EntityExpr(type, render);
	}

	/**
	 * A named query parameter, rendered as {@code :name}.
	 *
	 * <p>Example: {@code param("since")} &rarr; {@code :since}.</p>
	 *
	 * @param name the parameter name without the leading colon; must not be {@code null} or blank, e.g. {@code "since"}
	 * @return the parameter as an {@link Expr}
	 */
	public static Expr param(String name) {
		return Expr.of(ctx -> ":" + name);
	}

	/**
	 * A literal value. Strings are quoted ({@code "PAID"} &rarr; {@code 'PAID'}); numbers and
	 * booleans are emitted verbatim ({@code 1000} &rarr; {@code 1000}).
	 *
	 * <p>Example: {@code lit(0)} &rarr; {@code 0}; {@code lit("PAID")} &rarr; {@code 'PAID'}.</p>
	 *
	 * @param value the literal value; <b>{@code null} is allowed</b> and renders as the SQL
	 *              {@code null} literal
	 * @return the literal as an {@link Expr}
	 */
	public static Expr lit(Object value) {
		return Expr.val(value);
	}

	/**
	 * Wraps a sub-query so it can be used as a scalar value or projected column (in parentheses).
	 *
	 * <p>Example: {@code sub(SELECT(COUNT(c(Order::id))).FROM(Order.class)).AS("orderCount")}
	 * &rarr; {@code (select count(o.id) from Order o) as orderCount}.</p>
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
	 * <p>Example: {@code COUNT(c(Order::id))} &rarr; {@code count(o.id)}.</p>
	 *
	 * @param arg the counted expression (e.g. {@link #c(Col)}); must not be {@code null}
	 * @return the aggregate as an {@link Expr}
	 */
	public static Expr COUNT(Object arg) { return fn("count", arg); }

	/**
	 * The {@code sum(...)} aggregate.
	 *
	 * <p>Example: {@code SUM(c(Order::total))} &rarr; {@code sum(o.total)}.</p>
	 *
	 * @param arg the summed expression; must not be {@code null}
	 * @return the aggregate as an {@link Expr}
	 */
	public static Expr SUM(Object arg) { return fn("sum", arg); }

	/**
	 * The {@code avg(...)} aggregate.
	 *
	 * <p>Example: {@code AVG(c(Order::discount))} &rarr; {@code avg(o.discount)}.</p>
	 *
	 * @param arg the averaged expression; must not be {@code null}
	 * @return the aggregate as an {@link Expr}
	 */
	public static Expr AVG(Object arg) { return fn("avg", arg); }

	/**
	 * The {@code max(...)} aggregate.
	 *
	 * <p>Example: {@code MAX(c(Order::total))} &rarr; {@code max(o.total)}.</p>
	 *
	 * @param arg the expression to take the maximum of; must not be {@code null}
	 * @return the aggregate as an {@link Expr}
	 */
	public static Expr MAX(Object arg) { return fn("max", arg); }

	/**
	 * The {@code min(...)} aggregate.
	 *
	 * <p>Example: {@code MIN(c(Order::total))} &rarr; {@code min(o.total)}.</p>
	 *
	 * @param arg the expression to take the minimum of; must not be {@code null}
	 * @return the aggregate as an {@link Expr}
	 */
	public static Expr MIN(Object arg) { return fn("min", arg); }

	/**
	 * The {@code size(...)} function (cardinality of a collection association).
	 *
	 * <p>Example: {@code SIZE(c(Customer::orders))} &rarr; {@code size(c.orders)}.</p>
	 *
	 * @param arg the collection-valued expression; must not be {@code null}
	 * @return the function call as an {@link Expr}
	 */
	public static Expr SIZE(Object arg) { return fn("size", arg); }

	/**
	 * The {@code coalesce(...)} function returning its first non-null argument.
	 *
	 * <p>Example: {@code COALESCE(AVG(c(Order::discount)), 0)} &rarr; {@code coalesce(avg(o.discount), 0)}.</p>
	 *
	 * @param args the candidate expressions, in order; must not be {@code null}, should contain at
	 *             least two non-{@code null} elements
	 * @return the function call as an {@link Expr}
	 */
	public static Expr COALESCE(Object... args) {
		return Expr.of(ctx -> "coalesce(" + Expr.list(ctx, args) + ")");
	}

	/**
	 * The {@code nullif(a, b)} function (returns {@code null} when {@code a == b}, else {@code a}).
	 *
	 * <p>Example: {@code NULLIF(c(Order::discount), 0)} &rarr; {@code nullif(o.discount, 0)}.</p>
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

	// --- Col overloads: take a bare getter reference directly, e.g. COUNT(Order::id) instead of COUNT(c(Order::id)) ---

	/**
	 * {@code count(column)}, e.g. {@code COUNT(Order::id)} &rarr; {@code count(o.id)}.
	 *
	 * @param col the column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return the aggregate as an {@link Expr}
	 */
	public static <T> Expr COUNT(Col<T> col) { return fn("count", Expr.col(col)); }

	/**
	 * {@code sum(column)}, e.g. {@code SUM(Order::total)} &rarr; {@code sum(o.total)}.
	 *
	 * @param col the column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return the aggregate as an {@link Expr}
	 */
	public static <T> Expr SUM(Col<T> col) { return fn("sum", Expr.col(col)); }

	/**
	 * {@code avg(column)}, e.g. {@code AVG(Order::discount)} &rarr; {@code avg(o.discount)}.
	 *
	 * @param col the column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return the aggregate as an {@link Expr}
	 */
	public static <T> Expr AVG(Col<T> col) { return fn("avg", Expr.col(col)); }

	/**
	 * {@code max(column)}, e.g. {@code MAX(Order::total)} &rarr; {@code max(o.total)}.
	 *
	 * @param col the column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return the aggregate as an {@link Expr}
	 */
	public static <T> Expr MAX(Col<T> col) { return fn("max", Expr.col(col)); }

	/**
	 * {@code min(column)}, e.g. {@code MIN(Order::total)} &rarr; {@code min(o.total)}.
	 *
	 * @param col the column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return the aggregate as an {@link Expr}
	 */
	public static <T> Expr MIN(Col<T> col) { return fn("min", Expr.col(col)); }

	/**
	 * {@code size(collection)}, e.g. {@code SIZE(Customer::orders)} &rarr; {@code size(c.orders)}.
	 *
	 * @param col the collection-valued column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return the function call as an {@link Expr}
	 */
	public static <T> Expr SIZE(Col<T> col) { return fn("size", Expr.col(col)); }

	/**
	 * {@code nullif(column, b)}, e.g. {@code NULLIF(Order::discount, 0)} &rarr; {@code nullif(o.discount, 0)}.
	 *
	 * @param a   the column getter (method reference); must not be {@code null}
	 * @param b   the comparand; must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return the function call as an {@link Expr}
	 */
	public static <T> Expr NULLIF(Col<T> a, Object b) { return NULLIF(Expr.col(a), b); }

	/**
	 * The {@code row_number()} window function.
	 *
	 * <p>Example: {@code ROW_NUMBER().OVER(PARTITION‿BY(c(Order::customerId)))} &rarr;
	 * {@code row_number() over (partition by o.customerId)}.</p>
	 *
	 * @return the function call as an {@link Expr}; combine with {@link Expr#OVER(Expr)}
	 */
	public static Expr ROW_NUMBER() { return Expr.of(ctx -> "row_number()"); }

	/**
	 * The {@code rank()} window function.
	 *
	 * <p>Example: {@code RANK().OVER(PARTITION‿BY(c(Order::customerId)).ORDER‿BY(Order::total).DESC())}.</p>
	 *
	 * @return the function call as an {@link Expr}; combine with {@link Expr#OVER(Expr)}
	 */
	public static Expr RANK() { return Expr.of(ctx -> "rank()"); }

	/**
	 * A {@code distinct} projection modifier for a {@code SELECT} list.
	 *
	 * <p>Example: {@code SELECT(DISTINCT(c(Order::customerId)))} &rarr; {@code select distinct o.customerId}.</p>
	 *
	 * @param cols the distinct expressions; must not be {@code null}, typically a single column or entity
	 * @return the modified projection as an {@link Expr}
	 */
	public static Expr DISTINCT(Object... cols) {
		Expr base = Expr.of(ctx -> "distinct " + Expr.list(ctx, cols));
		// Preserve the single-entity marker so SELECT(DISTINCT(entity)) still yields a typed result list.
		if (cols.length == 1 && cols[0] instanceof EntityExpr) {
			return new EntityExpr(((EntityExpr) cols[0]).type, base);
		}
		return base;
	}

	/**
	 * {@code distinct} of a whole entity — shorthand for {@code DISTINCT(entity(type))}.
	 *
	 * <p>Example: {@code SELECT(DISTINCT(Customer.class)).FROM(Customer.class).AS("c")}
	 * &rarr; {@code select distinct c from Customer c}. The result still works with
	 * {@link Q#via(jakarta.persistence.EntityManager)}.</p>
	 *
	 * @param entityType the distinct entity class; must not be {@code null}, e.g. {@code Customer.class}
	 * @return the distinct entity projection as an {@link Expr}
	 */
	public static Expr DISTINCT(Class<?> entityType) {
		return DISTINCT(entity(entityType));
	}

	/**
	 * A constructor (DTO) projection: {@code new fully.qualified.Dto(args...)}.
	 *
	 * <p>Example: {@code NEW(CustomerSummary.class, c(Customer::id), COUNT(c("o", Order::id)))}
	 * &rarr; {@code new linqava.CustomerSummary(c.id, count(o.id))}.</p>
	 *
	 * @param dto  the DTO class whose constructor is invoked; must not be {@code null}. The
	 *             fully-qualified name ({@link Class#getName()}) is emitted.
	 * @param args the constructor arguments, in order; must not be {@code null}
	 * @return the constructor projection as an {@link Expr}
	 */
	public static Expr NEW(Class<?> dto, Object... args) {
		return Expr.of(ctx -> "new " + dto.getName() + "(" + Expr.list(ctx, args) + ")");
	}

	/**
	 * A {@code treat(expr as Subtype)} down-cast for polymorphic associations; follow with
	 * {@link Expr#dot(Col)} to access a subtype field.
	 *
	 * <p>Example: {@code TREAT(expr, CreditCardPayment.class).dot(CreditCardPayment::cardType)}
	 * &rarr; {@code treat(... as CreditCardPayment).cardType}.</p>
	 *
	 * @param expr the expression to cast; must not be {@code null}
	 * @param type the target subtype; must not be {@code null}. Its simple name is emitted.
	 * @return the cast as an {@link Expr}
	 */
	public static Expr TREAT(Object expr, Class<?> type) {
		Expr e = Expr.val(expr);
		return Expr.of(ctx -> "treat(" + e.render(ctx) + " as " + type.getSimpleName() + ")");
	}

	/**
	 * A {@code treat(rootEntity as Subtype)} down-cast — shorthand for {@code TREAT(entity(rootType), subtype)};
	 * follow with {@link Expr#dot(Col)} to access a subtype field.
	 *
	 * <p>Example: {@code TREAT(Payment.class, CreditCardPayment.class).dot(CreditCardPayment::cardType)}
	 * &rarr; {@code treat(p as CreditCardPayment).cardType}.</p>
	 *
	 * @param rootType the entity being cast (its alias is emitted); must not be {@code null}, e.g. {@code Payment.class}
	 * @param subtype  the target subtype; must not be {@code null}. Its simple name is emitted.
	 * @return the cast as an {@link Expr}
	 */
	public static Expr TREAT(Class<?> rootType, Class<?> subtype) {
		return TREAT(entity(rootType), subtype);
	}

	// ===== window helper =====

	/**
	 * The {@code partition by ...} clause of a window; chain {@link Expr#ORDER‿BY(Col)} and
	 * {@link Expr#DESC()} for ordering.
	 *
	 * <p>Example: {@code PARTITION‿BY(c(Order::customerId)).ORDER‿BY(Order::total).DESC()}
	 * &rarr; {@code partition by o.customerId order by o.total desc}.</p>
	 *
	 * @param cols the partitioning columns, in order; must not be {@code null}
	 * @return the window specification as an {@link Expr}, to be passed to {@link Expr#OVER(Expr)}
	 */
	public static Expr PARTITION‿BY(Object... cols) {
		return Expr.of(ctx -> "partition by " + Expr.list(ctx, cols));
	}

	// ===== boolean predicate builders (flat, lambda-free composition) =====

	/**
	 * Equality predicate ({@code =}) with a type-safe left column.
	 *
	 * <p>Example: {@code 二(Order::status, "PAID")} &rarr; {@code o.status = 'PAID'}.</p>
	 *
	 * @param l   the left column getter (method reference); must not be {@code null}
	 * @param r   the right operand: an {@link Expr}, a {@link #param(String)}, a sub-query {@link Q}
	 *            or a literal. To test for null use {@link Cond#IS‿NULL(Col)} — passing {@code null}
	 *            here renders the literal text {@code null}.
	 * @param <T> the entity type owning the left column
	 * @return a leaf predicate; combine with {@link #AND(Cond...)} / {@link #OR(Cond...)}
	 */
	public static <T> Cond 二(Col<T> l, Object r) { return new Cond().二(l, r); }   // =

	/**
	 * Equality predicate ({@code =}) with an expression left operand.
	 *
	 * @param l the left operand (e.g. an aggregate {@link Expr}); must not be {@code null}
	 * @param r the right operand (see {@link #二(Col, Object)}); {@code null} renders as literal {@code null}
	 * @return a leaf predicate
	 */
	public static Cond 二(Object l, Object r) { return new Cond().二(l, r); }

	/**
	 * Less-than predicate ({@code <}) with a type-safe left column.
	 *
	 * <p>Example: {@code ᐸ(Order::discount, 5)} &rarr; {@code o.discount < 5}.</p>
	 *
	 * @param l   the left column getter; must not be {@code null}
	 * @param r   the right operand; must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @return a leaf predicate
	 */
	public static <T> Cond ᐸ(Col<T> l, Object r) { return new Cond().ᐸ(l, r); }    // <

	/**
	 * Less-than predicate ({@code <}) with an expression left operand.
	 *
	 * @param l the left operand; must not be {@code null}
	 * @param r the right operand; must not be {@code null}
	 * @return a leaf predicate
	 */
	public static Cond ᐸ(Object l, Object r) { return new Cond().ᐸ(l, r); }

	/**
	 * Greater-than predicate ({@code >}) with a type-safe left column.
	 *
	 * <p>Example: {@code ᐳ(Order::total, 100)} &rarr; {@code o.total > 100}.</p>
	 *
	 * @param l   the left column getter; must not be {@code null}
	 * @param r   the right operand; must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @return a leaf predicate
	 */
	public static <T> Cond ᐳ(Col<T> l, Object r) { return new Cond().ᐳ(l, r); }    // >

	/**
	 * Greater-than predicate ({@code >}) with an expression left operand.
	 *
	 * @param l the left operand; must not be {@code null}
	 * @param r the right operand; must not be {@code null}
	 * @return a leaf predicate
	 */
	public static Cond ᐳ(Object l, Object r) { return new Cond().ᐳ(l, r); }

	/**
	 * Less-than-or-equal predicate ({@code <=}) with a type-safe left column.
	 *
	 * <p>Example: {@code ᐸ二(Order::discount, 50)} &rarr; {@code o.discount <= 50}.</p>
	 *
	 * @param l   the left column getter; must not be {@code null}
	 * @param r   the right operand; must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @return a leaf predicate
	 */
	public static <T> Cond ᐸ二(Col<T> l, Object r) { return new Cond().ᐸ二(l, r); } // <=

	/**
	 * Less-than-or-equal predicate ({@code <=}) with an expression left operand.
	 *
	 * @param l the left operand; must not be {@code null}
	 * @param r the right operand; must not be {@code null}
	 * @return a leaf predicate
	 */
	public static Cond ᐸ二(Object l, Object r) { return new Cond().ᐸ二(l, r); }

	/**
	 * Greater-than-or-equal predicate ({@code >=}) with a type-safe left column.
	 *
	 * <p>Example: {@code ᐳ二(Order::total, 1000)} &rarr; {@code o.total >= 1000}.</p>
	 *
	 * @param l   the left column getter; must not be {@code null}
	 * @param r   the right operand; must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @return a leaf predicate
	 */
	public static <T> Cond ᐳ二(Col<T> l, Object r) { return new Cond().ᐳ二(l, r); } // >=

	/**
	 * Greater-than-or-equal predicate ({@code >=}) with an expression left operand.
	 *
	 * @param l the left operand; must not be {@code null}
	 * @param r the right operand; must not be {@code null}
	 * @return a leaf predicate
	 */
	public static Cond ᐳ二(Object l, Object r) { return new Cond().ᐳ二(l, r); }

	/**
	 * Not-equal predicate ({@code <>}) with a type-safe left column.
	 *
	 * <p>Example: {@code ᐸᐳ(Order::status, "CANCELLED")} &rarr; {@code o.status <> 'CANCELLED'}.</p>
	 *
	 * @param l   the left column getter; must not be {@code null}
	 * @param r   the right operand; must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @return a leaf predicate
	 */
	public static <T> Cond ᐸᐳ(Col<T> l, Object r) { return new Cond().ᐸᐳ(l, r); } // <>

	/**
	 * Not-equal predicate ({@code <>}) with an expression left operand.
	 *
	 * @param l the left operand; must not be {@code null}
	 * @param r the right operand; must not be {@code null}
	 * @return a leaf predicate
	 */
	public static Cond ᐸᐳ(Object l, Object r) { return new Cond().ᐸᐳ(l, r); }

	/**
	 * Conjunction of predicates ({@code and}), parenthesized as a group.
	 *
	 * <p>Example: {@code AND(二(Order::status, "PAID"), ᐳ(Order::total, 100))}
	 * &rarr; {@code (o.status = 'PAID' and o.total > 100)}.</p>
	 *
	 * @param parts the predicates to combine, in order; must not be {@code null} and must contain at
	 *              least one non-{@code null} element
	 * @return a combined predicate
	 */
	public static Cond AND(Cond... parts) { return Cond.combine("and", parts); }

	/**
	 * Disjunction of predicates ({@code or}), parenthesized as a group.
	 *
	 * <p>Example: {@code OR(ᐳ(Order::total, 100), ᐸ(Order::discount, 5))}
	 * &rarr; {@code (o.total > 100 or o.discount < 5)}.</p>
	 *
	 * @param parts the predicates to combine, in order; must not be {@code null} and must contain at
	 *              least one non-{@code null} element
	 * @return a combined predicate
	 */
	public static Cond OR(Cond... parts) { return Cond.combine("or", parts); }

	/**
	 * Negation of a predicate ({@code not (...)}).
	 *
	 * <p>Example: {@code NOT(二(Order::status, "PAID"))} &rarr; {@code not (o.status = 'PAID')}.</p>
	 *
	 * @param predicate the predicate to negate; must not be {@code null}
	 * @return the negated predicate
	 */
	public static Cond NOT(Cond predicate) { return Cond.negate(predicate); }

	// ===== CASE expression =====

	/**
	 * Starts a {@code CASE WHEN ... THEN ... [ELSE ...] END} expression.
	 *
	 * <p>Example: {@code CASE().WHEN($ -> $.ᐳ二(Order::total, 1000)).THEN("GOLD").ELSE("BRONZE").END()}.</p>
	 *
	 * @return a new {@link Case} builder
	 */
	public static Case CASE() { return new Case(); }

	private static Expr fn(String name, Object arg) {
		Expr a = Expr.val(arg);
		return Expr.of(ctx -> name + "(" + a.render(ctx) + ")");
	}
}
