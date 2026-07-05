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
import static pro.peter_rader.linqava.Linq.SELECTㅤ;
import static pro.peter_rader.linqava.Linq.SELECTㅤꁘㅤFROM;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
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

	private static <T> List<T> toList(final Iterable<T> it) {
		List<T> list = new ArrayList<>();
		it.forEach(list::add);
		return list;
	}
}
