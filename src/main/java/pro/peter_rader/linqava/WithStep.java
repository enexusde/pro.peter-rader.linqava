/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package pro.peter_rader.linqava;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code WITH} phase: add further common table expressions, then start the main query with
 * {@code SELECT}. Only after {@code SELECT} does {@code FROM} become available (via {@link SelectStep}).
 *
 * <p>The main query's entity type isn't known until {@code SELECT} is called, so the accumulated CTEs
 * are held here and only attached to the (then properly typed) {@link Q} once {@code SELECT} runs —
 * this keeps the whole chain, including {@link Q#via}, cast-free.</p>
 */
public final class WithStep {

	private static final class PendingCte {
		final String name;
		final Q<?> definition;
		final boolean recursive;

		PendingCte(String name, Q<?> definition, boolean recursive) {
			this.name = name;
			this.definition = definition;
			this.recursive = recursive;
		}
	}

	private final List<PendingCte> ctes = new ArrayList<>();

	WithStep() {
	}

	/**
	 * Adds another common table expression ({@code , name AS (definition)}).
	 *
	 * @param name       the CTE name; must not be {@code null} or blank
	 * @param definition the defining sub-query; must not be {@code null}
	 * @return this {@code WITH} phase, for chaining
	 */
	public WithStep WITH(String name, Q<?> definition) {
		ctes.add(new PendingCte(name, definition, false));
		return this;
	}

	/**
	 * Adds another common table expression whose definition ends in a {@code group by} — see
	 * {@link Q#GROUPㅤBY(Object...)} for why that returns {@link Grouped} rather than {@code Q<?>}.
	 *
	 * @param name       the CTE name; must not be {@code null} or blank
	 * @param definition the defining sub-query; must not be {@code null}
	 * @return this {@code WITH} phase, for chaining
	 */
	public WithStep WITH(String name, Grouped<?> definition) {
		return WITH(name, definition.unwrap());
	}

	/**
	 * Adds a recursive common table expression and marks the {@code WITH} clause recursive.
	 *
	 * @param name       the CTE name; must not be {@code null} or blank
	 * @param definition the recursive defining sub-query (anchor {@code UNION ALL} step); must not be {@code null}
	 * @return this {@code WITH} phase, for chaining
	 */
	public WithStep WITHㅤRECURSIVE(String name, Q<?> definition) {
		ctes.add(new PendingCte(name, definition, true));
		return this;
	}

	/**
	 * Adds a recursive common table expression whose definition ends in a {@code group by} — see
	 * {@link #WITH(String, Grouped)}.
	 *
	 * @param name       the CTE name; must not be {@code null} or blank
	 * @param definition the recursive defining sub-query (anchor {@code UNION ALL} step); must not be
	 *                   {@code null}
	 * @return this {@code WITH} phase, for chaining
	 */
	public WithStep WITHㅤRECURSIVE(String name, Grouped<?> definition) {
		return WITHㅤRECURSIVE(name, definition.unwrap());
	}

	/**
	 * Starts the main query's projection ({@code select ...}).
	 *
	 * @param cols the projected columns/expressions, in order; must not be {@code null}, may be empty
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public SelectStep<Object> SELECTㅤ(Object... cols) {
		return new SelectStep<>(attachCtes(new Q<Object>()).addSelect(cols));
	}

	/**
	 * Starts the main query's projection with a bare leading getter reference.
	 *
	 * @param first the first column getter (method reference); must not be {@code null}
	 * @param rest  the remaining columns/expressions, in order; must not be {@code null}, may be empty
	 * @param <A>   the entity type owning the first column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public <A> SelectStep<Object> SELECTㅤ(TypedCol<A, ?> first, Object... rest) {
		return new SelectStep<>(attachCtes(new Q<Object>()).addSelect(first, rest));
	}

	/**
	 * Starts the main query's projection with one column referenced by raw alias and field name —
	 * for a derived/CTE column that has no entity getter.
	 *
	 * @param alias the range-variable alias qualifying the column; must not be {@code null}
	 * @param field the field name; must not be {@code null}
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public SelectStep<Object> SELECTㅤ(String alias, String field) {
		return new SelectStep<>(attachCtes(new Q<Object>()).addSelect(Linq.typedCol(alias, field)));
	}

	/**
	 * Starts the main query's projection with two columns referenced by raw alias and field name —
	 * see {@link #SELECTㅤ(String, String)}.
	 *
	 * @param alias1 the range-variable alias qualifying the first column; must not be {@code null}
	 * @param field1 the first field name; must not be {@code null}
	 * @param alias2 the range-variable alias qualifying the second column; must not be {@code null}
	 * @param field2 the second field name; must not be {@code null}
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public SelectStep<Object> SELECTㅤ(String alias1, String field1, String alias2, String field2) {
		return new SelectStep<>(
				attachCtes(new Q<Object>()).addSelect(Linq.typedCol(alias1, field1), Linq.typedCol(alias2, field2)));
	}

	/**
	 * Starts the main query's projection with three columns referenced by raw alias and field name —
	 * see {@link #SELECTㅤ(String, String)}.
	 *
	 * @param alias1 the range-variable alias qualifying the first column; must not be {@code null}
	 * @param field1 the first field name; must not be {@code null}
	 * @param alias2 the range-variable alias qualifying the second column; must not be {@code null}
	 * @param field2 the second field name; must not be {@code null}
	 * @param alias3 the range-variable alias qualifying the third column; must not be {@code null}
	 * @param field3 the third field name; must not be {@code null}
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public SelectStep<Object> SELECTㅤ(String alias1, String field1, String alias2, String field2, String alias3,
			String field3) {
		return new SelectStep<>(attachCtes(new Q<Object>()).addSelect(Linq.typedCol(alias1, field1),
				Linq.typedCol(alias2, field2), Linq.typedCol(alias3, field3)));
	}

	/**
	 * Starts the main query's projection with a bare leading getter reference followed by a raw
	 * alias-qualified field with no getter.
	 *
	 * @param first the first column getter (method reference); must not be {@code null}
	 * @param alias the range-variable alias qualifying the second column; must not be {@code null}
	 * @param field the second column's field name; must not be {@code null}
	 * @param <A>   the entity type owning the first column
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public <A> SelectStep<Object> SELECTㅤ(TypedCol<A, ?> first, String alias, String field) {
		return new SelectStep<>(
				attachCtes(new Q<Object>()).addSelect(Expr.typedCol(first), Linq.typedCol(alias, field)));
	}

	/**
	 * Starts the main query as a whole-entity selection — shorthand for {@code SELECT(entity(type))}.
	 * The selected type is threaded through to the resulting {@link Q}, so {@link Q#via} needs no cast.
	 *
	 * @param entityType the selected entity class; must not be {@code null}
	 * @param <E>        the selected entity type
	 * @return the {@code SELECT} phase, which requires a {@link SelectStep#ㅤFROMㅤ(Class) FROM} next
	 */
	public <E> SelectStep<E> SELECT(Class<E> entityType) {
		return new SelectStep<>(attachCtes(new Q<>(entityType)).addSelect(Linq.entity(entityType)));
	}

	private <E> Q<E> attachCtes(Q<E> q) {
		for (PendingCte c : ctes) {
			q.addCte(c.name, c.definition, c.recursive);
		}
		return q;
	}
}
