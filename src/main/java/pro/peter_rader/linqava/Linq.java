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
 * Q<?> q = SELECT(typedCol(User::id))
 *           .FROM(User.class)
 *           .WHERE(User::Name).ᆖ("John");
 * q.getUnsafeHql(); // "select id from User where Name = 'John'"
 * }</pre>
 *
 * <p>
 * General null policy: unless a method explicitly states otherwise, no argument
 * may be {@code null}; passing {@code null} throws {@link NullPointerException}
 * either immediately or when {@link Q#getUnsafeHql()} is called. The documented
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
	 * Example: {@code SELECT(typedCol(Order::id), COUNT(typedCol(Order::id)))} &rarr;
	 * {@code select o.id, count(o.id) ...}
	 * </p>
	 *
	 * @param cols the projected columns/expressions, in order; typically
	 *             {@link #typedCol(TypedCol)}, aggregates such as {@link #COUNT(Object)}, or
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
	 * {@code from <Entity>}. Deliberately restricted compared to
	 * {@link #SELECTㅤ(Class) SELECT(clazz)}.{@link SelectStep#ㅤFROMㅤ(Class) FROM(clazz)}: the returned
	 * {@link EntityQ} has no {@code AS(String)} and no {@code JOIN}, because a single, unaliased
	 * source is all this shape ever needs or supports.
	 *
	 * <p>Example: {@code SELECTㅤꁘㅤFROM(Order.class).WHERE(Order::status).ᆖ("PAID")} &rarr;
	 * {@code from Order where status = 'PAID'}.</p>
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
	 * column in {@link #typedCol(TypedCol)}.
	 *
	 * @param first the first column getter (method reference); must not be
	 *              {@code null}
	 * @param rest  the remaining columns/expressions, in order; must not be
	 *              {@code null}, may be empty
	 * @param <A>   the entity type owning the first column
	 * @return the {@code SELECT} phase, which requires a
	 *         {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <A> SelectStep<Object> SELECTㅤ(TypedCol<A, ?> first, Object... rest) {
		return new SelectStep<>(new Q<Object>().addSelect(first, rest));
	}

	/**
	 * Starts a {@code SELECT} of exactly two bare getter references from (possibly) different
	 * entities, e.g. {@code SELECT(Order::id, Customer::name)} — like {@link #SELECTㅤ(TypedCol, Object...)}
	 * but for a second column that is also a bare getter (which {@code Object...} cannot target
	 * directly, since a method reference can only bind to a functional-interface-typed parameter).
	 *
	 * @param first  the first column getter (method reference); must not be {@code null}
	 * @param second the second column getter (method reference); must not be {@code null}
	 * @param <A>    the entity type owning the first column
	 * @param <B>    the entity type owning the second column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <A, B> SelectStep<Object> SELECTㅤ(TypedCol<A, ?> first, TypedCol<B, ?> second) {
		return new SelectStep<>(new Q<Object>().addSelect(Expr.typedCol(first), Expr.typedCol(second)));
	}

	/**
	 * Starts a {@code SELECT} of exactly three bare getter references — see
	 * {@link #SELECTㅤ(TypedCol, TypedCol)}.
	 *
	 * @param first  the first column getter (method reference); must not be {@code null}
	 * @param second the second column getter (method reference); must not be {@code null}
	 * @param third  the third column getter (method reference); must not be {@code null}
	 * @param <A>    the entity type owning the first column
	 * @param <B>    the entity type owning the second column
	 * @param <C>    the entity type owning the third column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <A, B, C> SelectStep<Object> SELECTㅤ(TypedCol<A, ?> first, TypedCol<B, ?> second,
			TypedCol<C, ?> third) {
		return new SelectStep<>(
				new Q<Object>().addSelect(Expr.typedCol(first), Expr.typedCol(second), Expr.typedCol(third)));
	}

	/**
	 * Starts a {@code SELECT} of three leading bare getter references followed by arbitrary further
	 * columns/expressions — see {@link #SELECTㅤ(TypedCol, TypedCol, TypedCol)}.
	 *
	 * @param first  the first column getter (method reference); must not be {@code null}
	 * @param second the second column getter (method reference); must not be {@code null}
	 * @param third  the third column getter (method reference); must not be {@code null}
	 * @param rest   the remaining columns/expressions, in order; must not be {@code null}, may be
	 *               empty
	 * @param <A>    the entity type owning the first column
	 * @param <B>    the entity type owning the second column
	 * @param <C>    the entity type owning the third column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <A, B, C> SelectStep<Object> SELECTㅤ(TypedCol<A, ?> first, TypedCol<B, ?> second,
			TypedCol<C, ?> third, Object... rest) {
		Object[] args = new Object[rest.length + 3];
		args[0] = Expr.typedCol(first);
		args[1] = Expr.typedCol(second);
		args[2] = Expr.typedCol(third);
		System.arraycopy(rest, 0, args, 3, rest.length);
		return new SelectStep<>(new Q<Object>().addSelect(args));
	}

	/**
	 * Starts a {@code SELECT} projecting two columns each qualified by an explicit alias — for
	 * referencing columns of two aliases of the <em>same</em> entity in a self-join, e.g.
	 * {@code SELECT("a", X::id, "b", X::id)} &rarr; {@code select a.id, b.id}.
	 *
	 * @param alias1 the range-variable alias qualifying the first column; must not be {@code null}
	 * @param col1   the first column getter (method reference); must not be {@code null}
	 * @param alias2 the range-variable alias qualifying the second column; must not be {@code null}
	 * @param col2   the second column getter (method reference); must not be {@code null}
	 * @param <A>    the entity type owning the first column
	 * @param <B>    the entity type owning the second column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <A, B> SelectStep<Object> SELECTㅤ(String alias1, TypedCol<A, ?> col1, String alias2,
			TypedCol<B, ?> col2) {
		return new SelectStep<>(new Q<Object>().addSelect(typedCol(alias1, col1), typedCol(alias2, col2)));
	}

	/**
	 * Starts a {@code SELECT} of one column referenced by raw alias and field name — for a
	 * derived/CTE column that has no entity getter, e.g. {@code SELECT("a", "name")} &rarr;
	 * {@code select a.name}.
	 *
	 * @param alias the range-variable alias qualifying the column; must not be {@code null}
	 * @param field the field name; must not be {@code null}
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static SelectStep<Object> SELECTㅤ(String alias, String field) {
		return new SelectStep<>(new Q<Object>().addSelect(typedCol(alias, field)));
	}

	/**
	 * Starts a {@code SELECT} of two columns referenced by raw alias and field name — see
	 * {@link #SELECTㅤ(String, String)}.
	 *
	 * @param alias1 the range-variable alias qualifying the first column; must not be {@code null}
	 * @param field1 the first field name; must not be {@code null}
	 * @param alias2 the range-variable alias qualifying the second column; must not be {@code null}
	 * @param field2 the second field name; must not be {@code null}
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static SelectStep<Object> SELECTㅤ(String alias1, String field1, String alias2, String field2) {
		return new SelectStep<>(new Q<Object>().addSelect(typedCol(alias1, field1), typedCol(alias2, field2)));
	}

	/**
	 * Starts a {@code SELECT} of three columns referenced by raw alias and field name — see
	 * {@link #SELECTㅤ(String, String)}.
	 *
	 * @param alias1 the range-variable alias qualifying the first column; must not be {@code null}
	 * @param field1 the first field name; must not be {@code null}
	 * @param alias2 the range-variable alias qualifying the second column; must not be {@code null}
	 * @param field2 the second field name; must not be {@code null}
	 * @param alias3 the range-variable alias qualifying the third column; must not be {@code null}
	 * @param field3 the third field name; must not be {@code null}
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static SelectStep<Object> SELECTㅤ(String alias1, String field1, String alias2, String field2,
			String alias3, String field3) {
		return new SelectStep<>(new Q<Object>().addSelect(typedCol(alias1, field1), typedCol(alias2, field2),
				typedCol(alias3, field3)));
	}

	/**
	 * Starts a {@code SELECT} whose first column is a bare getter reference and whose second is a
	 * raw alias-qualified field with no getter, e.g. {@code SELECT(Customer::name, "b", "total")}
	 * &rarr; {@code select name, b.total}.
	 *
	 * @param first the first column getter (method reference); must not be {@code null}
	 * @param alias the range-variable alias qualifying the second column; must not be {@code null}
	 * @param field the second column's field name; must not be {@code null}
	 * @param <A>   the entity type owning the first column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <A> SelectStep<Object> SELECTㅤ(TypedCol<A, ?> first, String alias, String field) {
		return new SelectStep<>(new Q<Object>().addSelect(Expr.typedCol(first), typedCol(alias, field)));
	}

	/**
	 * SELECT of 1 explicitly aliased bare column — avoids any {@code typedCol(...)}-style
	 * wrapping.
	 *
	 * <p>Example: {@code SELECT(User::id, "id", User::name, "name")} &rarr;
	 * {@code select id as id, name as name}.</p>
	 *
	 * @param first the first column getter (method reference); must not be {@code null}
	 * @param firstAlias the first column's alias; must not be {@code null}
	 * @param <T1> the entity type owning the first column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <T1> SelectStep<Object> SELECTㅤ(TypedCol<T1, ?> first, String firstAlias) {
		return new SelectStep<>(new Q<Object>().addSelect(
				Expr.typedCol(first).ㅤAS(firstAlias)));
	}

	/**
	 * SELECT of 1 explicitly aliased bare column followed by arbitrary further
	 * columns/expressions — for mixing an aliased getter with e.g. an aggregate or
	 * window function that carries its own {@code .AS(...)}.
	 *
	 * @param first the first column getter (method reference); must not be {@code null}
	 * @param firstAlias the first column's alias; must not be {@code null}
	 * @param rest the remaining columns/expressions, in order; must not be {@code null}, may be empty
	 * @param <T1> the entity type owning the first column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <T1> SelectStep<Object> SELECTㅤ(
			TypedCol<T1, ?> first, String firstAlias,
			Object... rest) {
		Object[] args = new Object[rest.length + 1];
		args[0] = Expr.typedCol(first).ㅤAS(firstAlias);
		System.arraycopy(rest, 0, args, 1, rest.length);
		return new SelectStep<>(new Q<Object>().addSelect(args));
	}

	/**
	 * SELECT of 2 explicitly aliased bare columns — avoids any {@code typedCol(...)}-style
	 * wrapping.
	 *
	 * <p>Example: {@code SELECT(User::id, "id", User::name, "name")} &rarr;
	 * {@code select id as id, name as name}.</p>
	 *
	 * @param first the first column getter (method reference); must not be {@code null}
	 * @param firstAlias the first column's alias; must not be {@code null}
	 * @param second the second column getter (method reference); must not be {@code null}
	 * @param secondAlias the second column's alias; must not be {@code null}
	 * @param <T1> the entity type owning the first column
	 * @param <T2> the entity type owning the second column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <T1, T2> SelectStep<Object> SELECTㅤ(
			TypedCol<T1, ?> first, String firstAlias,
			TypedCol<T2, ?> second, String secondAlias) {
		return new SelectStep<>(new Q<Object>().addSelect(
				Expr.typedCol(first).ㅤAS(firstAlias),
				Expr.typedCol(second).ㅤAS(secondAlias)));
	}

	/**
	 * SELECT of 2 explicitly aliased bare columns followed by arbitrary further
	 * columns/expressions — for mixing an aliased getter with e.g. an aggregate or
	 * window function that carries its own {@code .AS(...)}.
	 *
	 * @param first the first column getter (method reference); must not be {@code null}
	 * @param firstAlias the first column's alias; must not be {@code null}
	 * @param second the second column getter (method reference); must not be {@code null}
	 * @param secondAlias the second column's alias; must not be {@code null}
	 * @param rest the remaining columns/expressions, in order; must not be {@code null}, may be empty
	 * @param <T1> the entity type owning the first column
	 * @param <T2> the entity type owning the second column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <T1, T2> SelectStep<Object> SELECTㅤ(
			TypedCol<T1, ?> first, String firstAlias,
			TypedCol<T2, ?> second, String secondAlias,
			Object... rest) {
		Object[] args = new Object[rest.length + 2];
		args[0] = Expr.typedCol(first).ㅤAS(firstAlias);
		args[1] = Expr.typedCol(second).ㅤAS(secondAlias);
		System.arraycopy(rest, 0, args, 2, rest.length);
		return new SelectStep<>(new Q<Object>().addSelect(args));
	}

	/**
	 * SELECT of 3 explicitly aliased bare columns — avoids any {@code typedCol(...)}-style
	 * wrapping.
	 *
	 * <p>Example: {@code SELECT(User::id, "id", User::name, "name")} &rarr;
	 * {@code select id as id, name as name}.</p>
	 *
	 * @param first the first column getter (method reference); must not be {@code null}
	 * @param firstAlias the first column's alias; must not be {@code null}
	 * @param second the second column getter (method reference); must not be {@code null}
	 * @param secondAlias the second column's alias; must not be {@code null}
	 * @param third the third column getter (method reference); must not be {@code null}
	 * @param thirdAlias the third column's alias; must not be {@code null}
	 * @param <T1> the entity type owning the first column
	 * @param <T2> the entity type owning the second column
	 * @param <T3> the entity type owning the third column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <T1, T2, T3> SelectStep<Object> SELECTㅤ(
			TypedCol<T1, ?> first, String firstAlias,
			TypedCol<T2, ?> second, String secondAlias,
			TypedCol<T3, ?> third, String thirdAlias) {
		return new SelectStep<>(new Q<Object>().addSelect(
				Expr.typedCol(first).ㅤAS(firstAlias),
				Expr.typedCol(second).ㅤAS(secondAlias),
				Expr.typedCol(third).ㅤAS(thirdAlias)));
	}

	/**
	 * SELECT of 3 explicitly aliased bare columns followed by arbitrary further
	 * columns/expressions — for mixing an aliased getter with e.g. an aggregate or
	 * window function that carries its own {@code .AS(...)}.
	 *
	 * @param first the first column getter (method reference); must not be {@code null}
	 * @param firstAlias the first column's alias; must not be {@code null}
	 * @param second the second column getter (method reference); must not be {@code null}
	 * @param secondAlias the second column's alias; must not be {@code null}
	 * @param third the third column getter (method reference); must not be {@code null}
	 * @param thirdAlias the third column's alias; must not be {@code null}
	 * @param rest the remaining columns/expressions, in order; must not be {@code null}, may be empty
	 * @param <T1> the entity type owning the first column
	 * @param <T2> the entity type owning the second column
	 * @param <T3> the entity type owning the third column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <T1, T2, T3> SelectStep<Object> SELECTㅤ(
			TypedCol<T1, ?> first, String firstAlias,
			TypedCol<T2, ?> second, String secondAlias,
			TypedCol<T3, ?> third, String thirdAlias,
			Object... rest) {
		Object[] args = new Object[rest.length + 3];
		args[0] = Expr.typedCol(first).ㅤAS(firstAlias);
		args[1] = Expr.typedCol(second).ㅤAS(secondAlias);
		args[2] = Expr.typedCol(third).ㅤAS(thirdAlias);
		System.arraycopy(rest, 0, args, 3, rest.length);
		return new SelectStep<>(new Q<Object>().addSelect(args));
	}

	/**
	 * SELECT of 4 explicitly aliased bare columns — avoids any {@code typedCol(...)}-style
	 * wrapping.
	 *
	 * <p>Example: {@code SELECT(User::id, "id", User::name, "name")} &rarr;
	 * {@code select id as id, name as name}.</p>
	 *
	 * @param first the first column getter (method reference); must not be {@code null}
	 * @param firstAlias the first column's alias; must not be {@code null}
	 * @param second the second column getter (method reference); must not be {@code null}
	 * @param secondAlias the second column's alias; must not be {@code null}
	 * @param third the third column getter (method reference); must not be {@code null}
	 * @param thirdAlias the third column's alias; must not be {@code null}
	 * @param fourth the fourth column getter (method reference); must not be {@code null}
	 * @param fourthAlias the fourth column's alias; must not be {@code null}
	 * @param <T1> the entity type owning the first column
	 * @param <T2> the entity type owning the second column
	 * @param <T3> the entity type owning the third column
	 * @param <T4> the entity type owning the fourth column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <T1, T2, T3, T4> SelectStep<Object> SELECTㅤ(
			TypedCol<T1, ?> first, String firstAlias,
			TypedCol<T2, ?> second, String secondAlias,
			TypedCol<T3, ?> third, String thirdAlias,
			TypedCol<T4, ?> fourth, String fourthAlias) {
		return new SelectStep<>(new Q<Object>().addSelect(
				Expr.typedCol(first).ㅤAS(firstAlias),
				Expr.typedCol(second).ㅤAS(secondAlias),
				Expr.typedCol(third).ㅤAS(thirdAlias),
				Expr.typedCol(fourth).ㅤAS(fourthAlias)));
	}

	/**
	 * SELECT of 5 explicitly aliased bare columns — avoids any {@code typedCol(...)}-style
	 * wrapping.
	 *
	 * <p>Example: {@code SELECT(User::id, "id", User::name, "name")} &rarr;
	 * {@code select id as id, name as name}.</p>
	 *
	 * @param first the first column getter (method reference); must not be {@code null}
	 * @param firstAlias the first column's alias; must not be {@code null}
	 * @param second the second column getter (method reference); must not be {@code null}
	 * @param secondAlias the second column's alias; must not be {@code null}
	 * @param third the third column getter (method reference); must not be {@code null}
	 * @param thirdAlias the third column's alias; must not be {@code null}
	 * @param fourth the fourth column getter (method reference); must not be {@code null}
	 * @param fourthAlias the fourth column's alias; must not be {@code null}
	 * @param fifth the fifth column getter (method reference); must not be {@code null}
	 * @param fifthAlias the fifth column's alias; must not be {@code null}
	 * @param <T1> the entity type owning the first column
	 * @param <T2> the entity type owning the second column
	 * @param <T3> the entity type owning the third column
	 * @param <T4> the entity type owning the fourth column
	 * @param <T5> the entity type owning the fifth column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <T1, T2, T3, T4, T5> SelectStep<Object> SELECTㅤ(
			TypedCol<T1, ?> first, String firstAlias,
			TypedCol<T2, ?> second, String secondAlias,
			TypedCol<T3, ?> third, String thirdAlias,
			TypedCol<T4, ?> fourth, String fourthAlias,
			TypedCol<T5, ?> fifth, String fifthAlias) {
		return new SelectStep<>(new Q<Object>().addSelect(
				Expr.typedCol(first).ㅤAS(firstAlias),
				Expr.typedCol(second).ㅤAS(secondAlias),
				Expr.typedCol(third).ㅤAS(thirdAlias),
				Expr.typedCol(fourth).ㅤAS(fourthAlias),
				Expr.typedCol(fifth).ㅤAS(fifthAlias)));
	}

	/**
	 * SELECT of 6 explicitly aliased bare columns — avoids any {@code typedCol(...)}-style
	 * wrapping.
	 *
	 * <p>Example: {@code SELECT(User::id, "id", User::name, "name")} &rarr;
	 * {@code select id as id, name as name}.</p>
	 *
	 * @param first the first column getter (method reference); must not be {@code null}
	 * @param firstAlias the first column's alias; must not be {@code null}
	 * @param second the second column getter (method reference); must not be {@code null}
	 * @param secondAlias the second column's alias; must not be {@code null}
	 * @param third the third column getter (method reference); must not be {@code null}
	 * @param thirdAlias the third column's alias; must not be {@code null}
	 * @param fourth the fourth column getter (method reference); must not be {@code null}
	 * @param fourthAlias the fourth column's alias; must not be {@code null}
	 * @param fifth the fifth column getter (method reference); must not be {@code null}
	 * @param fifthAlias the fifth column's alias; must not be {@code null}
	 * @param sixth the sixth column getter (method reference); must not be {@code null}
	 * @param sixthAlias the sixth column's alias; must not be {@code null}
	 * @param <T1> the entity type owning the first column
	 * @param <T2> the entity type owning the second column
	 * @param <T3> the entity type owning the third column
	 * @param <T4> the entity type owning the fourth column
	 * @param <T5> the entity type owning the fifth column
	 * @param <T6> the entity type owning the sixth column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <T1, T2, T3, T4, T5, T6> SelectStep<Object> SELECTㅤ(
			TypedCol<T1, ?> first, String firstAlias,
			TypedCol<T2, ?> second, String secondAlias,
			TypedCol<T3, ?> third, String thirdAlias,
			TypedCol<T4, ?> fourth, String fourthAlias,
			TypedCol<T5, ?> fifth, String fifthAlias,
			TypedCol<T6, ?> sixth, String sixthAlias) {
		return new SelectStep<>(new Q<Object>().addSelect(
				Expr.typedCol(first).ㅤAS(firstAlias),
				Expr.typedCol(second).ㅤAS(secondAlias),
				Expr.typedCol(third).ㅤAS(thirdAlias),
				Expr.typedCol(fourth).ㅤAS(fourthAlias),
				Expr.typedCol(fifth).ㅤAS(fifthAlias),
				Expr.typedCol(sixth).ㅤAS(sixthAlias)));
	}

	/**
	 * SELECT of 7 explicitly aliased bare columns — avoids any {@code typedCol(...)}-style
	 * wrapping.
	 *
	 * <p>Example: {@code SELECT(User::id, "id", User::name, "name")} &rarr;
	 * {@code select id as id, name as name}.</p>
	 *
	 * @param first the first column getter (method reference); must not be {@code null}
	 * @param firstAlias the first column's alias; must not be {@code null}
	 * @param second the second column getter (method reference); must not be {@code null}
	 * @param secondAlias the second column's alias; must not be {@code null}
	 * @param third the third column getter (method reference); must not be {@code null}
	 * @param thirdAlias the third column's alias; must not be {@code null}
	 * @param fourth the fourth column getter (method reference); must not be {@code null}
	 * @param fourthAlias the fourth column's alias; must not be {@code null}
	 * @param fifth the fifth column getter (method reference); must not be {@code null}
	 * @param fifthAlias the fifth column's alias; must not be {@code null}
	 * @param sixth the sixth column getter (method reference); must not be {@code null}
	 * @param sixthAlias the sixth column's alias; must not be {@code null}
	 * @param seventh the seventh column getter (method reference); must not be {@code null}
	 * @param seventhAlias the seventh column's alias; must not be {@code null}
	 * @param <T1> the entity type owning the first column
	 * @param <T2> the entity type owning the second column
	 * @param <T3> the entity type owning the third column
	 * @param <T4> the entity type owning the fourth column
	 * @param <T5> the entity type owning the fifth column
	 * @param <T6> the entity type owning the sixth column
	 * @param <T7> the entity type owning the seventh column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <T1, T2, T3, T4, T5, T6, T7> SelectStep<Object> SELECTㅤ(
			TypedCol<T1, ?> first, String firstAlias,
			TypedCol<T2, ?> second, String secondAlias,
			TypedCol<T3, ?> third, String thirdAlias,
			TypedCol<T4, ?> fourth, String fourthAlias,
			TypedCol<T5, ?> fifth, String fifthAlias,
			TypedCol<T6, ?> sixth, String sixthAlias,
			TypedCol<T7, ?> seventh, String seventhAlias) {
		return new SelectStep<>(new Q<Object>().addSelect(
				Expr.typedCol(first).ㅤAS(firstAlias),
				Expr.typedCol(second).ㅤAS(secondAlias),
				Expr.typedCol(third).ㅤAS(thirdAlias),
				Expr.typedCol(fourth).ㅤAS(fourthAlias),
				Expr.typedCol(fifth).ㅤAS(fifthAlias),
				Expr.typedCol(sixth).ㅤAS(sixthAlias),
				Expr.typedCol(seventh).ㅤAS(seventhAlias)));
	}

	/**
	 * SELECT of 8 explicitly aliased bare columns — avoids any {@code typedCol(...)}-style
	 * wrapping.
	 *
	 * <p>Example: {@code SELECT(User::id, "id", User::name, "name")} &rarr;
	 * {@code select id as id, name as name}.</p>
	 *
	 * @param first the first column getter (method reference); must not be {@code null}
	 * @param firstAlias the first column's alias; must not be {@code null}
	 * @param second the second column getter (method reference); must not be {@code null}
	 * @param secondAlias the second column's alias; must not be {@code null}
	 * @param third the third column getter (method reference); must not be {@code null}
	 * @param thirdAlias the third column's alias; must not be {@code null}
	 * @param fourth the fourth column getter (method reference); must not be {@code null}
	 * @param fourthAlias the fourth column's alias; must not be {@code null}
	 * @param fifth the fifth column getter (method reference); must not be {@code null}
	 * @param fifthAlias the fifth column's alias; must not be {@code null}
	 * @param sixth the sixth column getter (method reference); must not be {@code null}
	 * @param sixthAlias the sixth column's alias; must not be {@code null}
	 * @param seventh the seventh column getter (method reference); must not be {@code null}
	 * @param seventhAlias the seventh column's alias; must not be {@code null}
	 * @param eighth the eighth column getter (method reference); must not be {@code null}
	 * @param eighthAlias the eighth column's alias; must not be {@code null}
	 * @param <T1> the entity type owning the first column
	 * @param <T2> the entity type owning the second column
	 * @param <T3> the entity type owning the third column
	 * @param <T4> the entity type owning the fourth column
	 * @param <T5> the entity type owning the fifth column
	 * @param <T6> the entity type owning the sixth column
	 * @param <T7> the entity type owning the seventh column
	 * @param <T8> the entity type owning the eighth column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <T1, T2, T3, T4, T5, T6, T7, T8> SelectStep<Object> SELECTㅤ(
			TypedCol<T1, ?> first, String firstAlias,
			TypedCol<T2, ?> second, String secondAlias,
			TypedCol<T3, ?> third, String thirdAlias,
			TypedCol<T4, ?> fourth, String fourthAlias,
			TypedCol<T5, ?> fifth, String fifthAlias,
			TypedCol<T6, ?> sixth, String sixthAlias,
			TypedCol<T7, ?> seventh, String seventhAlias,
			TypedCol<T8, ?> eighth, String eighthAlias) {
		return new SelectStep<>(new Q<Object>().addSelect(
				Expr.typedCol(first).ㅤAS(firstAlias),
				Expr.typedCol(second).ㅤAS(secondAlias),
				Expr.typedCol(third).ㅤAS(thirdAlias),
				Expr.typedCol(fourth).ㅤAS(fourthAlias),
				Expr.typedCol(fifth).ㅤAS(fifthAlias),
				Expr.typedCol(sixth).ㅤAS(sixthAlias),
				Expr.typedCol(seventh).ㅤAS(seventhAlias),
				Expr.typedCol(eighth).ㅤAS(eighthAlias)));
	}

	/**
	 * SELECT of 9 explicitly aliased bare columns — avoids any {@code typedCol(...)}-style
	 * wrapping.
	 *
	 * <p>Example: {@code SELECT(User::id, "id", User::name, "name")} &rarr;
	 * {@code select id as id, name as name}.</p>
	 *
	 * @param first the first column getter (method reference); must not be {@code null}
	 * @param firstAlias the first column's alias; must not be {@code null}
	 * @param second the second column getter (method reference); must not be {@code null}
	 * @param secondAlias the second column's alias; must not be {@code null}
	 * @param third the third column getter (method reference); must not be {@code null}
	 * @param thirdAlias the third column's alias; must not be {@code null}
	 * @param fourth the fourth column getter (method reference); must not be {@code null}
	 * @param fourthAlias the fourth column's alias; must not be {@code null}
	 * @param fifth the fifth column getter (method reference); must not be {@code null}
	 * @param fifthAlias the fifth column's alias; must not be {@code null}
	 * @param sixth the sixth column getter (method reference); must not be {@code null}
	 * @param sixthAlias the sixth column's alias; must not be {@code null}
	 * @param seventh the seventh column getter (method reference); must not be {@code null}
	 * @param seventhAlias the seventh column's alias; must not be {@code null}
	 * @param eighth the eighth column getter (method reference); must not be {@code null}
	 * @param eighthAlias the eighth column's alias; must not be {@code null}
	 * @param ninth the ninth column getter (method reference); must not be {@code null}
	 * @param ninthAlias the ninth column's alias; must not be {@code null}
	 * @param <T1> the entity type owning the first column
	 * @param <T2> the entity type owning the second column
	 * @param <T3> the entity type owning the third column
	 * @param <T4> the entity type owning the fourth column
	 * @param <T5> the entity type owning the fifth column
	 * @param <T6> the entity type owning the sixth column
	 * @param <T7> the entity type owning the seventh column
	 * @param <T8> the entity type owning the eighth column
	 * @param <T9> the entity type owning the ninth column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <T1, T2, T3, T4, T5, T6, T7, T8, T9> SelectStep<Object> SELECTㅤ(
			TypedCol<T1, ?> first, String firstAlias,
			TypedCol<T2, ?> second, String secondAlias,
			TypedCol<T3, ?> third, String thirdAlias,
			TypedCol<T4, ?> fourth, String fourthAlias,
			TypedCol<T5, ?> fifth, String fifthAlias,
			TypedCol<T6, ?> sixth, String sixthAlias,
			TypedCol<T7, ?> seventh, String seventhAlias,
			TypedCol<T8, ?> eighth, String eighthAlias,
			TypedCol<T9, ?> ninth, String ninthAlias) {
		return new SelectStep<>(new Q<Object>().addSelect(
				Expr.typedCol(first).ㅤAS(firstAlias),
				Expr.typedCol(second).ㅤAS(secondAlias),
				Expr.typedCol(third).ㅤAS(thirdAlias),
				Expr.typedCol(fourth).ㅤAS(fourthAlias),
				Expr.typedCol(fifth).ㅤAS(fifthAlias),
				Expr.typedCol(sixth).ㅤAS(sixthAlias),
				Expr.typedCol(seventh).ㅤAS(seventhAlias),
				Expr.typedCol(eighth).ㅤAS(eighthAlias),
				Expr.typedCol(ninth).ㅤAS(ninthAlias)));
	}

	/**
	 * SELECT of 10 explicitly aliased bare columns — avoids any {@code typedCol(...)}-style
	 * wrapping.
	 *
	 * <p>Example: {@code SELECT(User::id, "id", User::name, "name")} &rarr;
	 * {@code select id as id, name as name}.</p>
	 *
	 * @param first the first column getter (method reference); must not be {@code null}
	 * @param firstAlias the first column's alias; must not be {@code null}
	 * @param second the second column getter (method reference); must not be {@code null}
	 * @param secondAlias the second column's alias; must not be {@code null}
	 * @param third the third column getter (method reference); must not be {@code null}
	 * @param thirdAlias the third column's alias; must not be {@code null}
	 * @param fourth the fourth column getter (method reference); must not be {@code null}
	 * @param fourthAlias the fourth column's alias; must not be {@code null}
	 * @param fifth the fifth column getter (method reference); must not be {@code null}
	 * @param fifthAlias the fifth column's alias; must not be {@code null}
	 * @param sixth the sixth column getter (method reference); must not be {@code null}
	 * @param sixthAlias the sixth column's alias; must not be {@code null}
	 * @param seventh the seventh column getter (method reference); must not be {@code null}
	 * @param seventhAlias the seventh column's alias; must not be {@code null}
	 * @param eighth the eighth column getter (method reference); must not be {@code null}
	 * @param eighthAlias the eighth column's alias; must not be {@code null}
	 * @param ninth the ninth column getter (method reference); must not be {@code null}
	 * @param ninthAlias the ninth column's alias; must not be {@code null}
	 * @param tenth the tenth column getter (method reference); must not be {@code null}
	 * @param tenthAlias the tenth column's alias; must not be {@code null}
	 * @param <T1> the entity type owning the first column
	 * @param <T2> the entity type owning the second column
	 * @param <T3> the entity type owning the third column
	 * @param <T4> the entity type owning the fourth column
	 * @param <T5> the entity type owning the fifth column
	 * @param <T6> the entity type owning the sixth column
	 * @param <T7> the entity type owning the seventh column
	 * @param <T8> the entity type owning the eighth column
	 * @param <T9> the entity type owning the ninth column
	 * @param <T10> the entity type owning the tenth column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> SelectStep<Object> SELECTㅤ(
			TypedCol<T1, ?> first, String firstAlias,
			TypedCol<T2, ?> second, String secondAlias,
			TypedCol<T3, ?> third, String thirdAlias,
			TypedCol<T4, ?> fourth, String fourthAlias,
			TypedCol<T5, ?> fifth, String fifthAlias,
			TypedCol<T6, ?> sixth, String sixthAlias,
			TypedCol<T7, ?> seventh, String seventhAlias,
			TypedCol<T8, ?> eighth, String eighthAlias,
			TypedCol<T9, ?> ninth, String ninthAlias,
			TypedCol<T10, ?> tenth, String tenthAlias) {
		return new SelectStep<>(new Q<Object>().addSelect(
				Expr.typedCol(first).ㅤAS(firstAlias),
				Expr.typedCol(second).ㅤAS(secondAlias),
				Expr.typedCol(third).ㅤAS(thirdAlias),
				Expr.typedCol(fourth).ㅤAS(fourthAlias),
				Expr.typedCol(fifth).ㅤAS(fifthAlias),
				Expr.typedCol(sixth).ㅤAS(sixthAlias),
				Expr.typedCol(seventh).ㅤAS(seventhAlias),
				Expr.typedCol(eighth).ㅤAS(eighthAlias),
				Expr.typedCol(ninth).ㅤAS(ninthAlias),
				Expr.typedCol(tenth).ㅤAS(tenthAlias)));
	}

	/**
	 * SELECT of 11 explicitly aliased bare columns — avoids any {@code typedCol(...)}-style
	 * wrapping.
	 *
	 * <p>Example: {@code SELECT(User::id, "id", User::name, "name")} &rarr;
	 * {@code select id as id, name as name}.</p>
	 *
	 * @param first the first column getter (method reference); must not be {@code null}
	 * @param firstAlias the first column's alias; must not be {@code null}
	 * @param second the second column getter (method reference); must not be {@code null}
	 * @param secondAlias the second column's alias; must not be {@code null}
	 * @param third the third column getter (method reference); must not be {@code null}
	 * @param thirdAlias the third column's alias; must not be {@code null}
	 * @param fourth the fourth column getter (method reference); must not be {@code null}
	 * @param fourthAlias the fourth column's alias; must not be {@code null}
	 * @param fifth the fifth column getter (method reference); must not be {@code null}
	 * @param fifthAlias the fifth column's alias; must not be {@code null}
	 * @param sixth the sixth column getter (method reference); must not be {@code null}
	 * @param sixthAlias the sixth column's alias; must not be {@code null}
	 * @param seventh the seventh column getter (method reference); must not be {@code null}
	 * @param seventhAlias the seventh column's alias; must not be {@code null}
	 * @param eighth the eighth column getter (method reference); must not be {@code null}
	 * @param eighthAlias the eighth column's alias; must not be {@code null}
	 * @param ninth the ninth column getter (method reference); must not be {@code null}
	 * @param ninthAlias the ninth column's alias; must not be {@code null}
	 * @param tenth the tenth column getter (method reference); must not be {@code null}
	 * @param tenthAlias the tenth column's alias; must not be {@code null}
	 * @param eleventh the eleventh column getter (method reference); must not be {@code null}
	 * @param eleventhAlias the eleventh column's alias; must not be {@code null}
	 * @param <T1> the entity type owning the first column
	 * @param <T2> the entity type owning the second column
	 * @param <T3> the entity type owning the third column
	 * @param <T4> the entity type owning the fourth column
	 * @param <T5> the entity type owning the fifth column
	 * @param <T6> the entity type owning the sixth column
	 * @param <T7> the entity type owning the seventh column
	 * @param <T8> the entity type owning the eighth column
	 * @param <T9> the entity type owning the ninth column
	 * @param <T10> the entity type owning the tenth column
	 * @param <T11> the entity type owning the eleventh column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> SelectStep<Object> SELECTㅤ(
			TypedCol<T1, ?> first, String firstAlias,
			TypedCol<T2, ?> second, String secondAlias,
			TypedCol<T3, ?> third, String thirdAlias,
			TypedCol<T4, ?> fourth, String fourthAlias,
			TypedCol<T5, ?> fifth, String fifthAlias,
			TypedCol<T6, ?> sixth, String sixthAlias,
			TypedCol<T7, ?> seventh, String seventhAlias,
			TypedCol<T8, ?> eighth, String eighthAlias,
			TypedCol<T9, ?> ninth, String ninthAlias,
			TypedCol<T10, ?> tenth, String tenthAlias,
			TypedCol<T11, ?> eleventh, String eleventhAlias) {
		return new SelectStep<>(new Q<Object>().addSelect(
				Expr.typedCol(first).ㅤAS(firstAlias),
				Expr.typedCol(second).ㅤAS(secondAlias),
				Expr.typedCol(third).ㅤAS(thirdAlias),
				Expr.typedCol(fourth).ㅤAS(fourthAlias),
				Expr.typedCol(fifth).ㅤAS(fifthAlias),
				Expr.typedCol(sixth).ㅤAS(sixthAlias),
				Expr.typedCol(seventh).ㅤAS(seventhAlias),
				Expr.typedCol(eighth).ㅤAS(eighthAlias),
				Expr.typedCol(ninth).ㅤAS(ninthAlias),
				Expr.typedCol(tenth).ㅤAS(tenthAlias),
				Expr.typedCol(eleventh).ㅤAS(eleventhAlias)));
	}

	/**
	 * SELECT of 12 explicitly aliased bare columns — avoids any {@code typedCol(...)}-style
	 * wrapping.
	 *
	 * <p>Example: {@code SELECT(User::id, "id", User::name, "name")} &rarr;
	 * {@code select id as id, name as name}.</p>
	 *
	 * @param first the first column getter (method reference); must not be {@code null}
	 * @param firstAlias the first column's alias; must not be {@code null}
	 * @param second the second column getter (method reference); must not be {@code null}
	 * @param secondAlias the second column's alias; must not be {@code null}
	 * @param third the third column getter (method reference); must not be {@code null}
	 * @param thirdAlias the third column's alias; must not be {@code null}
	 * @param fourth the fourth column getter (method reference); must not be {@code null}
	 * @param fourthAlias the fourth column's alias; must not be {@code null}
	 * @param fifth the fifth column getter (method reference); must not be {@code null}
	 * @param fifthAlias the fifth column's alias; must not be {@code null}
	 * @param sixth the sixth column getter (method reference); must not be {@code null}
	 * @param sixthAlias the sixth column's alias; must not be {@code null}
	 * @param seventh the seventh column getter (method reference); must not be {@code null}
	 * @param seventhAlias the seventh column's alias; must not be {@code null}
	 * @param eighth the eighth column getter (method reference); must not be {@code null}
	 * @param eighthAlias the eighth column's alias; must not be {@code null}
	 * @param ninth the ninth column getter (method reference); must not be {@code null}
	 * @param ninthAlias the ninth column's alias; must not be {@code null}
	 * @param tenth the tenth column getter (method reference); must not be {@code null}
	 * @param tenthAlias the tenth column's alias; must not be {@code null}
	 * @param eleventh the eleventh column getter (method reference); must not be {@code null}
	 * @param eleventhAlias the eleventh column's alias; must not be {@code null}
	 * @param twelfth the twelfth column getter (method reference); must not be {@code null}
	 * @param twelfthAlias the twelfth column's alias; must not be {@code null}
	 * @param <T1> the entity type owning the first column
	 * @param <T2> the entity type owning the second column
	 * @param <T3> the entity type owning the third column
	 * @param <T4> the entity type owning the fourth column
	 * @param <T5> the entity type owning the fifth column
	 * @param <T6> the entity type owning the sixth column
	 * @param <T7> the entity type owning the seventh column
	 * @param <T8> the entity type owning the eighth column
	 * @param <T9> the entity type owning the ninth column
	 * @param <T10> the entity type owning the tenth column
	 * @param <T11> the entity type owning the eleventh column
	 * @param <T12> the entity type owning the twelfth column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> SelectStep<Object> SELECTㅤ(
			TypedCol<T1, ?> first, String firstAlias,
			TypedCol<T2, ?> second, String secondAlias,
			TypedCol<T3, ?> third, String thirdAlias,
			TypedCol<T4, ?> fourth, String fourthAlias,
			TypedCol<T5, ?> fifth, String fifthAlias,
			TypedCol<T6, ?> sixth, String sixthAlias,
			TypedCol<T7, ?> seventh, String seventhAlias,
			TypedCol<T8, ?> eighth, String eighthAlias,
			TypedCol<T9, ?> ninth, String ninthAlias,
			TypedCol<T10, ?> tenth, String tenthAlias,
			TypedCol<T11, ?> eleventh, String eleventhAlias,
			TypedCol<T12, ?> twelfth, String twelfthAlias) {
		return new SelectStep<>(new Q<Object>().addSelect(
				Expr.typedCol(first).ㅤAS(firstAlias),
				Expr.typedCol(second).ㅤAS(secondAlias),
				Expr.typedCol(third).ㅤAS(thirdAlias),
				Expr.typedCol(fourth).ㅤAS(fourthAlias),
				Expr.typedCol(fifth).ㅤAS(fifthAlias),
				Expr.typedCol(sixth).ㅤAS(sixthAlias),
				Expr.typedCol(seventh).ㅤAS(seventhAlias),
				Expr.typedCol(eighth).ㅤAS(eighthAlias),
				Expr.typedCol(ninth).ㅤAS(ninthAlias),
				Expr.typedCol(tenth).ㅤAS(tenthAlias),
				Expr.typedCol(eleventh).ㅤAS(eleventhAlias),
				Expr.typedCol(twelfth).ㅤAS(twelfthAlias)));
	}

	/**
	 * SELECT of 13 explicitly aliased bare columns — avoids any {@code typedCol(...)}-style
	 * wrapping.
	 *
	 * <p>Example: {@code SELECT(User::id, "id", User::name, "name")} &rarr;
	 * {@code select id as id, name as name}.</p>
	 *
	 * @param first the first column getter (method reference); must not be {@code null}
	 * @param firstAlias the first column's alias; must not be {@code null}
	 * @param second the second column getter (method reference); must not be {@code null}
	 * @param secondAlias the second column's alias; must not be {@code null}
	 * @param third the third column getter (method reference); must not be {@code null}
	 * @param thirdAlias the third column's alias; must not be {@code null}
	 * @param fourth the fourth column getter (method reference); must not be {@code null}
	 * @param fourthAlias the fourth column's alias; must not be {@code null}
	 * @param fifth the fifth column getter (method reference); must not be {@code null}
	 * @param fifthAlias the fifth column's alias; must not be {@code null}
	 * @param sixth the sixth column getter (method reference); must not be {@code null}
	 * @param sixthAlias the sixth column's alias; must not be {@code null}
	 * @param seventh the seventh column getter (method reference); must not be {@code null}
	 * @param seventhAlias the seventh column's alias; must not be {@code null}
	 * @param eighth the eighth column getter (method reference); must not be {@code null}
	 * @param eighthAlias the eighth column's alias; must not be {@code null}
	 * @param ninth the ninth column getter (method reference); must not be {@code null}
	 * @param ninthAlias the ninth column's alias; must not be {@code null}
	 * @param tenth the tenth column getter (method reference); must not be {@code null}
	 * @param tenthAlias the tenth column's alias; must not be {@code null}
	 * @param eleventh the eleventh column getter (method reference); must not be {@code null}
	 * @param eleventhAlias the eleventh column's alias; must not be {@code null}
	 * @param twelfth the twelfth column getter (method reference); must not be {@code null}
	 * @param twelfthAlias the twelfth column's alias; must not be {@code null}
	 * @param thirteenth the thirteenth column getter (method reference); must not be {@code null}
	 * @param thirteenthAlias the thirteenth column's alias; must not be {@code null}
	 * @param <T1> the entity type owning the first column
	 * @param <T2> the entity type owning the second column
	 * @param <T3> the entity type owning the third column
	 * @param <T4> the entity type owning the fourth column
	 * @param <T5> the entity type owning the fifth column
	 * @param <T6> the entity type owning the sixth column
	 * @param <T7> the entity type owning the seventh column
	 * @param <T8> the entity type owning the eighth column
	 * @param <T9> the entity type owning the ninth column
	 * @param <T10> the entity type owning the tenth column
	 * @param <T11> the entity type owning the eleventh column
	 * @param <T12> the entity type owning the twelfth column
	 * @param <T13> the entity type owning the thirteenth column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> SelectStep<Object> SELECTㅤ(
			TypedCol<T1, ?> first, String firstAlias,
			TypedCol<T2, ?> second, String secondAlias,
			TypedCol<T3, ?> third, String thirdAlias,
			TypedCol<T4, ?> fourth, String fourthAlias,
			TypedCol<T5, ?> fifth, String fifthAlias,
			TypedCol<T6, ?> sixth, String sixthAlias,
			TypedCol<T7, ?> seventh, String seventhAlias,
			TypedCol<T8, ?> eighth, String eighthAlias,
			TypedCol<T9, ?> ninth, String ninthAlias,
			TypedCol<T10, ?> tenth, String tenthAlias,
			TypedCol<T11, ?> eleventh, String eleventhAlias,
			TypedCol<T12, ?> twelfth, String twelfthAlias,
			TypedCol<T13, ?> thirteenth, String thirteenthAlias) {
		return new SelectStep<>(new Q<Object>().addSelect(
				Expr.typedCol(first).ㅤAS(firstAlias),
				Expr.typedCol(second).ㅤAS(secondAlias),
				Expr.typedCol(third).ㅤAS(thirdAlias),
				Expr.typedCol(fourth).ㅤAS(fourthAlias),
				Expr.typedCol(fifth).ㅤAS(fifthAlias),
				Expr.typedCol(sixth).ㅤAS(sixthAlias),
				Expr.typedCol(seventh).ㅤAS(seventhAlias),
				Expr.typedCol(eighth).ㅤAS(eighthAlias),
				Expr.typedCol(ninth).ㅤAS(ninthAlias),
				Expr.typedCol(tenth).ㅤAS(tenthAlias),
				Expr.typedCol(eleventh).ㅤAS(eleventhAlias),
				Expr.typedCol(twelfth).ㅤAS(twelfthAlias),
				Expr.typedCol(thirteenth).ㅤAS(thirteenthAlias)));
	}

	/**
	 * SELECT of 14 explicitly aliased bare columns — avoids any {@code typedCol(...)}-style
	 * wrapping.
	 *
	 * <p>Example: {@code SELECT(User::id, "id", User::name, "name")} &rarr;
	 * {@code select id as id, name as name}.</p>
	 *
	 * @param first the first column getter (method reference); must not be {@code null}
	 * @param firstAlias the first column's alias; must not be {@code null}
	 * @param second the second column getter (method reference); must not be {@code null}
	 * @param secondAlias the second column's alias; must not be {@code null}
	 * @param third the third column getter (method reference); must not be {@code null}
	 * @param thirdAlias the third column's alias; must not be {@code null}
	 * @param fourth the fourth column getter (method reference); must not be {@code null}
	 * @param fourthAlias the fourth column's alias; must not be {@code null}
	 * @param fifth the fifth column getter (method reference); must not be {@code null}
	 * @param fifthAlias the fifth column's alias; must not be {@code null}
	 * @param sixth the sixth column getter (method reference); must not be {@code null}
	 * @param sixthAlias the sixth column's alias; must not be {@code null}
	 * @param seventh the seventh column getter (method reference); must not be {@code null}
	 * @param seventhAlias the seventh column's alias; must not be {@code null}
	 * @param eighth the eighth column getter (method reference); must not be {@code null}
	 * @param eighthAlias the eighth column's alias; must not be {@code null}
	 * @param ninth the ninth column getter (method reference); must not be {@code null}
	 * @param ninthAlias the ninth column's alias; must not be {@code null}
	 * @param tenth the tenth column getter (method reference); must not be {@code null}
	 * @param tenthAlias the tenth column's alias; must not be {@code null}
	 * @param eleventh the eleventh column getter (method reference); must not be {@code null}
	 * @param eleventhAlias the eleventh column's alias; must not be {@code null}
	 * @param twelfth the twelfth column getter (method reference); must not be {@code null}
	 * @param twelfthAlias the twelfth column's alias; must not be {@code null}
	 * @param thirteenth the thirteenth column getter (method reference); must not be {@code null}
	 * @param thirteenthAlias the thirteenth column's alias; must not be {@code null}
	 * @param fourteenth the fourteenth column getter (method reference); must not be {@code null}
	 * @param fourteenthAlias the fourteenth column's alias; must not be {@code null}
	 * @param <T1> the entity type owning the first column
	 * @param <T2> the entity type owning the second column
	 * @param <T3> the entity type owning the third column
	 * @param <T4> the entity type owning the fourth column
	 * @param <T5> the entity type owning the fifth column
	 * @param <T6> the entity type owning the sixth column
	 * @param <T7> the entity type owning the seventh column
	 * @param <T8> the entity type owning the eighth column
	 * @param <T9> the entity type owning the ninth column
	 * @param <T10> the entity type owning the tenth column
	 * @param <T11> the entity type owning the eleventh column
	 * @param <T12> the entity type owning the twelfth column
	 * @param <T13> the entity type owning the thirteenth column
	 * @param <T14> the entity type owning the fourteenth column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> SelectStep<Object> SELECTㅤ(
			TypedCol<T1, ?> first, String firstAlias,
			TypedCol<T2, ?> second, String secondAlias,
			TypedCol<T3, ?> third, String thirdAlias,
			TypedCol<T4, ?> fourth, String fourthAlias,
			TypedCol<T5, ?> fifth, String fifthAlias,
			TypedCol<T6, ?> sixth, String sixthAlias,
			TypedCol<T7, ?> seventh, String seventhAlias,
			TypedCol<T8, ?> eighth, String eighthAlias,
			TypedCol<T9, ?> ninth, String ninthAlias,
			TypedCol<T10, ?> tenth, String tenthAlias,
			TypedCol<T11, ?> eleventh, String eleventhAlias,
			TypedCol<T12, ?> twelfth, String twelfthAlias,
			TypedCol<T13, ?> thirteenth, String thirteenthAlias,
			TypedCol<T14, ?> fourteenth, String fourteenthAlias) {
		return new SelectStep<>(new Q<Object>().addSelect(
				Expr.typedCol(first).ㅤAS(firstAlias),
				Expr.typedCol(second).ㅤAS(secondAlias),
				Expr.typedCol(third).ㅤAS(thirdAlias),
				Expr.typedCol(fourth).ㅤAS(fourthAlias),
				Expr.typedCol(fifth).ㅤAS(fifthAlias),
				Expr.typedCol(sixth).ㅤAS(sixthAlias),
				Expr.typedCol(seventh).ㅤAS(seventhAlias),
				Expr.typedCol(eighth).ㅤAS(eighthAlias),
				Expr.typedCol(ninth).ㅤAS(ninthAlias),
				Expr.typedCol(tenth).ㅤAS(tenthAlias),
				Expr.typedCol(eleventh).ㅤAS(eleventhAlias),
				Expr.typedCol(twelfth).ㅤAS(twelfthAlias),
				Expr.typedCol(thirteenth).ㅤAS(thirteenthAlias),
				Expr.typedCol(fourteenth).ㅤAS(fourteenthAlias)));
	}

	/**
	 * SELECT of 15 explicitly aliased bare columns — avoids any {@code typedCol(...)}-style
	 * wrapping.
	 *
	 * <p>Example: {@code SELECT(User::id, "id", User::name, "name")} &rarr;
	 * {@code select id as id, name as name}.</p>
	 *
	 * @param first the first column getter (method reference); must not be {@code null}
	 * @param firstAlias the first column's alias; must not be {@code null}
	 * @param second the second column getter (method reference); must not be {@code null}
	 * @param secondAlias the second column's alias; must not be {@code null}
	 * @param third the third column getter (method reference); must not be {@code null}
	 * @param thirdAlias the third column's alias; must not be {@code null}
	 * @param fourth the fourth column getter (method reference); must not be {@code null}
	 * @param fourthAlias the fourth column's alias; must not be {@code null}
	 * @param fifth the fifth column getter (method reference); must not be {@code null}
	 * @param fifthAlias the fifth column's alias; must not be {@code null}
	 * @param sixth the sixth column getter (method reference); must not be {@code null}
	 * @param sixthAlias the sixth column's alias; must not be {@code null}
	 * @param seventh the seventh column getter (method reference); must not be {@code null}
	 * @param seventhAlias the seventh column's alias; must not be {@code null}
	 * @param eighth the eighth column getter (method reference); must not be {@code null}
	 * @param eighthAlias the eighth column's alias; must not be {@code null}
	 * @param ninth the ninth column getter (method reference); must not be {@code null}
	 * @param ninthAlias the ninth column's alias; must not be {@code null}
	 * @param tenth the tenth column getter (method reference); must not be {@code null}
	 * @param tenthAlias the tenth column's alias; must not be {@code null}
	 * @param eleventh the eleventh column getter (method reference); must not be {@code null}
	 * @param eleventhAlias the eleventh column's alias; must not be {@code null}
	 * @param twelfth the twelfth column getter (method reference); must not be {@code null}
	 * @param twelfthAlias the twelfth column's alias; must not be {@code null}
	 * @param thirteenth the thirteenth column getter (method reference); must not be {@code null}
	 * @param thirteenthAlias the thirteenth column's alias; must not be {@code null}
	 * @param fourteenth the fourteenth column getter (method reference); must not be {@code null}
	 * @param fourteenthAlias the fourteenth column's alias; must not be {@code null}
	 * @param fifteenth the fifteenth column getter (method reference); must not be {@code null}
	 * @param fifteenthAlias the fifteenth column's alias; must not be {@code null}
	 * @param <T1> the entity type owning the first column
	 * @param <T2> the entity type owning the second column
	 * @param <T3> the entity type owning the third column
	 * @param <T4> the entity type owning the fourth column
	 * @param <T5> the entity type owning the fifth column
	 * @param <T6> the entity type owning the sixth column
	 * @param <T7> the entity type owning the seventh column
	 * @param <T8> the entity type owning the eighth column
	 * @param <T9> the entity type owning the ninth column
	 * @param <T10> the entity type owning the tenth column
	 * @param <T11> the entity type owning the eleventh column
	 * @param <T12> the entity type owning the twelfth column
	 * @param <T13> the entity type owning the thirteenth column
	 * @param <T14> the entity type owning the fourteenth column
	 * @param <T15> the entity type owning the fifteenth column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> SelectStep<Object> SELECTㅤ(
			TypedCol<T1, ?> first, String firstAlias,
			TypedCol<T2, ?> second, String secondAlias,
			TypedCol<T3, ?> third, String thirdAlias,
			TypedCol<T4, ?> fourth, String fourthAlias,
			TypedCol<T5, ?> fifth, String fifthAlias,
			TypedCol<T6, ?> sixth, String sixthAlias,
			TypedCol<T7, ?> seventh, String seventhAlias,
			TypedCol<T8, ?> eighth, String eighthAlias,
			TypedCol<T9, ?> ninth, String ninthAlias,
			TypedCol<T10, ?> tenth, String tenthAlias,
			TypedCol<T11, ?> eleventh, String eleventhAlias,
			TypedCol<T12, ?> twelfth, String twelfthAlias,
			TypedCol<T13, ?> thirteenth, String thirteenthAlias,
			TypedCol<T14, ?> fourteenth, String fourteenthAlias,
			TypedCol<T15, ?> fifteenth, String fifteenthAlias) {
		return new SelectStep<>(new Q<Object>().addSelect(
				Expr.typedCol(first).ㅤAS(firstAlias),
				Expr.typedCol(second).ㅤAS(secondAlias),
				Expr.typedCol(third).ㅤAS(thirdAlias),
				Expr.typedCol(fourth).ㅤAS(fourthAlias),
				Expr.typedCol(fifth).ㅤAS(fifthAlias),
				Expr.typedCol(sixth).ㅤAS(sixthAlias),
				Expr.typedCol(seventh).ㅤAS(seventhAlias),
				Expr.typedCol(eighth).ㅤAS(eighthAlias),
				Expr.typedCol(ninth).ㅤAS(ninthAlias),
				Expr.typedCol(tenth).ㅤAS(tenthAlias),
				Expr.typedCol(eleventh).ㅤAS(eleventhAlias),
				Expr.typedCol(twelfth).ㅤAS(twelfthAlias),
				Expr.typedCol(thirteenth).ㅤAS(thirteenthAlias),
				Expr.typedCol(fourteenth).ㅤAS(fourteenthAlias),
				Expr.typedCol(fifteenth).ㅤAS(fifteenthAlias)));
	}

	/**
	 * SELECT of 16 explicitly aliased bare columns — avoids any {@code typedCol(...)}-style
	 * wrapping.
	 *
	 * <p>Example: {@code SELECT(User::id, "id", User::name, "name")} &rarr;
	 * {@code select id as id, name as name}.</p>
	 *
	 * @param first the first column getter (method reference); must not be {@code null}
	 * @param firstAlias the first column's alias; must not be {@code null}
	 * @param second the second column getter (method reference); must not be {@code null}
	 * @param secondAlias the second column's alias; must not be {@code null}
	 * @param third the third column getter (method reference); must not be {@code null}
	 * @param thirdAlias the third column's alias; must not be {@code null}
	 * @param fourth the fourth column getter (method reference); must not be {@code null}
	 * @param fourthAlias the fourth column's alias; must not be {@code null}
	 * @param fifth the fifth column getter (method reference); must not be {@code null}
	 * @param fifthAlias the fifth column's alias; must not be {@code null}
	 * @param sixth the sixth column getter (method reference); must not be {@code null}
	 * @param sixthAlias the sixth column's alias; must not be {@code null}
	 * @param seventh the seventh column getter (method reference); must not be {@code null}
	 * @param seventhAlias the seventh column's alias; must not be {@code null}
	 * @param eighth the eighth column getter (method reference); must not be {@code null}
	 * @param eighthAlias the eighth column's alias; must not be {@code null}
	 * @param ninth the ninth column getter (method reference); must not be {@code null}
	 * @param ninthAlias the ninth column's alias; must not be {@code null}
	 * @param tenth the tenth column getter (method reference); must not be {@code null}
	 * @param tenthAlias the tenth column's alias; must not be {@code null}
	 * @param eleventh the eleventh column getter (method reference); must not be {@code null}
	 * @param eleventhAlias the eleventh column's alias; must not be {@code null}
	 * @param twelfth the twelfth column getter (method reference); must not be {@code null}
	 * @param twelfthAlias the twelfth column's alias; must not be {@code null}
	 * @param thirteenth the thirteenth column getter (method reference); must not be {@code null}
	 * @param thirteenthAlias the thirteenth column's alias; must not be {@code null}
	 * @param fourteenth the fourteenth column getter (method reference); must not be {@code null}
	 * @param fourteenthAlias the fourteenth column's alias; must not be {@code null}
	 * @param fifteenth the fifteenth column getter (method reference); must not be {@code null}
	 * @param fifteenthAlias the fifteenth column's alias; must not be {@code null}
	 * @param sixteenth the sixteenth column getter (method reference); must not be {@code null}
	 * @param sixteenthAlias the sixteenth column's alias; must not be {@code null}
	 * @param <T1> the entity type owning the first column
	 * @param <T2> the entity type owning the second column
	 * @param <T3> the entity type owning the third column
	 * @param <T4> the entity type owning the fourth column
	 * @param <T5> the entity type owning the fifth column
	 * @param <T6> the entity type owning the sixth column
	 * @param <T7> the entity type owning the seventh column
	 * @param <T8> the entity type owning the eighth column
	 * @param <T9> the entity type owning the ninth column
	 * @param <T10> the entity type owning the tenth column
	 * @param <T11> the entity type owning the eleventh column
	 * @param <T12> the entity type owning the twelfth column
	 * @param <T13> the entity type owning the thirteenth column
	 * @param <T14> the entity type owning the fourteenth column
	 * @param <T15> the entity type owning the fifteenth column
	 * @param <T16> the entity type owning the sixteenth column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> SelectStep<Object> SELECTㅤ(
			TypedCol<T1, ?> first, String firstAlias,
			TypedCol<T2, ?> second, String secondAlias,
			TypedCol<T3, ?> third, String thirdAlias,
			TypedCol<T4, ?> fourth, String fourthAlias,
			TypedCol<T5, ?> fifth, String fifthAlias,
			TypedCol<T6, ?> sixth, String sixthAlias,
			TypedCol<T7, ?> seventh, String seventhAlias,
			TypedCol<T8, ?> eighth, String eighthAlias,
			TypedCol<T9, ?> ninth, String ninthAlias,
			TypedCol<T10, ?> tenth, String tenthAlias,
			TypedCol<T11, ?> eleventh, String eleventhAlias,
			TypedCol<T12, ?> twelfth, String twelfthAlias,
			TypedCol<T13, ?> thirteenth, String thirteenthAlias,
			TypedCol<T14, ?> fourteenth, String fourteenthAlias,
			TypedCol<T15, ?> fifteenth, String fifteenthAlias,
			TypedCol<T16, ?> sixteenth, String sixteenthAlias) {
		return new SelectStep<>(new Q<Object>().addSelect(
				Expr.typedCol(first).ㅤAS(firstAlias),
				Expr.typedCol(second).ㅤAS(secondAlias),
				Expr.typedCol(third).ㅤAS(thirdAlias),
				Expr.typedCol(fourth).ㅤAS(fourthAlias),
				Expr.typedCol(fifth).ㅤAS(fifthAlias),
				Expr.typedCol(sixth).ㅤAS(sixthAlias),
				Expr.typedCol(seventh).ㅤAS(seventhAlias),
				Expr.typedCol(eighth).ㅤAS(eighthAlias),
				Expr.typedCol(ninth).ㅤAS(ninthAlias),
				Expr.typedCol(tenth).ㅤAS(tenthAlias),
				Expr.typedCol(eleventh).ㅤAS(eleventhAlias),
				Expr.typedCol(twelfth).ㅤAS(twelfthAlias),
				Expr.typedCol(thirteenth).ㅤAS(thirteenthAlias),
				Expr.typedCol(fourteenth).ㅤAS(fourteenthAlias),
				Expr.typedCol(fifteenth).ㅤAS(fifteenthAlias),
				Expr.typedCol(sixteenth).ㅤAS(sixteenthAlias)));
	}

	/**
	 * SELECT of 17 explicitly aliased bare columns — avoids any {@code typedCol(...)}-style
	 * wrapping.
	 *
	 * <p>Example: {@code SELECT(User::id, "id", User::name, "name")} &rarr;
	 * {@code select id as id, name as name}.</p>
	 *
	 * @param first the first column getter (method reference); must not be {@code null}
	 * @param firstAlias the first column's alias; must not be {@code null}
	 * @param second the second column getter (method reference); must not be {@code null}
	 * @param secondAlias the second column's alias; must not be {@code null}
	 * @param third the third column getter (method reference); must not be {@code null}
	 * @param thirdAlias the third column's alias; must not be {@code null}
	 * @param fourth the fourth column getter (method reference); must not be {@code null}
	 * @param fourthAlias the fourth column's alias; must not be {@code null}
	 * @param fifth the fifth column getter (method reference); must not be {@code null}
	 * @param fifthAlias the fifth column's alias; must not be {@code null}
	 * @param sixth the sixth column getter (method reference); must not be {@code null}
	 * @param sixthAlias the sixth column's alias; must not be {@code null}
	 * @param seventh the seventh column getter (method reference); must not be {@code null}
	 * @param seventhAlias the seventh column's alias; must not be {@code null}
	 * @param eighth the eighth column getter (method reference); must not be {@code null}
	 * @param eighthAlias the eighth column's alias; must not be {@code null}
	 * @param ninth the ninth column getter (method reference); must not be {@code null}
	 * @param ninthAlias the ninth column's alias; must not be {@code null}
	 * @param tenth the tenth column getter (method reference); must not be {@code null}
	 * @param tenthAlias the tenth column's alias; must not be {@code null}
	 * @param eleventh the eleventh column getter (method reference); must not be {@code null}
	 * @param eleventhAlias the eleventh column's alias; must not be {@code null}
	 * @param twelfth the twelfth column getter (method reference); must not be {@code null}
	 * @param twelfthAlias the twelfth column's alias; must not be {@code null}
	 * @param thirteenth the thirteenth column getter (method reference); must not be {@code null}
	 * @param thirteenthAlias the thirteenth column's alias; must not be {@code null}
	 * @param fourteenth the fourteenth column getter (method reference); must not be {@code null}
	 * @param fourteenthAlias the fourteenth column's alias; must not be {@code null}
	 * @param fifteenth the fifteenth column getter (method reference); must not be {@code null}
	 * @param fifteenthAlias the fifteenth column's alias; must not be {@code null}
	 * @param sixteenth the sixteenth column getter (method reference); must not be {@code null}
	 * @param sixteenthAlias the sixteenth column's alias; must not be {@code null}
	 * @param seventeenth the seventeenth column getter (method reference); must not be {@code null}
	 * @param seventeenthAlias the seventeenth column's alias; must not be {@code null}
	 * @param <T1> the entity type owning the first column
	 * @param <T2> the entity type owning the second column
	 * @param <T3> the entity type owning the third column
	 * @param <T4> the entity type owning the fourth column
	 * @param <T5> the entity type owning the fifth column
	 * @param <T6> the entity type owning the sixth column
	 * @param <T7> the entity type owning the seventh column
	 * @param <T8> the entity type owning the eighth column
	 * @param <T9> the entity type owning the ninth column
	 * @param <T10> the entity type owning the tenth column
	 * @param <T11> the entity type owning the eleventh column
	 * @param <T12> the entity type owning the twelfth column
	 * @param <T13> the entity type owning the thirteenth column
	 * @param <T14> the entity type owning the fourteenth column
	 * @param <T15> the entity type owning the fifteenth column
	 * @param <T16> the entity type owning the sixteenth column
	 * @param <T17> the entity type owning the seventeenth column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> SelectStep<Object> SELECTㅤ(
			TypedCol<T1, ?> first, String firstAlias,
			TypedCol<T2, ?> second, String secondAlias,
			TypedCol<T3, ?> third, String thirdAlias,
			TypedCol<T4, ?> fourth, String fourthAlias,
			TypedCol<T5, ?> fifth, String fifthAlias,
			TypedCol<T6, ?> sixth, String sixthAlias,
			TypedCol<T7, ?> seventh, String seventhAlias,
			TypedCol<T8, ?> eighth, String eighthAlias,
			TypedCol<T9, ?> ninth, String ninthAlias,
			TypedCol<T10, ?> tenth, String tenthAlias,
			TypedCol<T11, ?> eleventh, String eleventhAlias,
			TypedCol<T12, ?> twelfth, String twelfthAlias,
			TypedCol<T13, ?> thirteenth, String thirteenthAlias,
			TypedCol<T14, ?> fourteenth, String fourteenthAlias,
			TypedCol<T15, ?> fifteenth, String fifteenthAlias,
			TypedCol<T16, ?> sixteenth, String sixteenthAlias,
			TypedCol<T17, ?> seventeenth, String seventeenthAlias) {
		return new SelectStep<>(new Q<Object>().addSelect(
				Expr.typedCol(first).ㅤAS(firstAlias),
				Expr.typedCol(second).ㅤAS(secondAlias),
				Expr.typedCol(third).ㅤAS(thirdAlias),
				Expr.typedCol(fourth).ㅤAS(fourthAlias),
				Expr.typedCol(fifth).ㅤAS(fifthAlias),
				Expr.typedCol(sixth).ㅤAS(sixthAlias),
				Expr.typedCol(seventh).ㅤAS(seventhAlias),
				Expr.typedCol(eighth).ㅤAS(eighthAlias),
				Expr.typedCol(ninth).ㅤAS(ninthAlias),
				Expr.typedCol(tenth).ㅤAS(tenthAlias),
				Expr.typedCol(eleventh).ㅤAS(eleventhAlias),
				Expr.typedCol(twelfth).ㅤAS(twelfthAlias),
				Expr.typedCol(thirteenth).ㅤAS(thirteenthAlias),
				Expr.typedCol(fourteenth).ㅤAS(fourteenthAlias),
				Expr.typedCol(fifteenth).ㅤAS(fifteenthAlias),
				Expr.typedCol(sixteenth).ㅤAS(sixteenthAlias),
				Expr.typedCol(seventeenth).ㅤAS(seventeenthAlias)));
	}

	/**
	 * SELECT of 18 explicitly aliased bare columns — avoids any {@code typedCol(...)}-style
	 * wrapping.
	 *
	 * <p>Example: {@code SELECT(User::id, "id", User::name, "name")} &rarr;
	 * {@code select id as id, name as name}.</p>
	 *
	 * @param first the first column getter (method reference); must not be {@code null}
	 * @param firstAlias the first column's alias; must not be {@code null}
	 * @param second the second column getter (method reference); must not be {@code null}
	 * @param secondAlias the second column's alias; must not be {@code null}
	 * @param third the third column getter (method reference); must not be {@code null}
	 * @param thirdAlias the third column's alias; must not be {@code null}
	 * @param fourth the fourth column getter (method reference); must not be {@code null}
	 * @param fourthAlias the fourth column's alias; must not be {@code null}
	 * @param fifth the fifth column getter (method reference); must not be {@code null}
	 * @param fifthAlias the fifth column's alias; must not be {@code null}
	 * @param sixth the sixth column getter (method reference); must not be {@code null}
	 * @param sixthAlias the sixth column's alias; must not be {@code null}
	 * @param seventh the seventh column getter (method reference); must not be {@code null}
	 * @param seventhAlias the seventh column's alias; must not be {@code null}
	 * @param eighth the eighth column getter (method reference); must not be {@code null}
	 * @param eighthAlias the eighth column's alias; must not be {@code null}
	 * @param ninth the ninth column getter (method reference); must not be {@code null}
	 * @param ninthAlias the ninth column's alias; must not be {@code null}
	 * @param tenth the tenth column getter (method reference); must not be {@code null}
	 * @param tenthAlias the tenth column's alias; must not be {@code null}
	 * @param eleventh the eleventh column getter (method reference); must not be {@code null}
	 * @param eleventhAlias the eleventh column's alias; must not be {@code null}
	 * @param twelfth the twelfth column getter (method reference); must not be {@code null}
	 * @param twelfthAlias the twelfth column's alias; must not be {@code null}
	 * @param thirteenth the thirteenth column getter (method reference); must not be {@code null}
	 * @param thirteenthAlias the thirteenth column's alias; must not be {@code null}
	 * @param fourteenth the fourteenth column getter (method reference); must not be {@code null}
	 * @param fourteenthAlias the fourteenth column's alias; must not be {@code null}
	 * @param fifteenth the fifteenth column getter (method reference); must not be {@code null}
	 * @param fifteenthAlias the fifteenth column's alias; must not be {@code null}
	 * @param sixteenth the sixteenth column getter (method reference); must not be {@code null}
	 * @param sixteenthAlias the sixteenth column's alias; must not be {@code null}
	 * @param seventeenth the seventeenth column getter (method reference); must not be {@code null}
	 * @param seventeenthAlias the seventeenth column's alias; must not be {@code null}
	 * @param eighteenth the eighteenth column getter (method reference); must not be {@code null}
	 * @param eighteenthAlias the eighteenth column's alias; must not be {@code null}
	 * @param <T1> the entity type owning the first column
	 * @param <T2> the entity type owning the second column
	 * @param <T3> the entity type owning the third column
	 * @param <T4> the entity type owning the fourth column
	 * @param <T5> the entity type owning the fifth column
	 * @param <T6> the entity type owning the sixth column
	 * @param <T7> the entity type owning the seventh column
	 * @param <T8> the entity type owning the eighth column
	 * @param <T9> the entity type owning the ninth column
	 * @param <T10> the entity type owning the tenth column
	 * @param <T11> the entity type owning the eleventh column
	 * @param <T12> the entity type owning the twelfth column
	 * @param <T13> the entity type owning the thirteenth column
	 * @param <T14> the entity type owning the fourteenth column
	 * @param <T15> the entity type owning the fifteenth column
	 * @param <T16> the entity type owning the sixteenth column
	 * @param <T17> the entity type owning the seventeenth column
	 * @param <T18> the entity type owning the eighteenth column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17,
			T18>
			SelectStep<Object> SELECTㅤ(
			TypedCol<T1, ?> first, String firstAlias,
			TypedCol<T2, ?> second, String secondAlias,
			TypedCol<T3, ?> third, String thirdAlias,
			TypedCol<T4, ?> fourth, String fourthAlias,
			TypedCol<T5, ?> fifth, String fifthAlias,
			TypedCol<T6, ?> sixth, String sixthAlias,
			TypedCol<T7, ?> seventh, String seventhAlias,
			TypedCol<T8, ?> eighth, String eighthAlias,
			TypedCol<T9, ?> ninth, String ninthAlias,
			TypedCol<T10, ?> tenth, String tenthAlias,
			TypedCol<T11, ?> eleventh, String eleventhAlias,
			TypedCol<T12, ?> twelfth, String twelfthAlias,
			TypedCol<T13, ?> thirteenth, String thirteenthAlias,
			TypedCol<T14, ?> fourteenth, String fourteenthAlias,
			TypedCol<T15, ?> fifteenth, String fifteenthAlias,
			TypedCol<T16, ?> sixteenth, String sixteenthAlias,
			TypedCol<T17, ?> seventeenth, String seventeenthAlias,
			TypedCol<T18, ?> eighteenth, String eighteenthAlias) {
		return new SelectStep<>(new Q<Object>().addSelect(
				Expr.typedCol(first).ㅤAS(firstAlias),
				Expr.typedCol(second).ㅤAS(secondAlias),
				Expr.typedCol(third).ㅤAS(thirdAlias),
				Expr.typedCol(fourth).ㅤAS(fourthAlias),
				Expr.typedCol(fifth).ㅤAS(fifthAlias),
				Expr.typedCol(sixth).ㅤAS(sixthAlias),
				Expr.typedCol(seventh).ㅤAS(seventhAlias),
				Expr.typedCol(eighth).ㅤAS(eighthAlias),
				Expr.typedCol(ninth).ㅤAS(ninthAlias),
				Expr.typedCol(tenth).ㅤAS(tenthAlias),
				Expr.typedCol(eleventh).ㅤAS(eleventhAlias),
				Expr.typedCol(twelfth).ㅤAS(twelfthAlias),
				Expr.typedCol(thirteenth).ㅤAS(thirteenthAlias),
				Expr.typedCol(fourteenth).ㅤAS(fourteenthAlias),
				Expr.typedCol(fifteenth).ㅤAS(fifteenthAlias),
				Expr.typedCol(sixteenth).ㅤAS(sixteenthAlias),
				Expr.typedCol(seventeenth).ㅤAS(seventeenthAlias),
				Expr.typedCol(eighteenth).ㅤAS(eighteenthAlias)));
	}

	/**
	 * SELECT of 19 explicitly aliased bare columns — avoids any {@code typedCol(...)}-style
	 * wrapping.
	 *
	 * <p>Example: {@code SELECT(User::id, "id", User::name, "name")} &rarr;
	 * {@code select id as id, name as name}.</p>
	 *
	 * @param first the first column getter (method reference); must not be {@code null}
	 * @param firstAlias the first column's alias; must not be {@code null}
	 * @param second the second column getter (method reference); must not be {@code null}
	 * @param secondAlias the second column's alias; must not be {@code null}
	 * @param third the third column getter (method reference); must not be {@code null}
	 * @param thirdAlias the third column's alias; must not be {@code null}
	 * @param fourth the fourth column getter (method reference); must not be {@code null}
	 * @param fourthAlias the fourth column's alias; must not be {@code null}
	 * @param fifth the fifth column getter (method reference); must not be {@code null}
	 * @param fifthAlias the fifth column's alias; must not be {@code null}
	 * @param sixth the sixth column getter (method reference); must not be {@code null}
	 * @param sixthAlias the sixth column's alias; must not be {@code null}
	 * @param seventh the seventh column getter (method reference); must not be {@code null}
	 * @param seventhAlias the seventh column's alias; must not be {@code null}
	 * @param eighth the eighth column getter (method reference); must not be {@code null}
	 * @param eighthAlias the eighth column's alias; must not be {@code null}
	 * @param ninth the ninth column getter (method reference); must not be {@code null}
	 * @param ninthAlias the ninth column's alias; must not be {@code null}
	 * @param tenth the tenth column getter (method reference); must not be {@code null}
	 * @param tenthAlias the tenth column's alias; must not be {@code null}
	 * @param eleventh the eleventh column getter (method reference); must not be {@code null}
	 * @param eleventhAlias the eleventh column's alias; must not be {@code null}
	 * @param twelfth the twelfth column getter (method reference); must not be {@code null}
	 * @param twelfthAlias the twelfth column's alias; must not be {@code null}
	 * @param thirteenth the thirteenth column getter (method reference); must not be {@code null}
	 * @param thirteenthAlias the thirteenth column's alias; must not be {@code null}
	 * @param fourteenth the fourteenth column getter (method reference); must not be {@code null}
	 * @param fourteenthAlias the fourteenth column's alias; must not be {@code null}
	 * @param fifteenth the fifteenth column getter (method reference); must not be {@code null}
	 * @param fifteenthAlias the fifteenth column's alias; must not be {@code null}
	 * @param sixteenth the sixteenth column getter (method reference); must not be {@code null}
	 * @param sixteenthAlias the sixteenth column's alias; must not be {@code null}
	 * @param seventeenth the seventeenth column getter (method reference); must not be {@code null}
	 * @param seventeenthAlias the seventeenth column's alias; must not be {@code null}
	 * @param eighteenth the eighteenth column getter (method reference); must not be {@code null}
	 * @param eighteenthAlias the eighteenth column's alias; must not be {@code null}
	 * @param nineteenth the nineteenth column getter (method reference); must not be {@code null}
	 * @param nineteenthAlias the nineteenth column's alias; must not be {@code null}
	 * @param <T1> the entity type owning the first column
	 * @param <T2> the entity type owning the second column
	 * @param <T3> the entity type owning the third column
	 * @param <T4> the entity type owning the fourth column
	 * @param <T5> the entity type owning the fifth column
	 * @param <T6> the entity type owning the sixth column
	 * @param <T7> the entity type owning the seventh column
	 * @param <T8> the entity type owning the eighth column
	 * @param <T9> the entity type owning the ninth column
	 * @param <T10> the entity type owning the tenth column
	 * @param <T11> the entity type owning the eleventh column
	 * @param <T12> the entity type owning the twelfth column
	 * @param <T13> the entity type owning the thirteenth column
	 * @param <T14> the entity type owning the fourteenth column
	 * @param <T15> the entity type owning the fifteenth column
	 * @param <T16> the entity type owning the sixteenth column
	 * @param <T17> the entity type owning the seventeenth column
	 * @param <T18> the entity type owning the eighteenth column
	 * @param <T19> the entity type owning the nineteenth column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17,
			T18, T19>
			SelectStep<Object> SELECTㅤ(
			TypedCol<T1, ?> first, String firstAlias,
			TypedCol<T2, ?> second, String secondAlias,
			TypedCol<T3, ?> third, String thirdAlias,
			TypedCol<T4, ?> fourth, String fourthAlias,
			TypedCol<T5, ?> fifth, String fifthAlias,
			TypedCol<T6, ?> sixth, String sixthAlias,
			TypedCol<T7, ?> seventh, String seventhAlias,
			TypedCol<T8, ?> eighth, String eighthAlias,
			TypedCol<T9, ?> ninth, String ninthAlias,
			TypedCol<T10, ?> tenth, String tenthAlias,
			TypedCol<T11, ?> eleventh, String eleventhAlias,
			TypedCol<T12, ?> twelfth, String twelfthAlias,
			TypedCol<T13, ?> thirteenth, String thirteenthAlias,
			TypedCol<T14, ?> fourteenth, String fourteenthAlias,
			TypedCol<T15, ?> fifteenth, String fifteenthAlias,
			TypedCol<T16, ?> sixteenth, String sixteenthAlias,
			TypedCol<T17, ?> seventeenth, String seventeenthAlias,
			TypedCol<T18, ?> eighteenth, String eighteenthAlias,
			TypedCol<T19, ?> nineteenth, String nineteenthAlias) {
		return new SelectStep<>(new Q<Object>().addSelect(
				Expr.typedCol(first).ㅤAS(firstAlias),
				Expr.typedCol(second).ㅤAS(secondAlias),
				Expr.typedCol(third).ㅤAS(thirdAlias),
				Expr.typedCol(fourth).ㅤAS(fourthAlias),
				Expr.typedCol(fifth).ㅤAS(fifthAlias),
				Expr.typedCol(sixth).ㅤAS(sixthAlias),
				Expr.typedCol(seventh).ㅤAS(seventhAlias),
				Expr.typedCol(eighth).ㅤAS(eighthAlias),
				Expr.typedCol(ninth).ㅤAS(ninthAlias),
				Expr.typedCol(tenth).ㅤAS(tenthAlias),
				Expr.typedCol(eleventh).ㅤAS(eleventhAlias),
				Expr.typedCol(twelfth).ㅤAS(twelfthAlias),
				Expr.typedCol(thirteenth).ㅤAS(thirteenthAlias),
				Expr.typedCol(fourteenth).ㅤAS(fourteenthAlias),
				Expr.typedCol(fifteenth).ㅤAS(fifteenthAlias),
				Expr.typedCol(sixteenth).ㅤAS(sixteenthAlias),
				Expr.typedCol(seventeenth).ㅤAS(seventeenthAlias),
				Expr.typedCol(eighteenth).ㅤAS(eighteenthAlias),
				Expr.typedCol(nineteenth).ㅤAS(nineteenthAlias)));
	}

	/**
	 * SELECT of 20 explicitly aliased bare columns — avoids any {@code typedCol(...)}-style
	 * wrapping.
	 *
	 * <p>Example: {@code SELECT(User::id, "id", User::name, "name")} &rarr;
	 * {@code select id as id, name as name}.</p>
	 *
	 * @param first the first column getter (method reference); must not be {@code null}
	 * @param firstAlias the first column's alias; must not be {@code null}
	 * @param second the second column getter (method reference); must not be {@code null}
	 * @param secondAlias the second column's alias; must not be {@code null}
	 * @param third the third column getter (method reference); must not be {@code null}
	 * @param thirdAlias the third column's alias; must not be {@code null}
	 * @param fourth the fourth column getter (method reference); must not be {@code null}
	 * @param fourthAlias the fourth column's alias; must not be {@code null}
	 * @param fifth the fifth column getter (method reference); must not be {@code null}
	 * @param fifthAlias the fifth column's alias; must not be {@code null}
	 * @param sixth the sixth column getter (method reference); must not be {@code null}
	 * @param sixthAlias the sixth column's alias; must not be {@code null}
	 * @param seventh the seventh column getter (method reference); must not be {@code null}
	 * @param seventhAlias the seventh column's alias; must not be {@code null}
	 * @param eighth the eighth column getter (method reference); must not be {@code null}
	 * @param eighthAlias the eighth column's alias; must not be {@code null}
	 * @param ninth the ninth column getter (method reference); must not be {@code null}
	 * @param ninthAlias the ninth column's alias; must not be {@code null}
	 * @param tenth the tenth column getter (method reference); must not be {@code null}
	 * @param tenthAlias the tenth column's alias; must not be {@code null}
	 * @param eleventh the eleventh column getter (method reference); must not be {@code null}
	 * @param eleventhAlias the eleventh column's alias; must not be {@code null}
	 * @param twelfth the twelfth column getter (method reference); must not be {@code null}
	 * @param twelfthAlias the twelfth column's alias; must not be {@code null}
	 * @param thirteenth the thirteenth column getter (method reference); must not be {@code null}
	 * @param thirteenthAlias the thirteenth column's alias; must not be {@code null}
	 * @param fourteenth the fourteenth column getter (method reference); must not be {@code null}
	 * @param fourteenthAlias the fourteenth column's alias; must not be {@code null}
	 * @param fifteenth the fifteenth column getter (method reference); must not be {@code null}
	 * @param fifteenthAlias the fifteenth column's alias; must not be {@code null}
	 * @param sixteenth the sixteenth column getter (method reference); must not be {@code null}
	 * @param sixteenthAlias the sixteenth column's alias; must not be {@code null}
	 * @param seventeenth the seventeenth column getter (method reference); must not be {@code null}
	 * @param seventeenthAlias the seventeenth column's alias; must not be {@code null}
	 * @param eighteenth the eighteenth column getter (method reference); must not be {@code null}
	 * @param eighteenthAlias the eighteenth column's alias; must not be {@code null}
	 * @param nineteenth the nineteenth column getter (method reference); must not be {@code null}
	 * @param nineteenthAlias the nineteenth column's alias; must not be {@code null}
	 * @param twentieth the twentieth column getter (method reference); must not be {@code null}
	 * @param twentiethAlias the twentieth column's alias; must not be {@code null}
	 * @param <T1> the entity type owning the first column
	 * @param <T2> the entity type owning the second column
	 * @param <T3> the entity type owning the third column
	 * @param <T4> the entity type owning the fourth column
	 * @param <T5> the entity type owning the fifth column
	 * @param <T6> the entity type owning the sixth column
	 * @param <T7> the entity type owning the seventh column
	 * @param <T8> the entity type owning the eighth column
	 * @param <T9> the entity type owning the ninth column
	 * @param <T10> the entity type owning the tenth column
	 * @param <T11> the entity type owning the eleventh column
	 * @param <T12> the entity type owning the twelfth column
	 * @param <T13> the entity type owning the thirteenth column
	 * @param <T14> the entity type owning the fourteenth column
	 * @param <T15> the entity type owning the fifteenth column
	 * @param <T16> the entity type owning the sixteenth column
	 * @param <T17> the entity type owning the seventeenth column
	 * @param <T18> the entity type owning the eighteenth column
	 * @param <T19> the entity type owning the nineteenth column
	 * @param <T20> the entity type owning the twentieth column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17,
			T18, T19, T20>
			SelectStep<Object> SELECTㅤ(
			TypedCol<T1, ?> first, String firstAlias,
			TypedCol<T2, ?> second, String secondAlias,
			TypedCol<T3, ?> third, String thirdAlias,
			TypedCol<T4, ?> fourth, String fourthAlias,
			TypedCol<T5, ?> fifth, String fifthAlias,
			TypedCol<T6, ?> sixth, String sixthAlias,
			TypedCol<T7, ?> seventh, String seventhAlias,
			TypedCol<T8, ?> eighth, String eighthAlias,
			TypedCol<T9, ?> ninth, String ninthAlias,
			TypedCol<T10, ?> tenth, String tenthAlias,
			TypedCol<T11, ?> eleventh, String eleventhAlias,
			TypedCol<T12, ?> twelfth, String twelfthAlias,
			TypedCol<T13, ?> thirteenth, String thirteenthAlias,
			TypedCol<T14, ?> fourteenth, String fourteenthAlias,
			TypedCol<T15, ?> fifteenth, String fifteenthAlias,
			TypedCol<T16, ?> sixteenth, String sixteenthAlias,
			TypedCol<T17, ?> seventeenth, String seventeenthAlias,
			TypedCol<T18, ?> eighteenth, String eighteenthAlias,
			TypedCol<T19, ?> nineteenth, String nineteenthAlias,
			TypedCol<T20, ?> twentieth, String twentiethAlias) {
		return new SelectStep<>(new Q<Object>().addSelect(
				Expr.typedCol(first).ㅤAS(firstAlias),
				Expr.typedCol(second).ㅤAS(secondAlias),
				Expr.typedCol(third).ㅤAS(thirdAlias),
				Expr.typedCol(fourth).ㅤAS(fourthAlias),
				Expr.typedCol(fifth).ㅤAS(fifthAlias),
				Expr.typedCol(sixth).ㅤAS(sixthAlias),
				Expr.typedCol(seventh).ㅤAS(seventhAlias),
				Expr.typedCol(eighth).ㅤAS(eighthAlias),
				Expr.typedCol(ninth).ㅤAS(ninthAlias),
				Expr.typedCol(tenth).ㅤAS(tenthAlias),
				Expr.typedCol(eleventh).ㅤAS(eleventhAlias),
				Expr.typedCol(twelfth).ㅤAS(twelfthAlias),
				Expr.typedCol(thirteenth).ㅤAS(thirteenthAlias),
				Expr.typedCol(fourteenth).ㅤAS(fourteenthAlias),
				Expr.typedCol(fifteenth).ㅤAS(fifteenthAlias),
				Expr.typedCol(sixteenth).ㅤAS(sixteenthAlias),
				Expr.typedCol(seventeenth).ㅤAS(seventeenthAlias),
				Expr.typedCol(eighteenth).ㅤAS(eighteenthAlias),
				Expr.typedCol(nineteenth).ㅤAS(nineteenthAlias),
				Expr.typedCol(twentieth).ㅤAS(twentiethAlias)));
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
	 * Starts a {@code SELECT} of a single {@code cast(...)} column whose Java result type is exactly
	 * the {@link CastStep#ㅤASㅤ(Class) AS} target type — the type is threaded through to the
	 * resulting {@link Q}, so {@link Q#via(jakarta.persistence.EntityManager)} needs no {@code Class}
	 * argument, even though (unlike {@link #SELECTㅤ(ScalarExpr)}) the query may return any number of
	 * rows.
	 *
	 * <p>
	 * Example: {@code SELECT(CAST(Order::total).AS(String.class)).FROM(Order.class)} &rarr; a
	 * {@code Q<String>}.
	 * </p>
	 *
	 * @param cast the cast column, e.g. {@code CAST(Order::total).AS(String.class)}; must not be
	 *             {@code null}
	 * @param <T>  the Java type of the cast column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <T> SelectStep<T> SELECTㅤ(CastExpr<T> cast) {
		return new SelectStep<>(new Q<>(cast.type).addSelect(cast));
	}

	/**
	 * Starts a {@code SELECT} of a single scalar/aggregate whose Java result type
	 * is statically known (e.g. {@link #COUNTㅤꁘ()}) — the type is threaded through
	 * to the resulting {@link ScalarQ}, whose {@link ScalarQ#via(jakarta.persistence.EntityManager)}
	 * returns that single value directly (not a list), since a bare aggregate
	 * always yields exactly one row.
	 *
	 * <p>
	 * Example: {@code SELECT(COUNTㅤꁘ()).FROM(Order.class)} &rarr; a
	 * {@code ScalarQ<Long>}.
	 * </p>
	 *
	 * @param expr the scalar expression, e.g. {@code COUNTㅤꁘ()}; must not be
	 *             {@code null}
	 * @param <T>  the Java type of the scalar
	 * @return the {@code SELECT} phase, which requires a
	 *         {@link ScalarSelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public static <T> ScalarSelectStep<T> SELECTㅤ(ScalarExpr<T> expr) {
		return new ScalarSelectStep<>(new Q<>(expr.type).addSelect(expr), expr.type);
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

	// ===== column / value helpers (package-private: no public wrapper function remains; every
	// public entry point that needs one of these builds it internally — see the individual
	// (alias, ...)-qualified overloads on Q/WhereStep/Cond/Linq's aggregate and predicate builders) =====

	/**
	 * A type-safe column reference, resolved to {@code alias.property} using the alias declared for
	 * the column's entity in the surrounding query — package-private helper shared across this class,
	 * used wherever a bare getter needs turning into an {@link Expr} (e.g. to chain {@code .AS(...)}
	 * or arithmetic onto it).
	 *
	 * @param col the entity getter, e.g. {@code User::name}; must be a method reference and not
	 *            {@code null}
	 * @param <T> the entity type owning the getter
	 * @return the column as an {@link Expr}
	 */
	static <T> Expr typedCol(TypedCol<T, ?> col) {
		return Expr.typedCol(col);
	}

	/**
	 * A column referenced by raw name — for derived/CTE/aliased columns that have
	 * no entity getter.
	 *
	 * <p>
	 * Example: {@code typedCol("orderCount")} &rarr; {@code orderCount}.
	 * </p>
	 *
	 * @param derivedColumn the literal column text emitted verbatim into the HQL;
	 *                      must not be {@code null}, e.g. {@code "orderCount"}
	 * @return the column as an {@link Expr}
	 */
	static Expr typedCol(String derivedColumn) {
		return Expr.of(ctx -> derivedColumn);
	}

	/**
	 * A column qualified with an explicit alias and a raw field name — for
	 * CTE/derived columns that have no entity getter.
	 *
	 * <p>
	 * Example: {@code typedCol("a", "name")} &rarr; {@code a.name}.
	 * </p>
	 *
	 * @param alias the range-variable alias; must not be {@code null}, e.g.
	 *              {@code "a"}
	 * @param field the field name; must not be {@code null}, e.g. {@code "name"}
	 * @return the aliased column as an {@link Expr}
	 */
	static Expr typedCol(String alias, String field) {
		return Expr.of(ctx -> alias + "." + field);
	}

	/**
	 * A type-safe column qualified with an explicit table alias — useful when the
	 * entity is not uniquely resolvable in the current scope (self-joins,
	 * correlated sub-queries, path joins).
	 *
	 * <p>
	 * Example: {@code typedCol("o", Order::customerId)} &rarr; {@code o.customerId}.
	 * </p>
	 *
	 * @param alias the range-variable alias to qualify the column with; must not be
	 *              {@code null}, e.g. {@code "o"}
	 * @param col   the entity getter, e.g. {@code Order::customerId}; must be a
	 *              method reference and not {@code null}
	 * @param <T>   the entity type owning the getter
	 * @return the aliased column as an {@link Expr}
	 */
	static <T> Expr typedCol(String alias, TypedCol<T, ?> col) {
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
		String name = type.getName();
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
	 * {@code sub(SELECT(COUNT(typedCol(Order::id))).FROM(Order.class)).AS("orderCount")}
	 * &rarr; {@code (select count(o.id) from Order o) as orderCount}.
	 * </p>
	 *
	 * @param subquery the nested query; must not be {@code null}
	 * @return the sub-query as an {@link Expr}
	 */
	public static Expr sub(Q subquery) {
		return Expr.of(ctx -> "(" + subquery.getUnsafeHql() + ")");
	}

	/**
	 * Like {@link #sub(Q)}, for a sub-query whose definition ends in a {@code group by} — see
	 * {@link Q#GROUPㅤBY(Object...)} for why that returns {@link Grouped} rather than {@code Q<?>}.
	 *
	 * @param subquery the nested query; must not be {@code null}
	 * @return the sub-query as an {@link Expr}
	 */
	public static Expr sub(Grouped<?> subquery) {
		return Expr.of(ctx -> "(" + subquery.getUnsafeHql() + ")");
	}

	/**
	 * Addition ({@code +}) of two bare getter references, e.g. {@code 十(Order::total, Order::discount)}
	 * &rarr; {@code o.total + o.discount} — for starting an arithmetic expression from two columns
	 * without an {@link Expr} receiver to chain {@link Expr#十(Object)} onto.
	 *
	 * @param left  the left column getter (method reference); must not be {@code null}
	 * @param right the right column getter (method reference); must not be {@code null}
	 * @param <T>   the entity type owning the left column
	 * @param <R>   the entity type owning the right column
	 * @return the sum as an {@link Expr}
	 */
	public static <T, R> Expr 十(TypedCol<T, ?> left, TypedCol<R, ?> right) {
		return Expr.typedCol(left).十(right);
	}

	/**
	 * Addition ({@code +}) starting from a column referenced by raw alias and field name, e.g.
	 * {@code 十("h", "depth", 1)} &rarr; {@code h.depth + 1} — for a derived/CTE column (typically a
	 * recursive CTE's own result, referenced arithmetically in its recursive step) that has no
	 * entity getter.
	 *
	 * @param alias the range-variable alias qualifying the left column; must not be {@code null}
	 * @param field the left column's field name; must not be {@code null}
	 * @param right the right operand (Expr/literal); must not be {@code null}
	 * @return the sum as an {@link Expr}
	 */
	public static Expr 十(String alias, String field, Object right) {
		return typedCol(alias, field).十(right);
	}

	// ===== aggregate / scalar functions =====

	/**
	 * The {@code count(*)} aggregate — counts all rows of the query, regardless of
	 * column nulls. Always yields a {@code long}, so the returned expression
	 * carries {@code Long.class} as its Java result type: selecting it bare (not
	 * chained into a comparison) via {@link #SELECTㅤ(ScalarExpr)} threads
	 * {@code Long} through to a cast-free {@link ScalarQ#via(jakarta.persistence.EntityManager)}
	 * that returns the count directly, not wrapped in a list.
	 *
	 * <p>
	 * Example: {@code SELECT(COUNTㅤꁘ()).FROM(Order.class)} &rarr;
	 * {@code select count(*) from Order} as a {@code ScalarQ<Long>}; or chained
	 * into a comparison, e.g. {@code SELECT(COUNTㅤꁘ().ㅤᆖㅤ(0)).FROM(Order.class)}
	 * &rarr; {@code select count(*) = 0 from Order} as a {@code ScalarQ<Boolean>}
	 * (see {@link ScalarExpr} for how comparisons re-thread the type to
	 * {@code Boolean}).
	 * </p>
	 *
	 * @return the aggregate as a {@link ScalarExpr} typed {@code Long}
	 */
	public static ScalarExpr<Long> COUNTㅤꁘ() {
		return new ScalarExpr<>(Long.class, Expr.of(ctx -> "count(*)"));
	}

	/**
	 * The {@code count(...)} aggregate.
	 *
	 * <p>
	 * Example: {@code COUNT(typedCol(Order::id))} &rarr; {@code count(o.id)}.
	 * </p>
	 *
	 * @param arg the counted expression (e.g. {@link #typedCol(TypedCol)}); must not be
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
	 * Example: {@code SUM(typedCol(Order::total))} &rarr; {@code sum(o.total)}.
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
	 * Example: {@code AVG(typedCol(Order::discount))} &rarr; {@code avg(o.discount)}.
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
	 * Example: {@code MAX(typedCol(Order::total))} &rarr; {@code max(o.total)}.
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
	 * Example: {@code MIN(typedCol(Order::total))} &rarr; {@code min(o.total)}.
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
	 * Example: {@code SIZE(typedCol(Customer::orders))} &rarr; {@code size(c.orders)}.
	 * </p>
	 *
	 * @param arg the collection-valued expression; must not be {@code null}
	 * @return the function call as an {@link Expr}
	 */
	public static Expr SIZE(Object arg) {
		return fn("size", arg);
	}

	/**
	 * The {@code lower(...)} function (lowercases a string expression).
	 *
	 * <p>
	 * Example: {@code LOWER(typedCol(Order::status))} &rarr; {@code lower(o.status)}.
	 * </p>
	 *
	 * @param arg the string-valued expression; must not be {@code null}
	 * @return the function call as an {@link Expr}
	 */
	public static Expr LOWER(Object arg) {
		return fn("lower", arg);
	}

	/**
	 * The {@code upper(...)} function (uppercases a string expression).
	 *
	 * <p>
	 * Example: {@code UPPER(typedCol(Order::status))} &rarr; {@code upper(o.status)}.
	 * </p>
	 *
	 * @param arg the string-valued expression; must not be {@code null}
	 * @return the function call as an {@link Expr}
	 */
	public static Expr UPPER(Object arg) {
		return fn("upper", arg);
	}

	/**
	 * The {@code coalesce(...)} function returning its first non-null argument.
	 *
	 * <p>
	 * Example: {@code COALESCE(AVG(typedCol(Order::discount)), 0)} &rarr;
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
	 * The {@code concat(...)} function, joining its arguments into a single string.
	 *
	 * <p>
	 * Example: {@code CONCAT(typedCol(Order::status), " - ", typedCol(Order::id))} &rarr;
	 * {@code concat(o.status, ' - ', o.id)}.
	 * </p>
	 *
	 * @param args the concatenated expressions/literals, in order; must not be
	 *             {@code null}, should contain at least two non-{@code null}
	 *             elements
	 * @return the function call as an {@link Expr}
	 */
	public static Expr CONCAT(Object... args) {
		return Expr.of(ctx -> "concat(" + Expr.list(ctx, args) + ")");
	}

	/**
	 * {@code concat(column, ...)} whose first argument is a bare getter reference,
	 * e.g. {@code CONCAT(Order::status, " - ", Order::id)} &rarr;
	 * {@code concat(o.status, ' - ', o.id)} — avoids wrapping the leading column in
	 * {@link #typedCol(TypedCol)}.
	 *
	 * @param first the first column getter (method reference); must not be
	 *              {@code null}
	 * @param rest  the remaining concatenated expressions/literals, in order; must
	 *              not be {@code null}, may be empty
	 * @param <T>   the entity type owning the first column
	 * @return the function call as an {@link Expr}
	 */
	public static <T> Expr CONCAT(TypedCol<T, ?> first, Object... rest) {
		Object[] args = new Object[rest.length + 1];
		args[0] = Expr.typedCol(first);
		System.arraycopy(rest, 0, args, 1, rest.length);
		return CONCAT(args);
	}

	/**
	 * {@code concat(column, ..., column, ...)} whose first <em>and</em> third arguments are bare
	 * getter references, e.g. {@code CONCAT(Order::status, "-", Order::id)} &rarr;
	 * {@code concat(o.status, '-', o.id)} — see {@link #CONCAT(TypedCol, Object...)}.
	 *
	 * @param first  the first column getter (method reference); must not be {@code null}
	 * @param second the second concatenated expression/literal; must not be {@code null}
	 * @param third  the third column getter (method reference); must not be {@code null}
	 * @param rest   the remaining concatenated expressions/literals, in order; must not be
	 *               {@code null}, may be empty
	 * @param <T>    the entity type owning the first column
	 * @param <R>    the entity type owning the third column
	 * @return the function call as an {@link Expr}
	 */
	public static <T, R> Expr CONCAT(TypedCol<T, ?> first, Object second, TypedCol<R, ?> third, Object... rest) {
		Object[] args = new Object[rest.length + 3];
		args[0] = Expr.typedCol(first);
		args[1] = second;
		args[2] = Expr.typedCol(third);
		System.arraycopy(rest, 0, args, 3, rest.length);
		return CONCAT(args);
	}

	/**
	 * The {@code nullif(a, b)} function (returns {@code null} when {@code a == b},
	 * else {@code a}).
	 *
	 * <p>
	 * Example: {@code NULLIF(typedCol(Order::discount), 0)} &rarr;
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

	/**
	 * Starts the {@code cast(expr as targetType)} function; follow with {@link CastStep#ㅤASㅤ(Class)}
	 * to supply the target type and finish it — {@code CAST(expr, targetType)} in one call is no
	 * longer possible.
	 *
	 * <p>
	 * Example: {@code CAST(typedCol(Order::total)).AS(String.class)} &rarr;
	 * {@code cast(o.total as String)}.
	 * </p>
	 *
	 * @param expr the cast expression; must not be {@code null}
	 * @return the {@code CAST} phase, which requires {@link CastStep#ㅤASㅤ(Class) AS} next
	 */
	public static CastStep ㅤCASTㅤ(Object expr) {
		return new CastStep(Expr.val(expr));
	}

	// --- TypedCol overloads: take a bare getter reference directly, e.g.
	// COUNT(Order::id) instead of COUNT(typedCol(Order::id)) ---

	/**
	 * {@code count(column)}, e.g. {@code COUNT(Order::id)} &rarr;
	 * {@code count(o.id)}.
	 *
	 * @param col the column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return the aggregate as an {@link Expr}
	 */
	public static <T> Expr COUNT(TypedCol<T, ?> col) {
		return fn("count", Expr.typedCol(col));
	}

	/**
	 * {@code count(alias.column)}, for a column qualified with an explicit alias — see
	 * {@link Q#JOIN(String, TypedCol)} for when this is needed.
	 *
	 * @param alias the range-variable alias to qualify the column with; must not be {@code null}
	 * @param col   the column getter (method reference); must not be {@code null}
	 * @param <T>   the entity type owning the column
	 * @return the aggregate as an {@link Expr}
	 */
	public static <T> Expr COUNT(String alias, TypedCol<T, ?> col) {
		return fn("count", typedCol(alias, col));
	}

	/**
	 * {@code sum(column)}, e.g. {@code SUM(Order::total)} &rarr;
	 * {@code sum(o.total)}.
	 *
	 * @param col the column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return the aggregate as an {@link Expr}
	 */
	public static <T> Expr SUM(TypedCol<T, ?> col) {
		return fn("sum", Expr.typedCol(col));
	}

	/**
	 * {@code sum(alias.column)}, for a column qualified with an explicit alias.
	 *
	 * @param alias the range-variable alias to qualify the column with; must not be {@code null}
	 * @param col   the column getter (method reference); must not be {@code null}
	 * @param <T>   the entity type owning the column
	 * @return the aggregate as an {@link Expr}
	 */
	public static <T> Expr SUM(String alias, TypedCol<T, ?> col) {
		return fn("sum", typedCol(alias, col));
	}

	/**
	 * {@code avg(column)}, e.g. {@code AVG(Order::discount)} &rarr;
	 * {@code avg(o.discount)}.
	 *
	 * @param col the column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return the aggregate as an {@link Expr}
	 */
	public static <T> Expr AVG(TypedCol<T, ?> col) {
		return fn("avg", Expr.typedCol(col));
	}

	/**
	 * {@code avg(alias.column)}, for a column qualified with an explicit alias.
	 *
	 * @param alias the range-variable alias to qualify the column with; must not be {@code null}
	 * @param col   the column getter (method reference); must not be {@code null}
	 * @param <T>   the entity type owning the column
	 * @return the aggregate as an {@link Expr}
	 */
	public static <T> Expr AVG(String alias, TypedCol<T, ?> col) {
		return fn("avg", typedCol(alias, col));
	}

	/**
	 * {@code max(column)}, e.g. {@code MAX(Order::total)} &rarr;
	 * {@code max(o.total)}.
	 *
	 * @param col the column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return the aggregate as an {@link Expr}
	 */
	public static <T> Expr MAX(TypedCol<T, ?> col) {
		return fn("max", Expr.typedCol(col));
	}

	/**
	 * {@code max(alias.column)}, for a column qualified with an explicit alias.
	 *
	 * @param alias the range-variable alias to qualify the column with; must not be {@code null}
	 * @param col   the column getter (method reference); must not be {@code null}
	 * @param <T>   the entity type owning the column
	 * @return the aggregate as an {@link Expr}
	 */
	public static <T> Expr MAX(String alias, TypedCol<T, ?> col) {
		return fn("max", typedCol(alias, col));
	}

	/**
	 * {@code min(column)}, e.g. {@code MIN(Order::total)} &rarr;
	 * {@code min(o.total)}.
	 *
	 * @param col the column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return the aggregate as an {@link Expr}
	 */
	public static <T> Expr MIN(TypedCol<T, ?> col) {
		return fn("min", Expr.typedCol(col));
	}

	/**
	 * {@code min(alias.column)}, for a column qualified with an explicit alias.
	 *
	 * @param alias the range-variable alias to qualify the column with; must not be {@code null}
	 * @param col   the column getter (method reference); must not be {@code null}
	 * @param <T>   the entity type owning the column
	 * @return the aggregate as an {@link Expr}
	 */
	public static <T> Expr MIN(String alias, TypedCol<T, ?> col) {
		return fn("min", typedCol(alias, col));
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
	public static <T> Expr SIZE(TypedCol<T, ?> col) {
		return fn("size", Expr.typedCol(col));
	}

	/**
	 * {@code size(alias.column)}, for a collection-valued column qualified with an explicit alias.
	 *
	 * @param alias the range-variable alias to qualify the column with; must not be {@code null}
	 * @param col   the collection-valued column getter (method reference); must not be {@code null}
	 * @param <T>   the entity type owning the column
	 * @return the function call as an {@link Expr}
	 */
	public static <T> Expr SIZE(String alias, TypedCol<T, ?> col) {
		return fn("size", typedCol(alias, col));
	}

	/**
	 * {@code lower(column)}, e.g. {@code LOWER(Order::status)} &rarr;
	 * {@code lower(o.status)}.
	 *
	 * @param col the column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return the function call as an {@link Expr}
	 */
	public static <T> Expr LOWER(TypedCol<T, ?> col) {
		return fn("lower", Expr.typedCol(col));
	}

	/**
	 * {@code lower(alias.column)}, for a column qualified with an explicit alias — e.g. the two sides
	 * of a self-join comparison, {@code LOWER("a", X::name)} vs. {@code LOWER("b", X::name)}.
	 *
	 * @param alias the range-variable alias to qualify the column with; must not be {@code null}
	 * @param col   the column getter (method reference); must not be {@code null}
	 * @param <T>   the entity type owning the column
	 * @return the function call as an {@link Expr}
	 */
	public static <T> Expr LOWER(String alias, TypedCol<T, ?> col) {
		return fn("lower", typedCol(alias, col));
	}

	/**
	 * {@code upper(column)}, e.g. {@code UPPER(Order::status)} &rarr;
	 * {@code upper(o.status)}.
	 *
	 * @param col the column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return the function call as an {@link Expr}
	 */
	public static <T> Expr UPPER(TypedCol<T, ?> col) {
		return fn("upper", Expr.typedCol(col));
	}

	/**
	 * {@code upper(alias.column)}, for a column qualified with an explicit alias.
	 *
	 * @param alias the range-variable alias to qualify the column with; must not be {@code null}
	 * @param col   the column getter (method reference); must not be {@code null}
	 * @param <T>   the entity type owning the column
	 * @return the function call as an {@link Expr}
	 */
	public static <T> Expr UPPER(String alias, TypedCol<T, ?> col) {
		return fn("upper", typedCol(alias, col));
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
	public static <T> Expr NULLIF(TypedCol<T, ?> a, Object b) {
		return NULLIF(Expr.typedCol(a), b);
	}

	/**
	 * {@code nullif(columnA, columnB)}, e.g. {@code NULLIF(Order::a, Order::b)} &rarr;
	 * {@code nullif(o.a, o.b)} — no {@link #typedCol(TypedCol) typedCol(...)} wrapping needed for either operand.
	 *
	 * @param a   the first column getter (method reference); must not be {@code null}
	 * @param b   the second column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the first column
	 * @param <R> the entity type owning the second column
	 * @return the function call as an {@link Expr}
	 */
	public static <T, R> Expr NULLIF(TypedCol<T, ?> a, TypedCol<R, ?> b) {
		return NULLIF(Expr.typedCol(a), Expr.typedCol(b));
	}

	/**
	 * Starts {@code cast(column as targetType)} from a bare getter reference, e.g.
	 * {@code CAST(Order::total).AS(String.class)} &rarr; {@code cast(o.total as String)} — follow
	 * with {@link CastStep#ㅤASㅤ(Class)} to supply the target type.
	 *
	 * @param col the column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return the {@code CAST} phase, which requires {@link CastStep#ㅤASㅤ(Class) AS} next
	 */
	public static <T> CastStep ㅤCASTㅤ(TypedCol<T, ?> col) {
		return new CastStep(Expr.typedCol(col));
	}

	/**
	 * The {@code row_number()} window function.
	 *
	 * <p>
	 * Example: {@code ROW_NUMBER().OVER(PARTITIONㅤBY(typedCol(Order::customerId)))}
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
	 * {@code RANK().OVER(PARTITIONㅤBY(typedCol(Order::customerId)).ORDERㅤBY(Order::total).DESC())}.
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
	 * Example: {@code SELECT(DISTINCT(typedCol(Order::customerId)))} &rarr;
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

	/**
	 * A {@code distinct} projection modifier for a single alias-qualified column —
	 * shorthand for {@code DISTINCT(typedCol(alias, col))}.
	 *
	 * <p>
	 * Example: {@code DISTINCT("d", Order::customerId)} &rarr;
	 * {@code distinct d.customerId}.
	 * </p>
	 *
	 * @param alias the range-variable alias to qualify the column with; must not be
	 *              {@code null}, e.g. {@code "d"}
	 * @param col   the column getter (method reference); must not be {@code null}
	 * @param <T>   the entity type owning the column
	 * @return the modified projection as an {@link Expr}
	 */
	public static <T> Expr DISTINCT(String alias, TypedCol<T, ?> col) {
		return DISTINCT(typedCol(alias, col));
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
	 * {@code NEW(CustomerSummary.class, Customer::id, COUNT(typedCol("o", Order::id)))}
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
	 * {@code NEW(CustomerSummary.class, Customer::id, typedCol(Customer::name))} —
	 * avoids wrapping the leading argument in {@link #typedCol(TypedCol)}.
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
	public static <T> Expr NEW(Class<?> dto, TypedCol<T, ?> first, Object... rest) {
		Object[] args = new Object[rest.length + 1];
		args[0] = Expr.typedCol(first);
		System.arraycopy(rest, 0, args, 1, rest.length);
		return Expr.of(ctx -> "new " + dto.getName() + "(" + Expr.list(ctx, args) + ")");
	}

	/**
	 * A constructor (DTO) projection whose first <em>and</em> second arguments are bare getter
	 * references, e.g. {@code NEW(CustomerSummary.class, Customer::id, Customer::name)} — see
	 * {@link #NEW(Class, TypedCol, Object...)}.
	 *
	 * @param dto    the DTO class whose constructor is invoked; must not be
	 *               {@code null}. The fully-qualified name ({@link Class#getName()})
	 *               is emitted.
	 * @param first  the first constructor argument (method reference); must not be {@code null}
	 * @param second the second constructor argument (method reference); must not be {@code null}
	 * @param rest   the remaining constructor arguments, in order; must not be {@code null}, may be
	 *               empty
	 * @param <T>    the entity type owning the first argument's column
	 * @param <R>    the entity type owning the second argument's column
	 * @return the constructor projection as an {@link Expr}
	 */
	public static <T, R> Expr NEW(Class<?> dto, TypedCol<T, ?> first, TypedCol<R, ?> second, Object... rest) {
		Object[] args = new Object[rest.length + 2];
		args[0] = Expr.typedCol(first);
		args[1] = Expr.typedCol(second);
		System.arraycopy(rest, 0, args, 2, rest.length);
		return Expr.of(ctx -> "new " + dto.getName() + "(" + Expr.list(ctx, args) + ")");
	}

	/**
	 * A {@code treat(expr as Subtype)} down-cast for polymorphic associations;
	 * follow with {@link Expr#ᐧ(TypedCol)} to access a subtype field.
	 *
	 * <p>
	 * Example:
	 * {@code TREAT(expr, CreditCardPayment.class).ᐧ(CreditCardPayment::cardType)}
	 * &rarr; {@code treat(... as CreditCardPayment).cardType}.
	 * </p>
	 *
	 * @param expr the expression to cast; must not be {@code null}
	 * @param type the target subtype; must not be {@code null}. Its fully-qualified name is
	 *             emitted.
	 * @return the cast as an {@link Expr}
	 */
	public static Expr ㅤTREATㅤ(Object expr, Class<?> type) {
		Expr e = Expr.val(expr);
		return Expr.of(ctx -> "treat(" + e.render(ctx) + " as " + type.getName() + ")");
	}

	/**
	 * A {@code treat(rootEntity as Subtype)} down-cast — shorthand for
	 * {@code TREAT(entity(rootType), subtype)}; follow with {@link Expr#ᐧ(TypedCol)} to
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
	 * @param subtype  the target subtype; must not be {@code null}. Its fully-qualified name
	 *                 is emitted.
	 * @return the cast as an {@link Expr}
	 */
	public static Expr ㅤTREATㅤ(Class<?> rootType, Class<?> subtype) {
		return ㅤTREATㅤ(entity(rootType), subtype);
	}

	// ===== window helper =====

	/**
	 * The {@code partition by ...} clause of a window; chain
	 * {@link Expr#ORDERㅤBY(TypedCol)} and {@link Expr#DESC()} for ordering.
	 *
	 * <p>
	 * Example:
	 * {@code PARTITIONㅤBY(typedCol(Order::customerId)).ORDERㅤBY(Order::total).DESC()}
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
	public static <T> Expr ㅤPARTITIONㅤBYㅤ(TypedCol<T, ?>... cols) {
		Object[] exprs = new Object[cols.length];
		for (int i = 0; i < cols.length; i++) {
			exprs[i] = Expr.typedCol(cols[i]);
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
	 *            {@link Cond#ISㅤNULL(TypedCol)} — passing {@code null} here renders the
	 *            literal text {@code null}.
	 * @param <T> the entity type owning the left column
	 * @return a leaf predicate; combine with {@link #ㅤANDㅤ(Cond...)} /
	 *         {@link #ㅤORㅤ(Cond...)}
	 */
	public static <T> Cond ㅤᆖㅤ(TypedCol<T, ?> l, Object r) {
		return new Cond().ᆖ(l, r);
	} // =

	/**
	 * Equality predicate ({@code =}) with bare columns on both sides, e.g.
	 * {@code ㅤᆖㅤ(Order::a, Order::b)} &rarr; {@code a = b} — no {@link #typedCol(TypedCol) typedCol(...)} wrapping
	 * needed for the right operand.
	 *
	 * @param l   the left column getter (method reference); must not be {@code null}
	 * @param r   the right column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @param <R> the entity type owning the right column
	 * @return a leaf predicate
	 */
	public static <T, R> Cond ㅤᆖㅤ(TypedCol<T, ?> l, TypedCol<R, ?> r) {
		return new Cond().ᆖ(l, r);
	}

	/**
	 * Equality predicate ({@code =}) with both columns qualified by an explicit alias — for comparing
	 * two rows of the <em>same</em> entity in a self-join, e.g.
	 * {@code ㅤᆖㅤ("a", X::name, "b", X::name)} &rarr; {@code a.name = b.name}.
	 *
	 * @param aliasL the range-variable alias qualifying the left column; must not be {@code null}
	 * @param l      the left column getter (method reference); must not be {@code null}
	 * @param aliasR the range-variable alias qualifying the right column; must not be {@code null}
	 * @param r      the right column getter (method reference); must not be {@code null}
	 * @param <T>    the entity type owning the left column
	 * @param <R>    the entity type owning the right column
	 * @return a leaf predicate
	 */
	public static <T, R> Cond ㅤᆖㅤ(String aliasL, TypedCol<T, ?> l, String aliasR, TypedCol<R, ?> r) {
		return new Cond().ᆖ(typedCol(aliasL, l), typedCol(aliasR, r));
	}

	/**
	 * Equality predicate ({@code =}) with an expression left operand.
	 *
	 * @param l the left operand (e.g. an aggregate {@link Expr}); must not be
	 *          {@code null}
	 * @param r the right operand (see {@link #ㅤᆖㅤ(TypedCol, Object)}); {@code null}
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
	public static <T> Cond ㅤᐸㅤ(TypedCol<T, ?> l, Object r) {
		return new Cond().ᐸ(l, r);
	} // <

	/**
	 * Less-than predicate ({@code <}) with bare columns on both sides — see {@link #ㅤᆖㅤ(TypedCol, TypedCol)}.
	 *
	 * @param l   the left column getter (method reference); must not be {@code null}
	 * @param r   the right column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @param <R> the entity type owning the right column
	 * @return a leaf predicate
	 */
	public static <T, R> Cond ㅤᐸㅤ(TypedCol<T, ?> l, TypedCol<R, ?> r) {
		return new Cond().ᐸ(l, r);
	}

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
	public static <T> Cond ㅤᐳㅤ(TypedCol<T, ?> l, Object r) {
		return new Cond().ᐳ(l, r);
	} // >

	/**
	 * Greater-than predicate ({@code >}) with bare columns on both sides — see {@link #ㅤᆖㅤ(TypedCol, TypedCol)}.
	 *
	 * @param l   the left column getter (method reference); must not be {@code null}
	 * @param r   the right column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @param <R> the entity type owning the right column
	 * @return a leaf predicate
	 */
	public static <T, R> Cond ㅤᐳㅤ(TypedCol<T, ?> l, TypedCol<R, ?> r) {
		return new Cond().ᐳ(l, r);
	}

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
	public static <T> Cond ㅤᐸᆖㅤ(TypedCol<T, ?> l, Object r) {
		return new Cond().ᐸᆖ(l, r);
	} // <=

	/**
	 * Less-than-or-equal predicate ({@code <=}) with bare columns on both sides — see
	 * {@link #ㅤᆖㅤ(TypedCol, TypedCol)}.
	 *
	 * @param l   the left column getter (method reference); must not be {@code null}
	 * @param r   the right column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @param <R> the entity type owning the right column
	 * @return a leaf predicate
	 */
	public static <T, R> Cond ㅤᐸᆖㅤ(TypedCol<T, ?> l, TypedCol<R, ?> r) {
		return new Cond().ᐸᆖ(l, r);
	}

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
	public static <T> Cond ㅤᐳᆖㅤ(TypedCol<T, ?> l, Object r) {
		return new Cond().ᐳᆖ(l, r);
	} // >=

	/**
	 * Greater-than-or-equal predicate ({@code >=}) with bare columns on both sides — see
	 * {@link #ㅤᆖㅤ(TypedCol, TypedCol)}.
	 *
	 * @param l   the left column getter (method reference); must not be {@code null}
	 * @param r   the right column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @param <R> the entity type owning the right column
	 * @return a leaf predicate
	 */
	public static <T, R> Cond ㅤᐳᆖㅤ(TypedCol<T, ?> l, TypedCol<R, ?> r) {
		return new Cond().ᐳᆖ(l, r);
	}

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
	public static <T> Cond ㅤᐸᐳㅤ(TypedCol<T, ?> l, Object r) {
		return new Cond().ᐸᐳ(l, r);
	} // <>

	/**
	 * Not-equal predicate ({@code <>}) with bare columns on both sides — see {@link #ㅤᆖㅤ(TypedCol, TypedCol)}.
	 *
	 * @param l   the left column getter (method reference); must not be {@code null}
	 * @param r   the right column getter (method reference); must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @param <R> the entity type owning the right column
	 * @return a leaf predicate
	 */
	public static <T, R> Cond ㅤᐸᐳㅤ(TypedCol<T, ?> l, TypedCol<R, ?> r) {
		return new Cond().ᐸᐳ(l, r);
	}

	/**
	 * Not-equal predicate ({@code <>}) with both columns qualified by an explicit alias — for
	 * comparing two rows of the <em>same</em> entity in a self-join — see
	 * {@link #ㅤᆖㅤ(String, TypedCol, String, TypedCol)}.
	 *
	 * @param aliasL the range-variable alias qualifying the left column; must not be {@code null}
	 * @param l      the left column getter (method reference); must not be {@code null}
	 * @param aliasR the range-variable alias qualifying the right column; must not be {@code null}
	 * @param r      the right column getter (method reference); must not be {@code null}
	 * @param <T>    the entity type owning the left column
	 * @param <R>    the entity type owning the right column
	 * @return a leaf predicate
	 */
	public static <T, R> Cond ㅤᐸᐳㅤ(String aliasL, TypedCol<T, ?> l, String aliasR, TypedCol<R, ?> r) {
		return new Cond().ᐸᐳ(typedCol(aliasL, l), typedCol(aliasR, r));
	}

	/**
	 * Not-equal predicate ({@code <>}) with an arbitrary left expression (e.g. {@code LOWER(...)})
	 * and a right column qualified by an explicit alias.
	 *
	 * @param l      the left operand (e.g. an {@link Expr}); must not be {@code null}
	 * @param aliasR the range-variable alias qualifying the right column; must not be {@code null}
	 * @param r      the right column getter (method reference); must not be {@code null}
	 * @param <R>    the entity type owning the right column
	 * @return a leaf predicate
	 */
	public static <R> Cond ㅤᐸᐳㅤ(Object l, String aliasR, TypedCol<R, ?> r) {
		return new Cond().ᐸᐳ(l, typedCol(aliasR, r));
	}

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
	public static <T> Cond ㅤINㅤ(TypedCol<T, ?> l, Object r) {
		return new Cond().IN(l, r);
	}

	/**
	 * Membership predicate ({@code in (...)}) with bare columns on both sides — see
	 * {@link #ㅤᆖㅤ(TypedCol, TypedCol)}.
	 *
	 * @param l   the left column getter (method reference); must not be {@code null}
	 * @param r   the right column getter (typically collection-valued); must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @param <R> the entity type owning the right column
	 * @return a leaf predicate
	 */
	public static <T, R> Cond ㅤINㅤ(TypedCol<T, ?> l, TypedCol<R, ?> r) {
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
	public static <T> Cond ㅤLIKEㅤ(TypedCol<T, ?> l, Object r) {
		return new Cond().LIKE(l, r);
	}

	/**
	 * Pattern-match predicate ({@code like}) with bare columns on both sides — see
	 * {@link #ㅤᆖㅤ(TypedCol, TypedCol)}.
	 *
	 * @param l   the left column getter (method reference); must not be {@code null}
	 * @param r   the right column getter (the pattern); must not be {@code null}
	 * @param <T> the entity type owning the left column
	 * @param <R> the entity type owning the right column
	 * @return a leaf predicate
	 */
	public static <T, R> Cond ㅤLIKEㅤ(TypedCol<T, ?> l, TypedCol<R, ?> r) {
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
	public static <T> Cond ㅤISㅤNULLㅤ(TypedCol<T, ?> c) {
		return new Cond().ISㅤNULL(c);
	}

	/**
	 * Not-null-test predicate ({@code is not null}).
	 *
	 * @param c   the column getter; must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return a leaf predicate
	 */
	public static <T> Cond ㅤISㅤNOTㅤNULLㅤ(TypedCol<T, ?> c) {
		return new Cond().ISㅤNOTㅤNULL(c);
	}

	/**
	 * Empty-collection predicate ({@code is empty}).
	 *
	 * @param c   the collection-valued column getter; must not be {@code null}
	 * @param <T> the entity type owning the column
	 * @return a leaf predicate
	 */
	public static <T> Cond ㅤISㅤEMPTYㅤ(TypedCol<T, ?> c) {
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
	public static <T> Cond ㅤISㅤNOTㅤEMPTYㅤ(TypedCol<T, ?> c) {
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
	 * {@code MEMBERㅤOF(param("product"), typedCol(Customer::wishlist))} &rarr;
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
	public static <T> Cond ㅤMEMBERㅤOFㅤ(Object value, TypedCol<T, ?> collection) {
		return new Cond().MEMBERㅤOF(value, Expr.typedCol(collection));
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
