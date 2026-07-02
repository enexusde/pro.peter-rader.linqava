/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _ 
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package linqava;

import static linqava.Linq.AVG;
import static linqava.Linq.CASE;
import static linqava.Linq.COALESCE;
import static linqava.Linq.COUNT;
import static linqava.Linq.DISTINCTㅤ;
import static linqava.Linq.MAX;
import static linqava.Linq.NEW;
import static linqava.Linq.NULLIF;
import static linqava.Linq.ㅤPARTITIONㅤBYㅤ;
import static linqava.Linq.RANK;
import static linqava.Linq.ROW_NUMBER;
import static linqava.Linq.SELECTㅤ;
import static linqava.Linq.SIZE;
import static linqava.Linq.SUM;
import static linqava.Linq.ㅤTREATㅤ;
import static linqava.Linq.WITH;
import static linqava.Linq.WITHㅤRECURSIVE;
import static linqava.Linq.col;
import static linqava.Linq.lit;
import static linqava.Linq.param;
import static linqava.Linq.sub;
import static linqava.Linq.ㅤᆖㅤ;
import static linqava.Linq.ㅤANDㅤ;
import static linqava.Linq.ㅤEXISTSㅤ;
import static linqava.Linq.ㅤMEMBERㅤOFㅤ;
import static linqava.Linq.ㅤORㅤ;
import static linqava.Linq.ㅤᐳᆖㅤ;
import static linqava.Linq.ㅤᐳㅤ;
import static linqava.Linq.ㅤᐸᆖㅤ;
import static linqava.Linq.ㅤᐸㅤ;
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
 * multi-word keywords are a single method whose words are joined by {@code ㅤ}
 * (U+203F), e.g. {@code LEFTㅤJOIN}, {@code GROUPㅤBY}, {@code ORDERㅤBY},
 * {@code UNIONㅤALL}.</li>
 * <li>Columns are getter method references ({@code User::Name}); most call
 * sites (comparisons, {@code GROUP BY}, {@code ORDER BY}, {@code JOIN FETCH},
 * {@code CASE...THEN}, ...) accept them bare. Where a column needs
 * {@code .AS(...)}/arithmetic chained onto it, or is a derived/alias-qualified
 * column with no getter, wrap it with {@code col(...)} — {@code col(User::id)},
 * {@code col("orderCount")}, {@code col("alias", "field")}.</li>
 * <li>The {@code AS} keyword aliases both fields ({@code col(User::id).AS("id")})
 * and tables ({@code FROM(User.class).AS("u")}).</li>
 * <li>{@code WHERE}/{@code ON}/{@code HAVING} start from a bare column or
 * expression, followed by an operator supplying the right-hand value, e.g.
 * {@code WHERE(User::Name).ᆖ("John")}; chain further predicates with
 * {@code .AND(col)}/{@code .OR(col)}. Operator glyphs: {@code ᆖ} (=), {@code ᐸ}
 * (&lt;), {@code ᐳ} (&gt;), {@code ᐸᆖ} (&lt;=), {@code ᐳᆖ} (&gt;=), {@code ᐸᐳ}
 * (&lt;&gt;); math: {@code ᐩ} (+), {@code ｰ} (-), {@code ᚷ} (*), {@code ノ} (/).
 * For nested/grouped boolean trees or predicates that don't start from a single
 * column (e.g. {@code EXISTS}, {@code MEMBERㅤOF}), build a {@link Cond} from
 * {@link Linq}'s static predicate functions and pass it directly, e.g.
 * {@code WHERE(AND(ᆖ(...), OR(...)))}.</li>
 * </ul>
 */
public class QueryTest {

	// SELECT id FROM User WHERE Name='John'
	@Test
	public void testCuteQuery() {
		Q<Object> q = SELECTㅤ(User::id).AS("idx").ㅤFROMㅤ(User.class);
		assertEquals("select id as idx from User", q.getHql());
	}

	// SELECT id FROM User WHERE Name='John'
	@Test
	public void testSimpleQuery() {
		Q<Object> q = SELECTㅤ(User::id).ㅤFROMㅤ(User.class).ㅤWHEREㅤ(User::Name).ㅤᆖㅤ("John");
		assertEquals("select id from User where Name = 'John'", q.getHql());
	}

