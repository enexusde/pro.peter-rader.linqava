/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package pro.peter_rader.linqava.h2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static pro.peter_rader.linqava.Linq.CONCAT;
import static pro.peter_rader.linqava.Linq.COUNT;
import static pro.peter_rader.linqava.Linq.COUNTㅤꁘ;
import static pro.peter_rader.linqava.Linq.LOWER;
import static pro.peter_rader.linqava.Linq.NEW;
import static pro.peter_rader.linqava.Linq.SELECTㅤ;
import static pro.peter_rader.linqava.Linq.SELECTㅤꁘㅤFROM;
import static pro.peter_rader.linqava.Linq.ㅤANDㅤ;
import static pro.peter_rader.linqava.Linq.ㅤCASTㅤ;
import static pro.peter_rader.linqava.Linq.ㅤᆖㅤ;
import static pro.peter_rader.linqava.Linq.ㅤᐸᐳㅤ;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import pro.peter_rader.linqava.EntityQ;
import pro.peter_rader.linqava.Grouped;
import pro.peter_rader.linqava.Q;
import pro.peter_rader.linqava.ScalarQ;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.Persistence;

/**
 * Exercises linqava end-to-end against a <em>real</em> Hibernate {@link EntityManager} backed by an
 * in-memory H2 database and real, persistable {@code @Entity} classes ({@link Customer},
 * {@link Order}) — as opposed to {@code QueryTest}, which only ever asserts the rendered HQL string
 * against a proxied {@code EntityManager} or the non-JPA {@code Model.java} fixtures.
 *
 * <p>
 * This class exists because a rendered-HQL-string assertion can pass while the query is still
 * invalid against a real persistence provider — e.g. a property path derived from an acronym-leading
 * getter (see {@link #findsByAcronymLeadingGetterProperty()}) once silently broke this way, and only
 * surfaced when actually executed against Hibernate.
 * </p>
 */
public class H2IntegrationTest {

	private static EntityManagerFactory emf;
	private EntityManager em;
	private EntityTransaction tx;

	@BeforeClass
	public static void openFactory() {
		emf = Persistence.createEntityManagerFactory("linqava-h2-test");
	}

	@AfterClass
	public static void closeFactory() {
		emf.close();
	}

	@Before
	public void openSession() {
		em = emf.createEntityManager();
		tx = em.getTransaction();
		tx.begin();
	}

	/** Rolls back so every test starts from an empty database, without needing a shared fixture. */
	@After
	public void closeSession() {
		if (tx.isActive()) {
			tx.rollback();
		}
		em.close();
	}

	/**
	 * The (getter, alias) pair overloads of {@code SELECT} build an aliased multi-column projection
	 * without any {@code typedCol(...)}/{@code col(...)} wrapping anywhere in user code.
	 */
	@Test
	public void viaExecutesSelectWithAliasedColumnPairs() {
		Customer c = new Customer().setName("Ada").setCountry("UK");
		em.persist(c);
		em.persist(new Order().setCustomer(c).setStatus("PAID").setTotal(10.0));
		em.flush();

		Q<Object> q = SELECTㅤ(Order::getId, "id", Order::getStatus, "status").ㅤFROMㅤ(Order.class);
		assertEquals("select id as id, status as status from pro.peter_rader.linqava.h2.Order", q.getUnsafeHql());

		List<Object[]> rows = q.via(em, Object[].class);

		assertEquals(1, rows.size());
		assertEquals("PAID", rows.get(0)[1]);
	}

	@Test
	public void viaFindsAllPersistedEntities() {
		em.persist(new Customer().setName("Ada").setCountry("UK"));
		em.persist(new Customer().setName("Grace").setCountry("US"));
		em.flush();

		List<Customer> all = toList(SELECTㅤꁘㅤFROM(Customer.class).via(em));

		assertEquals(2, all.size());
	}

	@Test
	public void whereFiltersToOnlyMatchingRows() {
		em.persist(new Customer().setName("Ada").setCountry("UK"));
		em.persist(new Customer().setName("Grace").setCountry("US"));
		em.flush();

		List<Customer> matches = toList(SELECTㅤꁘㅤFROM(Customer.class).ㅤWHEREㅤ(Customer::getCountry).ㅤᆖㅤ("US").via(em));

		assertEquals(1, matches.size());
		assertEquals("Grace", matches.get(0).getName());
	}

