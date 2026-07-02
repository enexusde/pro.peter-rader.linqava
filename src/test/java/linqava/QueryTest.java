/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _ 
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package linqava;

import static linqava.Linq.ㅤANDㅤ;
import static linqava.Linq.AVG;
import static linqava.Linq.CASE;
import static linqava.Linq.COALESCE;
import static linqava.Linq.COUNT;
import static linqava.Linq.DISTINCTㅤ;
import static linqava.Linq.MAX;
import static linqava.Linq.NEW;
import static linqava.Linq.NULLIF;
import static linqava.Linq.OR;
import static linqava.Linq.PARTITION‿BY;
import static linqava.Linq.RANK;
import static linqava.Linq.ROW_NUMBER;
import static linqava.Linq.SELECTㅤ;
import static linqava.Linq.SIZE;
import static linqava.Linq.SUM;
import static linqava.Linq.TREAT;
import static linqava.Linq.WITH;
import static linqava.Linq.WITH‿RECURSIVE;
import static linqava.Linq.c;
import static linqava.Linq.lit;
import static linqava.Linq.param;
import static linqava.Linq.sub;
import static linqava.Linq.ᆖ;
import static linqava.Linq.ᐸ;
import static linqava.Linq.ᐸᆖ;
import static linqava.Linq.ᐳ;
import static linqava.Linq.ᐳᆖ;
import static org.junit.Assert.assertEquals;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 * Verifies that the HQL produced by {@link Q#getHql()} is correct for each
 * complex HQL/JPQL query.
 *
 * <p>
 * Mapping rules:
 * </p>
 * <ul>
 * <li>Keywords {@code SELECT / FROM / WHERE / JOIN / ...} are methods;
 * multi-word keywords are a single method whose words are joined by {@code ‿}
 * (U+203F), e.g. {@code LEFT‿JOIN}, {@code GROUP‿BY}, {@code ORDER‿BY},
 * {@code UNION‿ALL}.</li>
 * <li>Columns are getter method references ({@code User::Name}); as a value
 * they are wrapped with {@code c(...)}, derived/aliased columns use
 * {@code c("alias")}.</li>
 * <li>The {@code AS} keyword aliases both fields ({@code c(User::id).AS("id")})
 * and tables ({@code FROM(User.class).AS("u")}).</li>
 * <li>Operators are methods on the condition context {@code $}: {@code ᆖ} (=),
 * {@code ᐸ} (&lt;), {@code ᐳ} (&gt;), {@code ᐸᆖ} (&lt;=), {@code ᐳᆖ} (&gt;=),
 * {@code ᐸᐳ} (&lt;&gt;); math: {@code ᐩ} (+), {@code ｰ} (-), {@code ᚷ} (*),
 * {@code ノ} (/).</li>
 * </ul>
 */
public class QueryTest {

	// SELECT id FROM User WHERE Name='John'
	@Test
	public void testCuteQuery() {
		Q q = SELECTㅤ(User::id).AS("idx").ㅤFROMㅤ(User.class);
		assertEquals("select id as idx from User", q.getHql());
	}

	// SELECT id FROM User WHERE Name='John'
	@Test
	public void testSimpleQuery() {
		Q q = SELECTㅤ(User::id).ㅤFROMㅤ(User.class).WHEREㅤ($ -> $.ᆖ(User::Name, "John"));
		assertEquals("select id from User where Name = 'John'", q.getHql());
	}

	// WITH activeUsers AS (...) SELECT a.name FROM activeUsers a ORDER BY a.name
	@Test
	public void testCteSimple() {
		Q q = WITH("activeUsers",
				SELECTㅤ(c(User::id).ㅤASㅤ("id"), c(User::name).ㅤASㅤ("name")).ㅤFROMㅤ(User.class).ㅤASㅤ("u")
						.WHEREㅤ($ -> $.ᆖ(User::active, true)))
				.SELECTㅤ(c("a", "name")).FROM("activeUsers").ㅤASㅤ("a").ㅤORDER‿BYㅤ(c("a", "name"));
		assertEquals("with activeUsers as (select u.id as id, u.name as name from User u "
				+ "where u.active = true) select a.name from activeUsers a order by a.name", q.getHql());
	}

	// WITH recentOrders AS (...), bigSpenders AS (...) SELECT ... FROM bigSpenders
	// b JOIN Customer c ...
	@Test
	public void testCteMultiple() {
		Q q = WITH("recentOrders",
				SELECTㅤ(c(Order::id).ㅤASㅤ("id"), c(Order::customerId).ㅤASㅤ("customerId")).ㅤFROMㅤ(Order.class).ㅤASㅤ("o")
						.WHEREㅤ($ -> $.ᐳ(Order::createdAt, param("since"))))
				.WITH("bigSpenders",
						SELECTㅤ(c(Order::customerId).ㅤASㅤ("customerId"), SUM(Order::total).ㅤASㅤ("total"))
								.FROM("recentOrders").ㅤASㅤ("r").JOIN(Order.class).ㅤASㅤ("o")
								.ㅤONㅤ($ -> $.ᆖ(Order::id, c("r", "id"))).GROUP‿BY(c(Order::customerId))
								.HAVING($ -> $.ㅤᐳㅤ(SUM(Order::total), param("threshold"))))
				.SELECTㅤ(Customer::name, c("b", "total")).FROM("bigSpenders").ㅤASㅤ("b").JOIN(Customer.class).ㅤASㅤ("c")
				.ㅤONㅤ($ -> $.ᆖ(Customer::id, c("b", "customerId"))).ㅤORDER‿BYㅤ(c("b", "total").DESC());
		assertEquals("with recentOrders as (select o.id as id, o.customerId as customerId from Order o "
				+ "where o.createdAt > :since), bigSpenders as (select o.customerId as customerId, "
				+ "sum(o.total) as total from recentOrders r join Order o on o.id = r.id "
				+ "group by o.customerId having sum(o.total) > :threshold) "
				+ "select c.name, b.total from bigSpenders b join Customer c on c.id = b.customerId "
				+ "order by b.total desc", q.getHql());
	}

	// WITH RECURSIVE empHierarchy AS (anchor UNION ALL recursive) SELECT h.id,
	// h.depth ...
	@Test
	public void testRecursiveCte() {
		Q q = WITH‿RECURSIVE("empHierarchy",
				SELECTㅤ(c(Employee::id).ㅤASㅤ("id"), c(Employee::managerId).ㅤASㅤ("managerId"), lit(0).ㅤASㅤ("depth"))
						.ㅤFROMㅤ(Employee.class).ㅤASㅤ("e").WHEREㅤ($ -> $.IS‿NULL(Employee::managerId))
						.UNION‿ALL(SELECTㅤ(c(Employee::id).ㅤASㅤ("id"), c(Employee::managerId).ㅤASㅤ("managerId"),
								c("h", "depth").ᐩ(1).ㅤASㅤ("depth")).ㅤFROMㅤ(Employee.class).ㅤASㅤ("e")
								.JOIN("empHierarchy").ㅤASㅤ("h").ㅤONㅤ($ -> $.ᆖ(Employee::managerId, c("h", "id")))))
				.SELECTㅤ(c("h", "id"), c("h", "depth")).FROM("empHierarchy").ㅤASㅤ("h")
				.ㅤORDER‿BYㅤ(c("h", "depth"), c("h", "id"));
		assertEquals("with recursive empHierarchy as (select e.id as id, e.managerId as managerId, 0 as depth "
				+ "from Employee e where e.managerId is null union all "
				+ "select e.id as id, e.managerId as managerId, h.depth + 1 as depth "
				+ "from Employee e join empHierarchy h on e.managerId = h.id) "
				+ "select h.id, h.depth from empHierarchy h order by h.depth, h.id", q.getHql());
	}

	// SELECT o FROM Order o WHERE o.total = (SELECT MAX(o2.total) FROM Order o2
	// WHERE o2.customerId = o.customerId)
	@Test
	public void testCorrelatedSubquery() {
		Q q = SELECTㅤ(Order.class).ㅤFROMㅤ(Order.class).ㅤASㅤ("o")
				.WHEREㅤ($ -> $.ᆖ(Order::total, SELECTㅤ(MAX(Order::total)).ㅤFROMㅤ(Order.class).ㅤASㅤ("o2")
						.WHEREㅤ(s -> s.ᆖ(Order::customerId, c("o", Order::customerId)))));
		assertEquals("select o from Order o where o.total = "
				+ "(select max(o2.total) from Order o2 where o2.customerId = o.customerId)", q.getHql());
	}

	// SELECT c FROM Customer c WHERE EXISTS (SELECT 1 FROM Order o WHERE
	// o.customerId = c.id AND o.status = 'PAID')
	@Test
	public void testExistsSubquery() {
		Q q = SELECTㅤ(Customer.class).ㅤFROMㅤ(Customer.class).ㅤASㅤ("c")
				.WHEREㅤ($ -> $.EXISTS(SELECTㅤ(lit(1)).ㅤFROMㅤ(Order.class).ㅤASㅤ("o")
						.WHEREㅤ(s -> s.ᆖ(Order::customerId, c("c", Customer::id)).ㅤANDㅤ().ᆖ(Order::status, "PAID"))));
		assertEquals("select c from Customer c where exists "
				+ "(select 1 from Order o where o.customerId = c.id and o.status = 'PAID')", q.getHql());
	}

	// SELECT p FROM Product p WHERE p.categoryId IN (SELECT cat.id FROM Category
	// cat WHERE cat.parentId = :parent)
	@Test
	public void testInSubquery() {
		Q q = SELECTㅤ(Product.class).ㅤFROMㅤ(Product.class).ㅤASㅤ("p")
				.WHEREㅤ($ -> $.IN(Product::categoryId, SELECTㅤ(Category::id).ㅤFROMㅤ(Category.class).ㅤASㅤ("cat")
						.WHEREㅤ(s -> s.ᆖ(Category::parentId, param("parent")))));
		assertEquals("select p from Product p where p.categoryId in "
				+ "(select cat.id from Category cat where cat.parentId = :parent)", q.getHql());
	}

	// SELECT o.customerId, COUNT(o.id), SUM(o.total) FROM Order o WHERE o.status <>
	// 'CANCELLED' GROUP BY ... HAVING ... ORDER BY ...
	@Test
	public void testGroupByHaving() {
		Q q = SELECTㅤ(Order::customerId, COUNT(Order::id), SUM(Order::total)).ㅤFROMㅤ(Order.class).ㅤASㅤ("o")
				.WHEREㅤ($ -> $.ᐸᐳ(Order::status, "CANCELLED")).GROUP‿BY(c(Order::customerId))
				.HAVING($ -> $.ㅤᐳㅤ(COUNT(Order::id), 5).ㅤANDㅤ().ㅤᐳㅤ(SUM(Order::total), 1000))
				.ㅤORDER‿BYㅤ(SUM(Order::total).DESC());
		assertEquals("select o.customerId, count(o.id), sum(o.total) from Order o "
				+ "where o.status <> 'CANCELLED' group by o.customerId "
				+ "having count(o.id) > 5 and sum(o.total) > 1000 order by sum(o.total) desc", q.getHql());
	}

	// SELECT DISTINCT c FROM Customer c LEFT JOIN FETCH c.orders o LEFT JOIN FETCH
	// o.items WHERE c.country = :country
	@Test
	public void testJoinFetch() {
		Q q = SELECTㅤ(DISTINCTㅤ(Customer.class)).ㅤFROMㅤ(Customer.class).ㅤASㅤ("c").LEFT‿JOIN‿FETCH(c(Customer::orders))
				.ㅤASㅤ("o").LEFT‿JOIN‿FETCH(c("o", "items")).WHEREㅤ($ -> $.ᆖ(Customer::country, param("country")));
		assertEquals("select distinct c from Customer c left join fetch c.orders o "
				+ "left join fetch o.items where c.country = :country", q.getHql());
	}

	// SELECT o.id, c.name, p.title FROM Order o JOIN Customer c ON ... JOIN
	// OrderItem oi ON ... JOIN Product p ON ...
	@Test
	public void testMultipleJoinsWithOn() {
		Q q = SELECTㅤ(Order::id, c(Customer::name), c(Product::title)).ㅤFROMㅤ(Order.class).ㅤASㅤ("o")
				.JOIN(Customer.class).ㅤASㅤ("c").ㅤONㅤ($ -> $.ᆖ(Customer::id, c("o", Order::customerId)))
				.JOIN(OrderItem.class).ㅤASㅤ("oi").ㅤONㅤ($ -> $.ᆖ(OrderItem::orderId, c("o", Order::id)))
				.JOIN(Product.class).ㅤASㅤ("p").ㅤONㅤ($ -> $.ᆖ(Product::id, c("oi", OrderItem::productId)))
				.WHEREㅤ($ -> $.ᐳ(Product::price, param("minPrice")));
		assertEquals("select o.id, c.name, p.title from Order o "
				+ "join Customer c on c.id = o.customerId join OrderItem oi on oi.orderId = o.id "
				+ "join Product p on p.id = oi.productId where p.price > :minPrice", q.getHql());
	}

	// SELECT o.id, CASE WHEN ... THEN ... ELSE ... END FROM Order o
	@Test
	public void testCaseWhen() {
		Q q = SELECTㅤ(Order::id, CASE().WHEN($ -> $.ᐳᆖ(Order::total, 1000)).THEN("GOLD")
				.WHEN($ -> $.ᐳᆖ(Order::total, 100)).THEN("SILVER").ELSE("BRONZE").END()).ㅤFROMㅤ(Order.class).ㅤASㅤ("o");
		assertEquals("select o.id, case when o.total >= 1000 then 'GOLD' "
				+ "when o.total >= 100 then 'SILVER' else 'BRONZE' end from Order o", q.getHql());
	}

	// SELECT o.customerId, o.id, o.total, ROW_NUMBER() OVER (PARTITION BY ... ORDER
	// BY ... DESC) AS rn FROM Order o
	@Test
	public void testWindowFunction() {
		Q q = SELECTㅤ(Order::customerId, c(Order::id), c(Order::total),
				ROW_NUMBER().OVER(PARTITION‿BY(c(Order::customerId)).ORDER‿BY(Order::total).DESC()).ㅤASㅤ("rn"))
				.ㅤFROMㅤ(Order.class).ㅤASㅤ("o");
		assertEquals(
				"select o.customerId, o.id, o.total, "
						+ "row_number() over (partition by o.customerId order by o.total desc) as rn from Order o",
				q.getHql());
	}

	// SELECT u.email FROM User u WHERE u.active = true UNION ALL SELECT s.email
	// FROM Supplier s WHERE s.preferred = true
	@Test
	public void testUnionAll() {
		Q q = SELECTㅤ(c(User::email).ㅤASㅤ("contact")).ㅤFROMㅤ(User.class).ㅤASㅤ("u").WHEREㅤ($ -> $.ᆖ(User::active, true))
				.UNION‿ALL(SELECTㅤ(c(Supplier::email).ㅤASㅤ("contact")).ㅤFROMㅤ(Supplier.class).ㅤASㅤ("s")
						.WHEREㅤ(s -> s.ᆖ(Supplier::preferred, true)));
		assertEquals("select u.email as contact from User u where u.active = true union all "
				+ "select s.email as contact from Supplier s where s.preferred = true", q.getHql());
	}

	// SELECT NEW CustomerSummary(c.id, c.name, COUNT(o.id)) FROM Customer c LEFT
	// JOIN c.orders o GROUP BY c.id, c.name
	@Test
	public void testConstructorExpression() {
		Q q = SELECTㅤ(NEW(CustomerSummary.class, c(Customer::id), c(Customer::name), COUNT(c("o", Order::id))))
				.ㅤFROMㅤ(Customer.class).ㅤASㅤ("c").LEFT‿JOIN(Customer::orders).ㅤASㅤ("o")
				.GROUP‿BY(c(Customer::id), c(Customer::name));
		assertEquals("select new linqava.CustomerSummary(c.id, c.name, count(o.id)) from Customer c "
				+ "left join c.orders o group by c.id, c.name", q.getHql());
	}

	// SELECT c FROM Customer c WHERE :product MEMBER OF c.wishlist AND c.orders IS
	// NOT EMPTY AND SIZE(c.orders) > 3
	@Test
	public void testCollectionMemberAndEmpty() {
		Q q = SELECTㅤ(Customer.class).ㅤFROMㅤ(Customer.class).ㅤASㅤ("c")
				.WHEREㅤ($ -> $.MEMBER‿OF(param("product"), c(Customer::wishlist)).ㅤANDㅤ().IS‿NOT‿EMPTY(Customer::orders)
						.ㅤANDㅤ().ㅤᐳㅤ(SIZE(Customer::orders), 3));
		assertEquals("select c from Customer c where :product member of c.wishlist "
				+ "and c.orders is not empty and size(c.orders) > 3", q.getHql());
	}

	// SELECT p FROM Payment p WHERE TREAT(p AS CreditCardPayment).cardType =
	// :cardType OR TREAT(...).iban LIKE :ibanPrefix
	@Test
	public void testTreatPolymorphism() {
		Q q = SELECTㅤ(Payment.class).ㅤFROMㅤ(Payment.class).ㅤASㅤ("p").WHEREㅤ($ -> $
				.ᆖ(TREAT(Payment.class, CreditCardPayment.class).ᐧ(CreditCardPayment::cardType), param("cardType")).OR()
				.LIKE(TREAT(Payment.class, BankTransferPayment.class).ᐧ(BankTransferPayment::iban),
						param("ibanPrefix")));
		assertEquals("select p from Payment p where treat(p as CreditCardPayment).cardType = :cardType "
				+ "or treat(p as BankTransferPayment).iban like :ibanPrefix", q.getHql());
	}

	// WITH rankedOrders AS (... RANK() OVER (...) AS rnk ...) SELECT r.customerId,
	// r.id, r.total FROM rankedOrders r WHERE r.rnk = 1
	@Test
	public void testCteWithWindowFunction() {
		Q q = WITH("rankedOrders",
				SELECTㅤ(c(Order::id).ㅤASㅤ("id"), c(Order::customerId).ㅤASㅤ("customerId"), c(Order::total).ㅤASㅤ("total"),
						RANK().OVER(PARTITION‿BY(c(Order::customerId)).ORDER‿BY(Order::total).DESC()).ㅤASㅤ("rnk"))
						.ㅤFROMㅤ(Order.class).ㅤASㅤ("o"))
				.SELECTㅤ(c("r", "customerId"), c("r", "id"), c("r", "total")).FROM("rankedOrders").ㅤASㅤ("r")
				.WHEREㅤ($ -> $.ᆖ(c("r", "rnk"), 1));
		assertEquals("with rankedOrders as (select o.id as id, o.customerId as customerId, o.total as total, "
				+ "rank() over (partition by o.customerId order by o.total desc) as rnk from Order o) "
				+ "select r.customerId, r.id, r.total from rankedOrders r where r.rnk = 1", q.getHql());
	}

	// SELECT c.name, (SELECT COUNT(o.id) FROM Order o WHERE o.customerId = c.id) AS
	// orderCount FROM Customer c ORDER BY orderCount DESC
	@Test
	public void testScalarSubqueryInSelect() {
		Q q = SELECTㅤ(Customer::name,
				sub(SELECTㅤ(COUNT(Order::id)).ㅤFROMㅤ(Order.class).ㅤASㅤ("o")
						.WHEREㅤ(s -> s.ᆖ(Order::customerId, c("c", Customer::id)))).ㅤASㅤ("orderCount"))
				.ㅤFROMㅤ(Customer.class).ㅤASㅤ("c").ㅤORDER‿BYㅤ(c("orderCount").DESC());
		assertEquals("select c.name, (select count(o.id) from Order o where o.customerId = c.id) as orderCount "
				+ "from Customer c order by orderCount desc", q.getHql());
	}

	// SELECT o.customerId, SUM(CASE WHEN ... END) AS paidTotal,
	// COALESCE(AVG(NULLIF(...)), 0) AS avgDiscount FROM Order o GROUP BY ...
	@Test
	public void testNestedCaseInAggregateWithCoalesce() {
		Q q = SELECTㅤ(Order::customerId,
				SUM(CASE().WHEN($ -> $.ᆖ(Order::status, "PAID")).THEN(c(Order::total)).ELSE(0).END()).ㅤASㅤ("paidTotal"),
				COALESCE(AVG(NULLIF(Order::discount, 0)), 0).ㅤASㅤ("avgDiscount")).ㅤFROMㅤ(Order.class).ㅤASㅤ("o")
				.GROUP‿BY(c(Order::customerId));
		assertEquals(
				"select o.customerId, sum(case when o.status = 'PAID' then o.total else 0 end) as paidTotal, "
						+ "coalesce(avg(nullif(o.discount, 0)), 0) as avgDiscount from Order o group by o.customerId",
				q.getHql());
	}

	// SELECT o FROM Order o WHERE status = 'PAID' AND (total > 100 OR (discount < 5
	// AND (customerId = 42 OR (total >= 1000 AND discount <= 50))))
	@Test
	public void testDeeplyNestedAndOr() {
		Q q = SELECTㅤ(Order.class).ㅤFROMㅤ(Order.class).ㅤASㅤ("o")
				.ㅤWHEREㅤ(ㅤANDㅤ(ᆖ(Order::status, "PAID"), OR(ᐳ(Order::total, 100), ㅤANDㅤ(ᐸ(Order::discount, 5),
						OR(ᆖ(Order::customerId, 42), ㅤANDㅤ(ᐳᆖ(Order::total, 1000), ᐸᆖ(Order::discount, 50)))))));
		assertEquals(
				"select o from Order o where (o.status = 'PAID' and (o.total > 100 or "
						+ "(o.discount < 5 and (o.customerId = 42 or (o.total >= 1000 and o.discount <= 50)))))",
				q.getHql());
	}

	// via(EntityManager) on a single-entity query returns a typed List<Order>.
	@Test
	public void testViaSingleEntity() {
		Q q = SELECTㅤ(Order.class).ㅤFROMㅤ(Order.class).ㅤASㅤ("o");
		String[] capturedHql = new String[1];
		List<Order> expected = Arrays.asList(new Order(), new Order());
		EntityManager em = fakeEntityManager(capturedHql, expected);

		List<Order> result = q.via(em);

		assertEquals("select o from Order o", capturedHql[0]);
		assertEquals(2, result.size());
	}

	// via(EntityManager) also works for SELECT DISTINCT of a single entity.
	@Test
	public void testViaDistinctSingleEntity() {
		Q q = SELECTㅤ(DISTINCTㅤ(Customer.class)).ㅤFROMㅤ(Customer.class).ㅤASㅤ("c");
		String[] capturedHql = new String[1];
		EntityManager em = fakeEntityManager(capturedHql, Collections.singletonList(new Customer()));

		List<Customer> result = q.via(em);

		assertEquals("select distinct c from Customer c", capturedHql[0]);
		assertEquals(1, result.size());
	}

	// via(EntityManager) rejects scalar/tuple projections.
	@Test(expected = IllegalStateException.class)
	public void testViaRejectsProjection() {
		Q q = SELECTㅤ(Order::id).ㅤFROMㅤ(Order.class).ㅤASㅤ("o");
		q.via(fakeEntityManager(new String[1], Collections.emptyList()));
	}

	// SELECT c FROM Car c WHERE c.driver.id > 0 AND c.plate.id > 0
	@Test
	public void testComplex1() {
		Q q = SELECTㅤ(Car.class).ㅤFROMㅤ(Car.class).ㅤASㅤ("c")
				.WHEREㅤ($ -> $.ㅤᐳㅤ(c(Car::driver).ᐧ(Driver::id), 0).ㅤANDㅤ().ㅤᐳㅤ(c(Car::plate).ᐧ(SerialPlate::id), 0));
		assertEquals("select c from Car c where c.driver.id > 0 and c.plate.id > 0", q.getHql());
	}// SELECT c FROM Car c WHERE c.driver.id > 0 AND c.plate.id > 0

	@Test
	public void testComplex2() {
		Q q = SELECTㅤ(Driver.class).ㅤFROMㅤ(Driver.class).WHEREㅤ($ -> $.ㅤᐳㅤ(c(Driver::id), 0));
		assertEquals("select Driver from Driver where id > 0", q.getHql());
	}
	@Test
	public void testComplex3() {
		Q q = SELECTㅤ(Driver.class).ㅤFROMㅤ(Driver.class).ㅤWHEREㅤ(Driver::id).ㅤᐳㅤ(0).ㅤANDㅤ(Driver::id).ᐸ(3);
		assertEquals("select Driver from Driver where id > 0 and id < 3", q.getHql());
	}

	/**
	 * A minimal {@link EntityManager} that records the HQL passed to
	 * {@code createQuery} and returns a fixed list.
	 */
	private static EntityManager fakeEntityManager(String[] capturedHql, List<?> resultList) {
		ClassLoader cl = QueryTest.class.getClassLoader();
		InvocationHandler queryHandler = (proxy, method, args) -> method.getName().equals("getResultList") ? resultList
				: proxy;
		InvocationHandler emHandler = (proxy, method, args) -> {
			if (method.getName().equals("createQuery") && args != null && args.length == 2
					&& args[0] instanceof String) {
				capturedHql[0] = (String) args[0];
				return Proxy.newProxyInstance(cl, new Class<?>[] { TypedQuery.class }, queryHandler);
			}
			return null;
		};
		return (EntityManager) Proxy.newProxyInstance(cl, new Class<?>[] { EntityManager.class }, emHandler);
	}
}