	// WITH activeUsers AS (...) SELECT a.name FROM activeUsers a ORDER BY a.name
	@Test
	public void testCteSimple() {
		Q<Object> q = WITH("activeUsers",
				SELECTㅤ(col(User::id).ㅤAS("id"), col(User::name).ㅤAS("name")).ㅤFROMㅤ(User.class).ㅤAS("u")
						.ㅤWHEREㅤ(User::active).ㅤᆖㅤ(true))
				.SELECTㅤ(col("a", "name")).FROM("activeUsers").ㅤAS("a").ㅤORDERㅤBYㅤ(col("a", "name"));
		assertEquals("with activeUsers as (select u.id as id, u.name as name from User u "
				+ "where u.active = true) select a.name from activeUsers a order by a.name", q.getHql());
	}

	// WITH recentOrders AS (...), bigSpenders AS (...) SELECT ... FROM bigSpenders
	// b JOIN Customer c ...
	@Test
	public void testCteMultiple() {
		Q<Object> q = WITH("recentOrders",
				SELECTㅤ(col(Order::id).ㅤAS("id"), col(Order::customerId).ㅤAS("customerId")).ㅤFROMㅤ(Order.class).ㅤAS("o")
						.ㅤWHEREㅤ(Order::createdAt).ㅤᐳㅤ(param("since")))
				.WITH("bigSpenders", SELECTㅤ(col(Order::customerId).ㅤAS("customerId"), SUM(Order::total).ㅤAS("total"))
						.FROM("recentOrders").ㅤAS("r").JOIN(Order.class).ㅤAS("o").ㅤONㅤ(Order::id).ㅤᆖㅤ(col("r", "id"))
						.GROUPㅤBY(Order::customerId).HAVING(SUM(Order::total)).ㅤᐳㅤ(param("threshold")))
				.SELECTㅤ(Customer::name, col("b", "total")).FROM("bigSpenders").ㅤAS("b").JOIN(Customer.class).ㅤAS("c")
				.ㅤONㅤ(Customer::id).ㅤᆖㅤ(col("b", "customerId")).ㅤORDERㅤBYㅤ("b", "total").DESC();
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
		Q<Object> q = WITHㅤRECURSIVE("empHierarchy",
				SELECTㅤ(col(Employee::id).ㅤAS("id"), col(Employee::managerId).ㅤAS("managerId"), lit(0).ㅤAS("depth"))
						.ㅤFROMㅤ(Employee.class).ㅤAS("e").ㅤWHEREㅤ(Employee::managerId).ISㅤNULL()
						.UNIONㅤALL(SELECTㅤ(col(Employee::id).ㅤAS("id"), col(Employee::managerId).ㅤAS("managerId"),
								col("h", "depth").ᐩ(1).ㅤAS("depth")).ㅤFROMㅤ(Employee.class).ㅤAS("e").JOIN("empHierarchy")
								.ㅤAS("h").ㅤONㅤ(Employee::managerId).ㅤᆖㅤ(col("h", "id"))))
				.SELECTㅤ(col("h", "id"), col("h", "depth")).FROM("empHierarchy").ㅤAS("h")
				.ㅤORDERㅤBYㅤ(col("h", "depth"), col("h", "id"));
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
		Q<Order> q = SELECTㅤ(Order.class).ㅤFROMㅤ(Order.class).ㅤAS("o").ㅤWHEREㅤ(Order::total)
				.ㅤᆖㅤ(SELECTㅤ(MAX(Order::total)).ㅤFROMㅤ(Order.class).ㅤAS("o2").ㅤWHEREㅤ(Order::customerId).ㅤᆖㅤ("o",
						Order::customerId));
		assertEquals("select o from Order o where o.total = "
				+ "(select max(o2.total) from Order o2 where o2.customerId = o.customerId)", q.getHql());
	}