	@Test
	public void firstWithConsumerFallbackPersistsANewRowWhenNoneMatches() {
		Customer found = SELECTㅤꁘㅤFROM(Customer.class).ㅤWHEREㅤ(Customer::getName).ㅤᆖㅤ("Linus")
				.first(em, c -> c.setName("Linus").setCountry("FI"));
		em.flush();

		assertTrue("fallback-created entity must be persisted with a generated id", found.getId() != null);

		List<Customer> reloaded = toList(SELECTㅤꁘㅤFROM(Customer.class).ㅤWHEREㅤ(Customer::getName).ㅤᆖㅤ("Linus").via(em));
		assertEquals(1, reloaded.size());
		assertEquals("FI", reloaded.get(0).getCountry());
	}

	@Test
	public void firstWithNullConsumerReturnsNullInsteadOfCreating() {
		Customer found = SELECTㅤꁘㅤFROM(Customer.class).ㅤWHEREㅤ(Customer::getName).ㅤᆖㅤ("Nobody")
				.first(em, (Consumer<Customer>) null);

		assertNull(found);
	}

	@Test
	public void joinAcrossTheFullTwoStepFormResolvesAnAliasedAssociation() {
		Customer ada = new Customer().setName("Ada").setCountry("UK");
		em.persist(ada);
		em.persist(new Order().setCustomer(ada).setStatus("PAID").setTotal(42.0));
		Customer grace = new Customer().setName("Grace").setCountry("US");
		em.persist(grace);
		em.persist(new Order().setCustomer(grace).setStatus("PAID").setTotal(99.0));
		em.flush();

		List<Order> ukOrders = toList(SELECTㅤ(Order.class).ㅤFROMㅤ(Order.class).ㅤAS("o").JOIN(Order::getCustomer)
				.ㅤAS("c").ㅤWHEREㅤ("c", Customer::getCountry).ㅤᆖㅤ("UK").via(em));

		assertEquals(1, ukOrders.size());
		assertEquals(42.0, ukOrders.get(0).getTotal(), 0.0001);
	}

	/**
	 * Regression test for the {@code Names.property()} acronym-decapitalization saga: confirms, against
	 * a real Hibernate metamodel (not just a re-derivation of what we think the rule is), that a getter
	 * with a leading acronym ({@code getVIPCode}) resolves to the correct HQL property.
	 */
	@Test
	public void findsByAcronymLeadingGetterProperty() {
		em.persist(new Customer().setName("Ada").setCountry("UK").setVIPCode("GOLD-1"));
		em.flush();

		List<Customer> matches = toList(
				SELECTㅤꁘㅤFROM(Customer.class).ㅤWHEREㅤ(Customer::getVIPCode).ㅤᆖㅤ("GOLD-1").via(em));

		assertEquals(1, matches.size());
	}

	@Test
	public void viaParameterizesLiteralsSafelyEvenWithQuoteCharacters() {
		em.persist(new Customer().setName("O'Brien").setCountry("IE"));
		em.flush();

		List<Customer> matches = toList(SELECTㅤꁘㅤFROM(Customer.class).ㅤWHEREㅤ(Customer::getName).ㅤᆖㅤ("O'Brien").via(em));

		assertEquals(1, matches.size());
		assertEquals("O'Brien", matches.get(0).getName());
	}

	@Test
	public void orderByOrdersRealResultsAscending() {
		Customer c = new Customer().setName("Ada").setCountry("UK");
		em.persist(c);
		em.persist(new Order().setCustomer(c).setStatus("PAID").setTotal(30.0));
		em.persist(new Order().setCustomer(c).setStatus("PAID").setTotal(10.0));
		em.persist(new Order().setCustomer(c).setStatus("PAID").setTotal(20.0));
		em.flush();

		List<Order> ordered = toList(SELECTㅤꁘㅤFROM(Order.class).ㅤORDERㅤBYㅤ(Order::getTotal).via(em));

		assertEquals(3, ordered.size());
		assertEquals(10.0, ordered.get(0).getTotal(), 0.0001);
		assertEquals(20.0, ordered.get(1).getTotal(), 0.0001);
		assertEquals(30.0, ordered.get(2).getTotal(), 0.0001);
	}

	/**
	 * {@code LIMIT}/{@code OFFSET} page through an ordered result set against real Hibernate/H2 — the
	 * middle page of five orders, sorted by total ascending, skipping the first and stopping before
	 * the last.
	 */
	@Test
	public void viaAppliesLimitAndOffsetForPagination() {
		em.persist(new Order().setStatus("PAID").setTotal(50.0));
		em.persist(new Order().setStatus("PAID").setTotal(10.0));
		em.persist(new Order().setStatus("PAID").setTotal(40.0));
		em.persist(new Order().setStatus("PAID").setTotal(20.0));
		em.persist(new Order().setStatus("PAID").setTotal(30.0));
		em.flush();

		EntityQ<Order> q = SELECTㅤꁘㅤFROM(Order.class).ㅤORDERㅤBYㅤ(Order::getTotal).LIMIT(2).OFFSET(1);
		assertEquals("from pro.peter_rader.linqava.h2.Order order by total limit 2 offset 1", q.getUnsafeHql());

		List<Order> page = toList(q.via(em));

		assertEquals(2, page.size());
		assertEquals(20.0, page.get(0).getTotal(), 0.0001);
		assertEquals(30.0, page.get(1).getTotal(), 0.0001);
	}

	/**
	 * {@code CAST(...)} translates a numeric column to its string representation, executed for real
	 * against Hibernate/H2 (the {@code String.class} target is resolved via Hibernate's unified type
	 * system).
	 */
	@Test
	public void viaExecutesCastOfNumericColumnToString() {
		em.persist(new Order().setStatus("PAID").setTotal(42.0));
		em.flush();

		Q<String> q = SELECTㅤ(ㅤCASTㅤ(Order::getTotal).ㅤASㅤ(String.class)).ㅤFROMㅤ(Order.class).ㅤAS("o")
				.ㅤWHEREㅤ(Order::getStatus).ㅤᆖㅤ("PAID");
		assertEquals("select cast(o.total as String) from pro.peter_rader.linqava.h2.Order o where o.status = 'PAID'", q.getUnsafeHql());

		List<String> results = new ArrayList<>();
		for (String value : q.via(em)) {
			results.add(value);
		}

		assertEquals(1, results.size());
		assertTrue(results.get(0).startsWith("42"));
	}

	/**
	 * {@code CONCAT(...)} joins a column and literals into a single string, executed for real against
	 * Hibernate/H2.
	 */
	@Test
	public void viaExecutesConcatOfColumnAndLiterals() {
		em.persist(new Order().setStatus("PAID").setTotal(10.0));
		em.flush();

		Q<Object> q = SELECTㅤ(CONCAT(Order::getStatus, "-", "done")).ㅤFROMㅤ(Order.class).ㅤAS("o");
		assertEquals("select concat(o.status, '-', 'done') from pro.peter_rader.linqava.h2.Order o", q.getUnsafeHql());

		List<String> results = q.via(em, String.class);

		assertEquals(1, results.size());
		assertEquals("PAID-done", results.get(0));
	}

	@Test
	public void unionAllCombinesTwoRealResultSets() {
		em.persist(new Order().setStatus("PAID").setTotal(10.0));
		em.persist(new Order().setStatus("CANCELLED").setTotal(20.0));
		em.persist(new Order().setStatus("PENDING").setTotal(30.0));
		em.flush();

		List<Order> combined = toList(SELECTㅤꁘㅤFROM(Order.class).ㅤWHEREㅤ(Order::getStatus).ㅤᆖㅤ("PAID")
				.UNIONㅤALL(SELECTㅤꁘㅤFROM(Order.class).ㅤWHEREㅤ(Order::getStatus).ㅤᆖㅤ("CANCELLED")).via(em));

		assertEquals(2, combined.size());
	}