	// SELECT c FROM Customer c WHERE EXISTS (SELECT 1 FROM Order o WHERE
	// o.customerId = c.id AND o.status = 'PAID')
	@Test
	public void testExistsSubquery() {
		Q<Customer> q = SELECTㅤ(Customer.class).ㅤFROMㅤ(Customer.class).ㅤAS("c")
				.ㅤWHEREㅤ(ㅤEXISTSㅤ(SELECTㅤ(lit(1)).ㅤFROMㅤ(Order.class).ㅤAS("o").ㅤWHEREㅤ(Order::customerId)
						.ㅤᆖㅤ("c", Customer::id).ㅤANDㅤ(Order::status).ㅤᆖㅤ("PAID")));
		assertEquals("select c from Customer c where exists "
				+ "(select 1 from Order o where o.customerId = c.id and o.status = 'PAID')", q.getHql());
	}

	// SELECT p FROM Product p WHERE p.categoryId IN (SELECT cat.id FROM Category
	// cat WHERE cat.parentId = :parent)
	@Test
	public void testInSubquery() {
		Q<Product> q = SELECTㅤ(Product.class).ㅤFROMㅤ(Product.class).ㅤAS("p").ㅤWHEREㅤ(Product::categoryId)
				.IN(SELECTㅤ(Category::id).ㅤFROMㅤ(Category.class).ㅤAS("cat").ㅤWHEREㅤ(Category::parentId)
						.ㅤᆖㅤ(param("parent")));
		assertEquals("select p from Product p where p.categoryId in "
				+ "(select cat.id from Category cat where cat.parentId = :parent)", q.getHql());
	}

	// SELECT o.customerId, COUNT(o.id), SUM(o.total) FROM Order o WHERE o.status <>
	// 'CANCELLED' GROUP BY ... HAVING ... ORDER BY ...
	@Test
	public void testGroupByHaving() {
		Q<Object> q = SELECTㅤ(Order::customerId, COUNT(Order::id), SUM(Order::total)).ㅤFROMㅤ(Order.class).ㅤAS("o")
				.ㅤWHEREㅤ(Order::status).ᐸᐳ("CANCELLED").GROUPㅤBY(Order::customerId)
				.HAVING(ㅤᐳㅤ(COUNT(Order::id), 5).ㅤANDㅤ(SUM(Order::total)).ㅤᐳㅤ(1000))
				.ㅤORDERㅤBYㅤ(SUM(Order::total).DESC());
		assertEquals("select o.customerId, count(o.id), sum(o.total) from Order o "
				+ "where o.status <> 'CANCELLED' group by o.customerId "
				+ "having count(o.id) > 5 and sum(o.total) > 1000 order by sum(o.total) desc", q.getHql());
	}

	// SELECT DISTINCT c FROM Customer c LEFT JOIN FETCH c.orders o LEFT JOIN FETCH
	// o.items WHERE c.country = :country
	@Test
	public void testJoinFetch() {
		Q<Customer> q = SELECTㅤ(DISTINCTㅤ(Customer.class)).ㅤFROMㅤ(Customer.class).ㅤAS("c")
				.LEFTㅤJOINㅤFETCH(Customer::orders).ㅤAS("o").LEFTㅤJOINㅤFETCH(col("o", "items"))
				.ㅤWHEREㅤ(Customer::country).ㅤᆖㅤ(param("country"));
		assertEquals("select distinct c from Customer c left join fetch c.orders o "
				+ "left join fetch o.items where c.country = :country", q.getHql());
	}

	// SELECT o.id, c.name, p.title FROM Order o JOIN Customer c ON ... JOIN
	// OrderItem oi ON ... JOIN Product p ON ...
	@Test
	public void testMultipleJoinsWithOn() {
		Q<Object> q = SELECTㅤ(Order::id, col(Customer::name), col(Product::title)).ㅤFROMㅤ(Order.class).ㅤAS("o")
				.JOIN(Customer.class).ㅤAS("c").ㅤONㅤ(Customer::id).ㅤᆖㅤ("o", Order::customerId).JOIN(OrderItem.class)
				.ㅤAS("oi").ㅤONㅤ(OrderItem::orderId).ㅤᆖㅤ("o", Order::id).JOIN(Product.class).ㅤAS("p").ㅤONㅤ(Product::id)
				.ㅤᆖㅤ("oi", OrderItem::productId).ㅤWHEREㅤ(Product::price).ㅤᐳㅤ(param("minPrice"));
		assertEquals("select o.id, c.name, p.title from Order o "
				+ "join Customer c on c.id = o.customerId join OrderItem oi on oi.orderId = o.id "
				+ "join Product p on p.id = oi.productId where p.price > :minPrice", q.getHql());
	}