	/**
	 * A many-to-many join through the full two-step {@code Q} form, across a genuinely exotic entity
	 * ({@link HTMLTag}, whose own type name and getter both lead with the {@code HTML} acronym).
	 */
	@Test
	public void manyToManyJoinFindsProductsHavingAGivenTag() {
		HTMLTag bestseller = new HTMLTag().setHTMLTagName("Bestseller");
		HTMLTag clearance = new HTMLTag().setHTMLTagName("Clearance");
		em.persist(bestseller);
		em.persist(clearance);

		Product novel = new Product().setSKUCode("BOOK-1");
		novel.getHTMLTags().add(bestseller);
		Product lamp = new Product().setSKUCode("LAMP-1");
		lamp.getHTMLTags().add(clearance);
		em.persist(novel);
		em.persist(lamp);
		em.flush();

		List<Product> bestsellers = toList(SELECTㅤ(Product.class).ㅤFROMㅤ(Product.class).ㅤAS("p")
				.JOIN(Product::getHTMLTags).ㅤAS("t").ㅤWHEREㅤ("t", HTMLTag::getHTMLTagName).ㅤᆖㅤ("Bestseller").via(em));

		assertEquals(1, bestsellers.size());
		assertEquals("BOOK-1", bestsellers.get(0).getSKUCode());
	}

	/**
	 * Filters by a property whose getter is <em>entirely</em> an acronym after the {@code get} prefix
	 * ({@code getSKUCode} &rarr; {@code SKUCode}), on top of {@link HTMLTag}'s acronym-leading entity
	 * name — both must resolve against the real Hibernate metamodel, not just render a plausible
	 * string.
	 */
	@Test
	public void findsByEntirelyAcronymGetterProperty() {
		em.persist(new Product().setSKUCode("ISBN-42"));
		em.flush();

		List<Product> matches = toList(SELECTㅤꁘㅤFROM(Product.class).ㅤWHEREㅤ(Product::getSKUCode).ㅤᆖㅤ("ISBN-42").via(em));

		assertEquals(1, matches.size());
	}

	/** An {@code @ElementCollection} map (locale &rarr; localized name) round-trips through a real session. */
	@Test
	public void elementCollectionMapRoundTripsThroughRealEntityManager() {
		Product book = new Product().setSKUCode("BOOK-2");
		book.getLocalizedNames().put("de", "Buch");
		book.getLocalizedNames().put("fr", "Livre");
		em.persist(book);
		em.flush();
		em.clear();

		Product reloaded = SELECTㅤꁘㅤFROM(Product.class).ㅤWHEREㅤ(Product::getSKUCode).ㅤᆖㅤ("BOOK-2").first(em);

		Map<String, String> names = reloaded.getLocalizedNames();
		assertEquals(2, names.size());
		assertEquals("Buch", names.get("de"));
		assertEquals("Livre", names.get("fr"));
	}

	/**
	 * A genuine map <em>association</em> ({@link Customer#getOrdersByStatus()}, {@code @OneToMany} +
	 * {@code @MapKey}) — distinct from the plain element-collection map above — round-trips and groups
	 * the same child rows by their own {@code status} property.
	 */
	@Test
	public void entityValuedMapAssociationGroupsOrdersByStatus() {
		Customer c = new Customer().setName("Ada").setCountry("UK");
		em.persist(c);
		em.persist(new Order().setCustomer(c).setStatus("PAID").setTotal(10.0));
		em.persist(new Order().setCustomer(c).setStatus("PENDING").setTotal(20.0));
		em.flush();
		em.clear();

		Customer reloaded = SELECTㅤꁘㅤFROM(Customer.class).ㅤWHEREㅤ(Customer::getName).ㅤᆖㅤ("Ada").first(em);

		Map<String, Order> byStatus = reloaded.getOrdersByStatus();
		assertEquals(2, byStatus.size());
		assertEquals(10.0, byStatus.get("PAID").getTotal(), 0.0001);
		assertEquals(20.0, byStatus.get("PENDING").getTotal(), 0.0001);
	}

	/**
	 * {@code via(EntityManager, Class)} executes a projection that selects an individual field
	 * alongside an aggregate ({@code group by status}) and returns each row as an {@code Object[]},
	 * rather than requiring the caller to drop down to {@code em.createQuery(getUnsafeHql())}.
	 */
	@Test
	public void viaWithObjectArrayExecutesFieldPlusAggregateProjection() {
		Customer c = new Customer().setName("Ada").setCountry("UK");
		em.persist(c);
		em.persist(new Order().setCustomer(c).setStatus("PAID").setTotal(10.0));
		em.persist(new Order().setCustomer(c).setStatus("PAID").setTotal(20.0));
		em.persist(new Order().setCustomer(c).setStatus("CANCELLED").setTotal(5.0));
		em.flush();

		Grouped<Object> q = SELECTㅤ(Order::getStatus, COUNT(Order::getId)).ㅤFROMㅤ(Order.class).ㅤAS("o")
				.GROUPㅤBY(Order::getStatus);
		assertEquals("select o.status, count(o.id) from pro.peter_rader.linqava.h2.Order o group by o.status", q.getUnsafeHql());

		List<Object[]> rows = q.via(em, Object[].class);

		Map<String, Long> countsByStatus = new HashMap<>();
		for (Object[] row : rows) {
			countsByStatus.put((String) row[0], (Long) row[1]);
		}
		assertEquals(2, rows.size());
		assertEquals(Long.valueOf(2), countsByStatus.get("PAID"));
		assertEquals(Long.valueOf(1), countsByStatus.get("CANCELLED"));
	}

	/**
	 * {@code via(EntityManager, Class)} also drives
	 * {@link pro.peter_rader.linqava.Linq#NEW(Class, TypedCol, Object...)} constructor projections straight
	 * into real DTO instances via Hibernate's own constructor-expression support.
	 */
	@Test
	public void viaWithDtoClassExecutesConstructorProjection() {
		Customer c = new Customer().setName("Ada").setCountry("UK");
		em.persist(c);
		em.persist(new Order().setCustomer(c).setStatus("PAID").setTotal(10.0));
		em.persist(new Order().setCustomer(c).setStatus("PAID").setTotal(20.0));
		em.flush();

		Grouped<Object> q = SELECTㅤ(NEW(StatusCount.class, Order::getStatus, COUNT(Order::getId))).ㅤFROMㅤ(Order.class)
				.ㅤAS("o").GROUPㅤBY(Order::getStatus);
		assertEquals("select new pro.peter_rader.linqava.h2.StatusCount(o.status, count(o.id)) "
				+ "from pro.peter_rader.linqava.h2.Order o group by o.status", q.getUnsafeHql());

		List<StatusCount> rows = q.via(em, StatusCount.class);

		assertEquals(1, rows.size());
		assertEquals("PAID", rows.get(0).getStatus());
		assertEquals(Long.valueOf(2), rows.get(0).getCount());
	}

	/**
	 * {@code COUNTㅤꁘ()} threads {@code Long} through to the resulting {@code ScalarQ<Long>}, whose
	 * {@code via(EntityManager)} returns the count directly (not a list) — no cast, no
	 * {@code via(em, Long.class)}, no {@code .iterator().next()}.
	 */
	@Test
	public void viaExecutesBareCountStarAsTypedLong() {
		Customer c = new Customer().setName("Ada").setCountry("UK");
		em.persist(c);
		em.persist(new Order().setCustomer(c).setStatus("PAID").setTotal(10.0));
		em.persist(new Order().setCustomer(c).setStatus("CANCELLED").setTotal(5.0));
		em.flush();

		ScalarQ<Long> q = SELECTㅤ(COUNTㅤꁘ()).ㅤFROMㅤ(Order.class);
		assertEquals("select count(*) from pro.peter_rader.linqava.h2.Order", q.getUnsafeHql());

		long count = q.via(em);

		assertEquals(2L, count);
	}