	// SELECT o.id, CASE WHEN ... THEN ... ELSE ... END FROM Order o
	@Test
	public void testCaseWhen() {
		Q<Object> q = SELECTㅤ(Order::id, CASE().WHEN(ㅤᐳᆖㅤ(Order::total, 1000)).THEN("GOLD")
				.WHEN(ㅤᐳᆖㅤ(Order::total, 100)).THEN("SILVER").ELSE("BRONZE").END()).ㅤFROMㅤ(Order.class).ㅤAS("o");
		assertEquals("select o.id, case when o.total >= 1000 then 'GOLD' "
				+ "when o.total >= 100 then 'SILVER' else 'BRONZE' end from Order o", q.getHql());
	}

	// SELECT o.customerId, o.id, o.total, ROW_NUMBER() OVER (PARTITION BY ... ORDER
	// BY ... DESC) AS rn FROM Order o
	@Test
	public void testWindowFunction() {
		Q<Object> q = SELECTㅤ(Order::customerId, col(Order::id), col(Order::total),
				ROW_NUMBER().OVER(ㅤPARTITIONㅤBYㅤ(Order::customerId).ORDERㅤBY(Order::total).DESC()).ㅤAS("rn"))
				.ㅤFROMㅤ(Order.class).ㅤAS("o");
		assertEquals(
				"select o.customerId, o.id, o.total, "
						+ "row_number() over (partition by o.customerId order by o.total desc) as rn from Order o",
				q.getHql());
	}

	// SELECT u.email FROM User u WHERE u.active = true UNION ALL SELECT s.email
	// FROM Supplier s WHERE s.preferred = true
	@Test
	public void testUnionAll() {
		Q<Object> q = SELECTㅤ(col(User::email).ㅤAS("contact")).ㅤFROMㅤ(User.class).ㅤAS("u").ㅤWHEREㅤ(User::active).ㅤᆖㅤ(true)
				.UNIONㅤALL(SELECTㅤ(col(Supplier::email).ㅤAS("contact")).ㅤFROMㅤ(Supplier.class).ㅤAS("s")
						.ㅤWHEREㅤ(Supplier::preferred).ㅤᆖㅤ(true));
		assertEquals("select u.email as contact from User u where u.active = true union all "
				+ "select s.email as contact from Supplier s where s.preferred = true", q.getHql());
	}

	// SELECT NEW CustomerSummary(c.id, c.name, COUNT(o.id)) FROM Customer c LEFT
	// JOIN c.orders o GROUP BY c.id, c.name
	@Test
	public void testConstructorExpression() {
		Q<Object> q = SELECTㅤ(NEW(CustomerSummary.class, Customer::id, col(Customer::name), COUNT(col("o", Order::id))))
				.ㅤFROMㅤ(Customer.class).ㅤAS("c").LEFTㅤJOIN(Customer::orders).ㅤAS("o")
				.GROUPㅤBY(Customer::id, Customer::name);
		assertEquals("select new linqava.CustomerSummary(c.id, c.name, count(o.id)) from Customer c "
				+ "left join c.orders o group by c.id, c.name", q.getHql());
	}

	// SELECT c FROM Customer c WHERE :product MEMBER OF c.wishlist AND c.orders IS
	// NOT EMPTY AND SIZE(c.orders) > 3
	@Test
	public void testCollectionMemberAndEmpty() {
		Q<Customer> q = SELECTㅤ(Customer.class).ㅤFROMㅤ(Customer.class).ㅤAS("c")
				.ㅤWHEREㅤ(ㅤMEMBERㅤOFㅤ(param("product"), Customer::wishlist).ㅤANDㅤ().ISㅤNOTㅤEMPTY(Customer::orders)
						.ㅤANDㅤ(SIZE(Customer::orders)).ㅤᐳㅤ(3));
		assertEquals("select c from Customer c where :product member of c.wishlist "
				+ "and c.orders is not empty and size(c.orders) > 3", q.getHql());
	}