	/**
	 * The {@code ScalarQ} fluent chain supports {@code WHERE}/{@code AND} just like {@link Q} does,
	 * folding each predicate into the same underlying clause and staying typed all the way to a
	 * primitive {@code long} result — the shape needed for a boolean check such as
	 * "does this table have a compound primary key", i.e. "do more than one column count as PK".
	 */
	@Test
	public void viaExecutesFilteredCountWithWhereAndChain() {
		Customer c = new Customer().setName("Ada").setCountry("UK");
		Customer other = new Customer().setName("Bob").setCountry("US");
		em.persist(c);
		em.persist(other);
		em.persist(new Order().setCustomer(c).setStatus("PAID").setTotal(10.0).setDiscount(1.0));
		em.persist(new Order().setCustomer(c).setStatus("PAID").setTotal(20.0).setDiscount(2.0));
		em.persist(new Order().setCustomer(c).setStatus("PENDING").setTotal(5.0));
		em.persist(new Order().setCustomer(other).setStatus("PAID").setTotal(30.0).setDiscount(3.0));
		em.flush();

		ScalarQ<Long> q = SELECTㅤ(COUNTㅤꁘ()).ㅤFROMㅤ(Order.class).ㅤWHEREㅤ(Order::getCustomer).ㅤᆖㅤ(c)
				.ㅤANDㅤ(Order::getDiscount).ISㅤNOTㅤNULL();
		assertTrue(q.getUnsafeHql().startsWith("select count(*) from pro.peter_rader.linqava.h2.Order where customer = "));
		assertTrue(q.getUnsafeHql().endsWith(" and discount is not null"));

		long count = q.via(em);

		assertEquals(2L, count);
		assertTrue(count > 1);
	}

	/**
	 * The comparison/combinator/alias operators on a {@code ScalarExpr} (see {@code ScalarExpr}) all
	 * thread {@code Boolean} through, so a boolean combination built from two {@code COUNTㅤꁘ()}
	 * comparisons executes as a real {@code boolean} against Hibernate/H2 — no cast, no
	 * {@code Object[]}/tuple handling.
	 */
	@Test
	public void viaExecutesBooleanCombinationOfCountComparisons() {
		Customer c = new Customer().setName("Ada").setCountry("UK");
		em.persist(c);
		em.persist(new Order().setCustomer(c).setStatus("PAID").setTotal(10.0));
		em.persist(new Order().setCustomer(c).setStatus("PAID").setTotal(20.0));
		em.flush();

		ScalarQ<Boolean> q = SELECTㅤ(COUNTㅤꁘ().ㅤᐳㅤ(1).ㅤANDㅤ(COUNTㅤꁘ()).ㅤᐸㅤ(5).ㅤAS("x")).ㅤFROMㅤ(Order.class)
				.ㅤWHEREㅤ(Order::getCustomer).ㅤᆖㅤ(c).ㅤANDㅤ(Order::getStatus).ㅤᆖㅤ("PAID");
		assertTrue(q.getUnsafeHql().startsWith("select count(*) > 1 and count(*) < 5 as x from pro.peter_rader.linqava.h2.Order where customer = "));

		boolean withinRange = q.via(em);

		assertTrue(withinRange);
	}

	/**
	 * A self-join where both aliases refer to the <em>same</em> entity — bare {@code TypedCol}
	 * references would be ambiguous between the two aliases, so every column on both sides of the
	 * {@code ON} condition (and the {@code SELECT}/{@code WHERE}) goes through one of the dedicated
	 * {@code (alias, getter)}-qualified overloads instead (on {@code SELECT}, {@code LOWER},
	 * {@code ᆖ}/{@code ᐸᐳ} and {@code WHERE}). Real end-to-end verification that {@code LOWER(...)}
	 * inside a multi-predicate {@code ON} clause, combined via the static {@code AND(Cond...)},
	 * produces working HQL against Hibernate/H2 — mirrors a duplicate-detection query pattern (find
	 * pairs of distinct rows sharing the same, not-yet-lowercased text).
	 */
	@Test
	public void viaExecutesSelfJoinWithLowerBasedOnCondition() {
		EMailAddressLocalName dup1 = new EMailAddressLocalName().setLocalName("John.Doe");
		EMailAddressLocalName dup2 = new EMailAddressLocalName().setLocalName("John.Doe");
		EMailAddressLocalName alreadyLower = new EMailAddressLocalName().setLocalName("jane.doe");
		em.persist(dup1);
		em.persist(dup2);
		em.persist(alreadyLower);
		em.flush();

		Q<Object> q = SELECTㅤ("a", EMailAddressLocalName::getId, "b", EMailAddressLocalName::getId)
				.ㅤFROMㅤ(EMailAddressLocalName.class).ㅤAS("a").JOIN(EMailAddressLocalName.class).ㅤAS("b")
				.ㅤONㅤ(ㅤANDㅤ(
						ㅤᐸᐳㅤ(LOWER("a", EMailAddressLocalName::getLocalName), "b", EMailAddressLocalName::getLocalName),
						ㅤᆖㅤ("a", EMailAddressLocalName::getLocalName, "b", EMailAddressLocalName::getLocalName),
						ㅤᐸᐳㅤ("a", EMailAddressLocalName::getId, "b", EMailAddressLocalName::getId)))
				.ㅤWHEREㅤ("b", EMailAddressLocalName::getId).ISㅤNOTㅤNULL();

		List<Long[]> rows = q.via(em, Long[].class);

		assertEquals(2, rows.size());
		Set<Long> idsInvolved = new HashSet<>();
		for (Long[] row : rows) {
			idsInvolved.add(row[0]);
			idsInvolved.add(row[1]);
		}
		assertEquals(new HashSet<>(Arrays.asList(dup1.getId(), dup2.getId())), idsInvolved);
	}

	/**
	 * {@code ScalarQ#FLUSHㅤMODE} really reaches the underlying {@link jakarta.persistence.TypedQuery}:
	 * with {@link FlushModeType#COMMIT}, a pending (unflushed) insert stays invisible to the count,
	 * whereas the default {@link FlushModeType#AUTO} implicitly flushes it first.
	 */
	@Test
	public void viaHonorsFlushModeOverrideOnScalarQ() {
		em.persist(new Order().setStatus("PAID").setTotal(10.0));

		ScalarQ<Long> commitMode = SELECTㅤ(COUNTㅤꁘ()).ㅤFROMㅤ(Order.class).FLUSHㅤMODE(FlushModeType.COMMIT);
		assertEquals(Long.valueOf(0), commitMode.via(em));

		ScalarQ<Long> autoMode = SELECTㅤ(COUNTㅤꁘ()).ㅤFROMㅤ(Order.class);
		assertEquals(Long.valueOf(1), autoMode.via(em));
	}

	/**
	 * Same guarantee as {@link #viaHonorsFlushModeOverrideOnScalarQ()}, but for the full {@link Q} form
	 * — {@code Q#FLUSHㅤMODE} reaches every {@code via}/{@code first} call since they all create their
	 * {@link jakarta.persistence.TypedQuery} through {@link Q#via(EntityManager)}.
	 */
	@Test
	public void viaHonorsFlushModeOverrideOnQ() {
		em.persist(new Order().setStatus("PAID").setTotal(10.0));

		Q<Order> commitMode = SELECTㅤ(Order.class).ㅤFROMㅤ(Order.class).ㅤAS("o")
				.ㅤWHEREㅤ(Order::getStatus).ㅤᆖㅤ("PAID").FLUSHㅤMODE(FlushModeType.COMMIT);
		assertTrue(toList(commitMode.via(em)).isEmpty());

		Q<Order> autoMode = SELECTㅤ(Order.class).ㅤFROMㅤ(Order.class).ㅤAS("o").ㅤWHEREㅤ(Order::getStatus)
				.ㅤᆖㅤ("PAID");
		assertEquals(1, toList(autoMode.via(em)).size());
	}

	/**
	 * Same guarantee as {@link #viaHonorsFlushModeOverrideOnScalarQ()}, but for the {@link EntityQ}
	 * shorthand.
	 */
	@Test
	public void viaHonorsFlushModeOverrideOnEntityQ() {
		em.persist(new Order().setStatus("PAID").setTotal(10.0));

		EntityQ<Order> commitMode = SELECTㅤꁘㅤFROM(Order.class).ㅤWHEREㅤ(Order::getStatus).ㅤᆖㅤ("PAID")
				.FLUSHㅤMODE(FlushModeType.COMMIT);
		assertTrue(toList(commitMode.via(em)).isEmpty());

		EntityQ<Order> autoMode = SELECTㅤꁘㅤFROM(Order.class).ㅤWHEREㅤ(Order::getStatus).ㅤᆖㅤ("PAID");
		assertEquals(1, toList(autoMode.via(em)).size());
	}

	private static <T> List<T> toList(final Iterable<T> it) {
		List<T> list = new ArrayList<>();
		it.forEach(list::add);
		return list;
	}
}