	// SELECT p FROM Payment p WHERE TREAT(p AS CreditCardPayment).cardType =
	// :cardType OR TREAT(...).iban LIKE :ibanPrefix
	@Test
	public void testTreatPolymorphism() {
		Q<Payment> q = SELECTㅤ(Payment.class).ㅤFROMㅤ(Payment.class).ㅤAS("p")
				.ㅤWHEREㅤ(ㅤTREATㅤ(Payment.class, CreditCardPayment.class).ᐧ(CreditCardPayment::cardType))
				.ㅤᆖㅤ(param("cardType"))
				.ㅤORㅤ(ㅤTREATㅤ(Payment.class, BankTransferPayment.class).ᐧ(BankTransferPayment::iban))
				.LIKE(param("ibanPrefix"));
		assertEquals("select p from Payment p where treat(p as CreditCardPayment).cardType = :cardType "
				+ "or treat(p as BankTransferPayment).iban like :ibanPrefix", q.getHql());
	}

	// WITH rankedOrders AS (... RANK() OVER (...) AS rnk ...) SELECT r.customerId,
	// r.id, r.total FROM rankedOrders r WHERE r.rnk = 1
	@Test
	public void testCteWithWindowFunction() {
		Q<Object> q = WITH("rankedOrders",
				SELECTㅤ(col(Order::id).ㅤAS("id"), col(Order::customerId).ㅤAS("customerId"), col(Order::total).ㅤAS("total"),
						RANK().OVER(ㅤPARTITIONㅤBYㅤ(Order::customerId).ORDERㅤBY(Order::total).DESC()).ㅤAS("rnk"))
						.ㅤFROMㅤ(Order.class).ㅤAS("o"))
				.SELECTㅤ(col("r", "customerId"), col("r", "id"), col("r", "total")).FROM("rankedOrders").ㅤAS("r")
				.ㅤWHEREㅤ(col("r", "rnk")).ㅤᆖㅤ(1);
		assertEquals("with rankedOrders as (select o.id as id, o.customerId as customerId, o.total as total, "
				+ "rank() over (partition by o.customerId order by o.total desc) as rnk from Order o) "
				+ "select r.customerId, r.id, r.total from rankedOrders r where r.rnk = 1", q.getHql());
	}

	// SELECT c.name, (SELECT COUNT(o.id) FROM Order o WHERE o.customerId = c.id) AS
	// orderCount FROM Customer c ORDER BY orderCount DESC
	@Test
	public void testScalarSubqueryInSelect() {
		Q<Object> q = SELECTㅤ(Customer::name,
				sub(SELECTㅤ(COUNT(Order::id)).ㅤFROMㅤ(Order.class).ㅤAS("o").ㅤWHEREㅤ(Order::customerId).ㅤᆖㅤ("c",
						Customer::id)).ㅤAS("orderCount"))
				.ㅤFROMㅤ(Customer.class).ㅤAS("c").ㅤORDERㅤBYㅤ(col("orderCount").DESC());
		assertEquals("select c.name, (select count(o.id) from Order o where o.customerId = c.id) as orderCount "
				+ "from Customer c order by orderCount desc", q.getHql());
	}

	// SELECT o.customerId, SUM(CASE WHEN ... END) AS paidTotal,
	// COALESCE(AVG(NULLIF(...)), 0) AS avgDiscount FROM Order o GROUP BY ...
	@Test
	public void testNestedCaseInAggregateWithCoalesce() {
		Q<Object> q = SELECTㅤ(Order::customerId,
				SUM(CASE().WHEN(ㅤᆖㅤ(Order::status, "PAID")).THEN(Order::total).ELSE(0).END()).ㅤAS("paidTotal"),
				COALESCE(AVG(NULLIF(Order::discount, 0)), 0).ㅤAS("avgDiscount")).ㅤFROMㅤ(Order.class).ㅤAS("o")
				.GROUPㅤBY(Order::customerId);
		assertEquals(
				"select o.customerId, sum(case when o.status = 'PAID' then o.total else 0 end) as paidTotal, "
						+ "coalesce(avg(nullif(o.discount, 0)), 0) as avgDiscount from Order o group by o.customerId",
				q.getHql());
	}

	// SELECT o FROM Order o WHERE status = 'PAID' AND (total > 100 OR (discount < 5
	// AND (customerId = 42 OR (total >= 1000 AND discount <= 50))))
	@Test
	public void testDeeplyNestedAndOr() {
		Q<Order> q = SELECTㅤ(Order.class).ㅤFROMㅤ(Order.class).ㅤAS("o")
				.ㅤWHEREㅤ(ㅤANDㅤ(ㅤᆖㅤ(Order::status, "PAID"),
						ㅤORㅤ(ㅤᐳㅤ(Order::total, 100), ㅤANDㅤ(ㅤᐸㅤ(Order::discount, 5), ㅤORㅤ(ㅤᆖㅤ(Order::customerId, 42),
								ㅤANDㅤ(ㅤᐳᆖㅤ(Order::total, 1000), ㅤᐸᆖㅤ(Order::discount, 50)))))));
		assertEquals(
				"select o from Order o where (o.status = 'PAID' and (o.total > 100 or "
						+ "(o.discount < 5 and (o.customerId = 42 or (o.total >= 1000 and o.discount <= 50)))))",
				q.getHql());
	}

	// via(EntityManager) on a single-entity query returns a typed List<Order>.
	@Test
	public void testViaSingleEntity() {
		Q<Order> q = SELECTㅤ(Order.class).ㅤFROMㅤ(Order.class).ㅤAS("o");
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
		Q<Customer> q = SELECTㅤ(DISTINCTㅤ(Customer.class)).ㅤFROMㅤ(Customer.class).ㅤAS("c");
		String[] capturedHql = new String[1];
		EntityManager em = fakeEntityManager(capturedHql, Collections.singletonList(new Customer()));

		List<Customer> result = q.via(em);

		assertEquals("select distinct c from Customer c", capturedHql[0]);
		assertEquals(1, result.size());
	}

	// via(EntityManager) rejects scalar/tuple projections.
	@Test(expected = IllegalStateException.class)
	public void testViaRejectsProjection() {
		Q<Object> q = SELECTㅤ(Order::id).ㅤFROMㅤ(Order.class).ㅤAS("o");
		q.via(fakeEntityManager(new String[1], Collections.emptyList()));
	}

	// SELECT c FROM Car c WHERE c.driver.id > 0 AND c.plate.id > 0
	@Test
	public void testComplex1() {
		Q<Car> q = SELECTㅤ(Car.class).ㅤFROMㅤ(Car.class).ㅤAS("c").ㅤWHEREㅤ(Car::driver).ㅤᐅㅤ(Driver::id).ㅤᐳㅤ(0)
				.ㅤANDㅤ(Car::plate).ㅤᐅㅤ(SerialPlate::id).ㅤᐳㅤ(0);
		assertEquals("select c from Car c where c.driver.id > 0 and c.plate.id > 0", q.getHql());
	}// SELECT c FROM Car c WHERE c.driver.id > 0 AND c.plate.id > 0

	@Test
	public void testComplex2() {
		Q<Driver> q = SELECTㅤ(Driver.class).ㅤFROMㅤ(Driver.class).ㅤWHEREㅤ(col(Driver::id)).ㅤᐳㅤ(0);
		assertEquals("select Driver from Driver where id > 0", q.getHql());
	}

	@Test
	public void testComplex3() {
		Q<Driver> q = SELECTㅤ(Driver.class).ㅤFROMㅤ(Driver.class).ㅤWHEREㅤ(Driver::id).ㅤᐳㅤ(0);
		assertEquals("select Driver from Driver where id > 0", q.getHql());
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
