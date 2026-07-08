/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _ 
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package pro.peter_rader.linqava;

import static org.junit.Assert.assertEquals;
import static pro.peter_rader.linqava.Linq.*;
;

import org.junit.Test;

/**
 * Verifies that the HQL produced by {@link Q#getUnsafeHql()} is correct for each
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
 * {@code CASE...THEN}, ...) accept them bare. To alias a column in a
 * {@code SELECT} list, pass it as a {@code (getter, alias)} pair directly to
 * {@code SELECT(...)} instead of chaining {@code .AS(...)} onto a wrapped
 * column, e.g. {@code SELECT(User::id, "id", User::name, "name")}. Derived/CTE
 * columns with no getter use the {@code (alias, field)} string-pair overloads
 * instead, e.g. {@code SELECT("a", "name")}.</li>
 * <li>The {@code AS} keyword aliases tables ({@code FROM(User.class).AS("u")}).</li>
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

	@Test
	public void testGetterPrefixIsStrippedAndDecapitalized() {
		Q<Object> q = SELECTㅤ(LegacyBean::getName).ㅤFROMㅤ(LegacyBean.class).ㅤWHEREㅤ(LegacyBean::isActive).ㅤᆖㅤ(true);
		assertEquals("select name from pro.peter_rader.linqava.LegacyBean where active = true", q.getUnsafeHql());
	}

	@Test
	public void testGetterPrefixWithLeadingAcronymIsKeptAsIs() {
		Q<Object> q = SELECTㅤ(LegacyBean::getURLName).ㅤFROMㅤ(LegacyBean.class);
		assertEquals("select URLName from pro.peter_rader.linqava.LegacyBean", q.getUnsafeHql());
	}

	@Test
	public void testSelectFromShorthandRendersWithoutAlias() {
		EntityQ<User> shorthand = SELECTㅤꁘㅤFROM(User.class);
		assertEquals("from pro.peter_rader.linqava.User", shorthand.getUnsafeHql());
	}

	@Test
	public void testSelectFromShorthandWhereRendersUnqualifiedProperty() {
		EntityQ<User> shorthand = SELECTㅤꁘㅤFROM(User.class).ㅤWHEREㅤ(User::Name).ㅤᆖㅤ("John");
		assertEquals("from pro.peter_rader.linqava.User where Name = 'John'", shorthand.getUnsafeHql());
	}

	@Test
	public void testSelectFromShorthandUnionAllRequiresSameEntityType() {
		EntityQ<User> q = SELECTㅤꁘㅤFROM(User.class).ㅤWHEREㅤ(User::active).ㅤᆖㅤ(true)
				.UNIONㅤALL(SELECTㅤꁘㅤFROM(User.class).ㅤWHEREㅤ(User::active).ㅤᆖㅤ(false));
		assertEquals("from pro.peter_rader.linqava.User where active = true union all from pro.peter_rader.linqava.User where active = false",
				q.getUnsafeHql());
	}

	// SELECT id FROM User WHERE Name='John'
	@Test
	public void testCuteQuery() {
		Q<Object> q = SELECTㅤ(User::id).AS("idx").ㅤFROMㅤ(User.class);
		assertEquals("select id as idx from pro.peter_rader.linqava.User", q.getUnsafeHql());
	}

	// SELECT id as id FROM User — the lower bound of the (getter, alias)-pair overload set (1 pair).
	@Test
	public void testSelectWithOneAliasedColumnPair() {
		Q<Object> q = SELECTㅤ(User::id, "id").ㅤFROMㅤ(User.class);
		assertEquals("select id as id from pro.peter_rader.linqava.User", q.getUnsafeHql());
	}

	// SELECT id as id, name as name FROM User — (getter, alias) pairs directly on SELECT, no
	// typedCol(...)/col(...) wrapping needed anywhere.
	@Test
	public void testSelectWithAliasedColumnPairs() {
		Q<Object> q = SELECTㅤ(User::id, "id", User::name, "name").ㅤFROMㅤ(User.class);
		assertEquals("select id as id, name as name from pro.peter_rader.linqava.User", q.getUnsafeHql());
	}

	// Same shape with three pairs, to confirm the overload set scales past the first arity.
	@Test
	public void testSelectWithThreeAliasedColumnPairs() {
		Q<Object> q = SELECTㅤ(Order::id, "id", Order::customerId, "customerId", Order::total, "total")
				.ㅤFROMㅤ(Order.class);
		assertEquals("select id as id, customerId as customerId, total as total from pro.peter_rader.linqava.Order", q.getUnsafeHql());
	}

	// The upper bound of the (getter, alias)-pair overload set (20 pairs) — reuses the same getter
	// under 20 distinct aliases, since the point is to exercise the overload itself, not 20 distinct
	// real columns.
	@Test
	public void testSelectWithTwentyAliasedColumnPairs() {
		Q<Object> q = SELECTㅤ(
				Order::id, "c1", Order::id, "c2", Order::id, "c3", Order::id, "c4", Order::id, "c5",
				Order::id, "c6", Order::id, "c7", Order::id, "c8", Order::id, "c9", Order::id, "c10",
				Order::id, "c11", Order::id, "c12", Order::id, "c13", Order::id, "c14", Order::id, "c15",
				Order::id, "c16", Order::id, "c17", Order::id, "c18", Order::id, "c19", Order::id, "c20")
				.ㅤFROMㅤ(Order.class);
		StringBuilder expected = new StringBuilder("select ");
		for (int i = 1; i <= 20; i++) {
			if (i > 1) {
				expected.append(", ");
			}
			expected.append("id as c").append(i);
		}
		expected.append(" from pro.peter_rader.linqava.Order");
		assertEquals(expected.toString(), q.getUnsafeHql());
	}

	// SELECT id FROM User WHERE Name='John'
	@Test
	public void testSimpleQuery() {
		Q<Object> q = SELECTㅤ(User::id).ㅤFROMㅤ(User.class).ㅤWHEREㅤ(User::Name).ㅤᆖㅤ("John");
		assertEquals("select id from pro.peter_rader.linqava.User where Name = 'John'", q.getUnsafeHql());
	}

	// WITH activeUsers AS (...) SELECT a.name FROM activeUsers a ORDER BY a.name
	@Test
	public void testCteSimple() {
		Q<Object> q = WITH("activeUsers",
				SELECTㅤ(User::id, "id", User::name, "name").ㅤFROMㅤ(User.class).ㅤAS("u")
						.ㅤWHEREㅤ(User::active).ㅤᆖㅤ(true))
				.SELECTㅤ("a", "name").FROM("activeUsers").ㅤAS("a").ㅤORDERㅤBYㅤ("a", "name").ASC();
		assertEquals("with activeUsers as (select u.id as id, u.name as name from pro.peter_rader.linqava.User u "
				+ "where u.active = true) select a.name from activeUsers a order by a.name asc", q.getUnsafeHql());
	}

	// WITH recentOrders AS (...), bigSpenders AS (...) SELECT ... FROM bigSpenders
	// b JOIN Customer c ...
	@Test
	public void testCteMultiple() {
		Q<Object> q = WITH("recentOrders",
				SELECTㅤ(Order::id, "id", Order::customerId, "customerId").ㅤFROMㅤ(Order.class).ㅤAS("o")
						.ㅤWHEREㅤ(Order::createdAt).ㅤᐳㅤ(param("since")))
				.WITH("bigSpenders", SELECTㅤ(Order::customerId, "customerId", SUM(Order::total).ㅤAS("total"))
						.FROM("recentOrders").ㅤAS("r").JOIN(Order.class).ㅤAS("o").ㅤONㅤ(Order::id).ㅤᆖㅤ("r", "id")
						.GROUPㅤBY(Order::customerId).HAVING(SUM(Order::total)).ㅤᐳㅤ(param("threshold")))
				.SELECTㅤ(Customer::name, "b", "total").FROM("bigSpenders").ㅤAS("b").JOIN(Customer.class).ㅤAS("c")
				.ㅤONㅤ(Customer::id).ㅤᆖㅤ("b", "customerId").ㅤORDERㅤBYㅤ("b", "total").DESC();
		assertEquals("with recentOrders as (select o.id as id, o.customerId as customerId from pro.peter_rader.linqava.Order o "
				+ "where o.createdAt > :since), bigSpenders as (select o.customerId as customerId, "
				+ "sum(o.total) as total from recentOrders r join pro.peter_rader.linqava.Order o on o.id = r.id "
				+ "group by o.customerId having sum(o.total) > :threshold) "
				+ "select c.name, b.total from bigSpenders b join pro.peter_rader.linqava.Customer c on c.id = b.customerId "
				+ "order by b.total desc", q.getUnsafeHql());
	}

	// WITH RECURSIVE empHierarchy AS (anchor UNION ALL recursive) SELECT h.id,
	// h.depth ...
	@Test
	public void testRecursiveCte() {
		Q<Object> q = WITHㅤRECURSIVE("empHierarchy",
				SELECTㅤ(Employee::id, "id", Employee::managerId, "managerId", lit(0).ㅤAS("depth"))
						.ㅤFROMㅤ(Employee.class).ㅤAS("e").ㅤWHEREㅤ(Employee::managerId).ISㅤNULL()
						.UNIONㅤALL(SELECTㅤ(Employee::id, "id", Employee::managerId, "managerId",
								ᐩ("h", "depth", 1).ㅤAS("depth")).ㅤFROMㅤ(Employee.class).ㅤAS("e")
								.JOIN("empHierarchy").ㅤAS("h").ㅤONㅤ(Employee::managerId).ㅤᆖㅤ("h", "id")))
				.SELECTㅤ("h", "id", "h", "depth").FROM("empHierarchy").ㅤAS("h")
				.ㅤORDERㅤBYㅤ("h", "depth", "h", "id");
		assertEquals("with recursive empHierarchy as (select e.id as id, e.managerId as managerId, 0 as depth "
				+ "from pro.peter_rader.linqava.Employee e where e.managerId is null union all "
				+ "select e.id as id, e.managerId as managerId, h.depth + 1 as depth "
				+ "from pro.peter_rader.linqava.Employee e join empHierarchy h on e.managerId = h.id) "
				+ "select h.id, h.depth from empHierarchy h order by h.depth, h.id", q.getUnsafeHql());
	}

	// SELECT o FROM Order o WHERE o.total = (SELECT MAX(o2.total) FROM Order o2
	// WHERE o2.customerId = o.customerId)
	@Test
	public void testCorrelatedSubquery() {
		Q<Order> q = SELECTㅤ(Order.class).ㅤFROMㅤ(Order.class).ㅤAS("o").ㅤWHEREㅤ(Order::total)
				.ㅤᆖㅤ(SELECTㅤ(MAX(Order::total)).ㅤFROMㅤ(Order.class).ㅤAS("o2").ㅤWHEREㅤ(Order::customerId).ㅤᆖㅤ("o",
						Order::customerId));
		assertEquals("select o from pro.peter_rader.linqava.Order o where o.total = "
				+ "(select max(o2.total) from pro.peter_rader.linqava.Order o2 where o2.customerId = o.customerId)", q.getUnsafeHql());
	}

	// SELECT c FROM Customer c WHERE EXISTS (SELECT 1 FROM Order o WHERE
	// o.customerId = c.id AND o.status = 'PAID')
	@Test
	public void testExistsSubquery() {
		Q<Customer> q = SELECTㅤ(Customer.class).ㅤFROMㅤ(Customer.class).ㅤAS("c")
				.ㅤWHEREㅤ(ㅤEXISTSㅤ(SELECTㅤ(lit(1)).ㅤFROMㅤ(Order.class).ㅤAS("o").ㅤWHEREㅤ(Order::customerId)
						.ㅤᆖㅤ("c", Customer::id).ㅤANDㅤ(Order::status).ㅤᆖㅤ("PAID")));
		assertEquals("select c from pro.peter_rader.linqava.Customer c where exists "
				+ "(select 1 from pro.peter_rader.linqava.Order o where o.customerId = c.id and o.status = 'PAID')", q.getUnsafeHql());
	}

	// SELECT p FROM Product p WHERE p.categoryId IN (SELECT cat.id FROM Category
	// cat WHERE cat.parentId = :parent)
	@Test
	public void testInSubquery() {
		Q<Product> q = SELECTㅤ(Product.class).ㅤFROMㅤ(Product.class).ㅤAS("p").ㅤWHEREㅤ(Product::categoryId)
				.IN(SELECTㅤ(Category::id).ㅤFROMㅤ(Category.class).ㅤAS("cat").ㅤWHEREㅤ(Category::parentId)
						.ㅤᆖㅤ(param("parent")));
		assertEquals("select p from pro.peter_rader.linqava.Product p where p.categoryId in "
				+ "(select cat.id from pro.peter_rader.linqava.Category cat where cat.parentId = :parent)", q.getUnsafeHql());
	}

	// SELECT o.customerId, COUNT(o.id), SUM(o.total) FROM Order o WHERE o.status <>
	// 'CANCELLED' GROUP BY ... HAVING ... ORDER BY ...
	@Test
	public void testGroupByHaving() {
		Grouped<Object> q = SELECTㅤ(Order::customerId, COUNT(Order::id), SUM(Order::total)).ㅤFROMㅤ(Order.class).ㅤAS("o")
				.ㅤWHEREㅤ(Order::status).ᐸᐳ("CANCELLED").GROUPㅤBY(Order::customerId)
				.HAVING(ㅤᐳㅤ(COUNT(Order::id), 5).ㅤANDㅤ(SUM(Order::total)).ㅤᐳㅤ(1000))
				.ㅤORDERㅤBYㅤ(SUM(Order::total).DESC());
		assertEquals("select o.customerId, count(o.id), sum(o.total) from pro.peter_rader.linqava.Order o "
				+ "where o.status <> 'CANCELLED' group by o.customerId "
				+ "having count(o.id) > 5 and sum(o.total) > 1000 order by sum(o.total) desc", q.getUnsafeHql());
	}

	// SELECT DISTINCT c FROM Customer c LEFT JOIN FETCH c.orders o LEFT JOIN FETCH
	// o.items WHERE c.country = :country
	@Test
	public void testJoinFetch() {
		Q<Customer> q = SELECTㅤ(DISTINCTㅤ(Customer.class)).ㅤFROMㅤ(Customer.class).ㅤAS("c")
				.LEFTㅤJOINㅤFETCH(Customer::orders).ㅤAS("o").LEFTㅤJOINㅤFETCH("o", "items")
				.ㅤWHEREㅤ(Customer::country).ㅤᆖㅤ(param("country"));
		assertEquals("select distinct c from pro.peter_rader.linqava.Customer c left join fetch c.orders o "
				+ "left join fetch o.items where c.country = :country", q.getUnsafeHql());
	}

	// SELECT o.id, c.name, p.title FROM Order o JOIN Customer c ON ... JOIN
	// OrderItem oi ON ... JOIN Product p ON ...
	@Test
	public void testMultipleJoinsWithOn() {
		Q<Object> q = SELECTㅤ(Order::id, Customer::name, Product::title).ㅤFROMㅤ(Order.class).ㅤAS("o")
				.JOIN(Customer.class).ㅤAS("c").ㅤONㅤ(Customer::id).ㅤᆖㅤ("o", Order::customerId).JOIN(OrderItem.class)
				.ㅤAS("oi").ㅤONㅤ(OrderItem::orderId).ㅤᆖㅤ("o", Order::id).JOIN(Product.class).ㅤAS("p").ㅤONㅤ(Product::id)
				.ㅤᆖㅤ("oi", OrderItem::productId).ㅤWHEREㅤ(Product::price).ㅤᐳㅤ(param("minPrice"));
		assertEquals("select o.id, c.name, p.title from pro.peter_rader.linqava.Order o "
				+ "join pro.peter_rader.linqava.Customer c on c.id = o.customerId join pro.peter_rader.linqava.OrderItem oi on oi.orderId = o.id "
				+ "join pro.peter_rader.linqava.Product p on p.id = oi.productId where p.price > :minPrice", q.getUnsafeHql());
	}

	// SELECT o.id, CASE WHEN ... THEN ... ELSE ... END FROM Order o
	@Test
	public void testCaseWhen() {
		Q<Object> q = SELECTㅤ(Order::id, CASE().WHEN(ㅤᐳᆖㅤ(Order::total, 1000)).THEN("GOLD")
				.WHEN(ㅤᐳᆖㅤ(Order::total, 100)).THEN("SILVER").ELSE("BRONZE").END()).ㅤFROMㅤ(Order.class).ㅤAS("o");
		assertEquals("select o.id, case when o.total >= 1000 then 'GOLD' "
				+ "when o.total >= 100 then 'SILVER' else 'BRONZE' end from pro.peter_rader.linqava.Order o", q.getUnsafeHql());
	}

	// SELECT o.customerId, o.id, o.total, ROW_NUMBER() OVER (PARTITION BY ... ORDER
	// BY ... DESC) AS rn FROM Order o
	@Test
	public void testWindowFunction() {
		Q<Object> q = SELECTㅤ(Order::customerId, Order::id, Order::total,
				ROW_NUMBER().OVER(ㅤPARTITIONㅤBYㅤ(Order::customerId).ORDERㅤBY(Order::total).DESC()).ㅤAS("rn"))
				.ㅤFROMㅤ(Order.class).ㅤAS("o");
		assertEquals(
				"select o.customerId, o.id, o.total, "
						+ "row_number() over (partition by o.customerId order by o.total desc) as rn from pro.peter_rader.linqava.Order o",
				q.getUnsafeHql());
	}

	// SELECT u.email FROM User u WHERE u.active = true UNION ALL SELECT s.email
	// FROM Supplier s WHERE s.preferred = true
	@Test
	public void testUnionAll() {
		Q<Object> q = SELECTㅤ(User::email, "contact").ㅤFROMㅤ(User.class).ㅤAS("u").ㅤWHEREㅤ(User::active)
				.ㅤᆖㅤ(true).UNIONㅤALL(SELECTㅤ(Supplier::email, "contact").ㅤFROMㅤ(Supplier.class).ㅤAS("s")
						.ㅤWHEREㅤ(Supplier::preferred).ㅤᆖㅤ(true));
		assertEquals("select u.email as contact from pro.peter_rader.linqava.User u where u.active = true union all "
				+ "select s.email as contact from pro.peter_rader.linqava.Supplier s where s.preferred = true", q.getUnsafeHql());
	}

	// SELECT NEW CustomerSummary(c.id, c.name, COUNT(o.id)) FROM Customer c LEFT
	// JOIN c.orders o GROUP BY c.id, c.name
	@Test
	public void testConstructorExpression() {
		Grouped<Object> q = SELECTㅤ(NEW(CustomerSummary.class, Customer::id, Customer::name, COUNT("o", Order::id)))
				.ㅤFROMㅤ(Customer.class).ㅤAS("c").LEFTㅤJOIN(Customer::orders).ㅤAS("o")
				.GROUPㅤBY(Customer::id, Customer::name);
		assertEquals("select new pro.peter_rader.linqava.CustomerSummary(c.id, c.name, count(o.id)) from pro.peter_rader.linqava.Customer c "
				+ "left join c.orders o group by c.id, c.name", q.getUnsafeHql());
	}

	// SELECT c FROM Customer c WHERE :product MEMBER OF c.wishlist AND c.orders IS
	// NOT EMPTY AND SIZE(c.orders) > 3
	@Test
	public void testCollectionMemberAndEmpty() {
		Q<Customer> q = SELECTㅤ(Customer.class).ㅤFROMㅤ(Customer.class).ㅤAS("c")
				.ㅤWHEREㅤ(ㅤMEMBERㅤOFㅤ(param("product"), Customer::wishlist).ㅤANDㅤ().ISㅤNOTㅤEMPTY(Customer::orders)
						.ㅤANDㅤ(SIZE(Customer::orders)).ㅤᐳㅤ(3));
		assertEquals("select c from pro.peter_rader.linqava.Customer c where :product member of c.wishlist "
				+ "and c.orders is not empty and size(c.orders) > 3", q.getUnsafeHql());
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
		assertEquals("select p from pro.peter_rader.linqava.Payment p where treat(p as pro.peter_rader.linqava.CreditCardPayment).cardType = :cardType "
				+ "or treat(p as pro.peter_rader.linqava.BankTransferPayment).iban like :ibanPrefix", q.getUnsafeHql());
	}

	// WITH rankedOrders AS (... RANK() OVER (...) AS rnk ...) SELECT r.customerId,
	// r.id, r.total FROM rankedOrders r WHERE r.rnk = 1
	@Test
	public void testCteWithWindowFunction() {
		Q<Object> q = WITH("rankedOrders",
				SELECTㅤ(Order::id, "id", Order::customerId, "customerId",
						Order::total, "total",
						RANK().OVER(ㅤPARTITIONㅤBYㅤ(Order::customerId).ORDERㅤBY(Order::total).DESC()).ㅤAS("rnk"))
						.ㅤFROMㅤ(Order.class).ㅤAS("o"))
				.SELECTㅤ("r", "customerId", "r", "id", "r", "total").FROM("rankedOrders").ㅤAS("r")
				.ㅤWHEREㅤ("r", "rnk").ㅤᆖㅤ(1);
		assertEquals("with rankedOrders as (select o.id as id, o.customerId as customerId, o.total as total, "
				+ "rank() over (partition by o.customerId order by o.total desc) as rnk from pro.peter_rader.linqava.Order o) "
				+ "select r.customerId, r.id, r.total from rankedOrders r where r.rnk = 1", q.getUnsafeHql());
	}

	// SELECT c.name, (SELECT COUNT(o.id) FROM Order o WHERE o.customerId = c.id) AS
	// orderCount FROM Customer c ORDER BY orderCount DESC
	@Test
	public void testScalarSubqueryInSelect() {
		Q<Object> q = SELECTㅤ(Customer::name,
				sub(SELECTㅤ(COUNT(Order::id)).ㅤFROMㅤ(Order.class).ㅤAS("o").ㅤWHEREㅤ(Order::customerId).ㅤᆖㅤ("c",
						Customer::id)).ㅤAS("orderCount"))
				.ㅤFROMㅤ(Customer.class).ㅤAS("c").ㅤORDERㅤBYㅤ("orderCount").DESC();
		assertEquals("select c.name, (select count(o.id) from pro.peter_rader.linqava.Order o where o.customerId = c.id) as orderCount "
				+ "from pro.peter_rader.linqava.Customer c order by orderCount desc", q.getUnsafeHql());
	}

	// SELECT o.customerId, SUM(CASE WHEN ... END) AS paidTotal,
	// COALESCE(AVG(NULLIF(...)), 0) AS avgDiscount FROM Order o GROUP BY ...
	@Test
	public void testNestedCaseInAggregateWithCoalesce() {
		Grouped<Object> q = SELECTㅤ(Order::customerId,
				SUM(CASE().WHEN(ㅤᆖㅤ(Order::status, "PAID")).THEN(Order::total).ELSE(0).END()).ㅤAS("paidTotal"),
				COALESCE(AVG(NULLIF(Order::discount, 0)), 0).ㅤAS("avgDiscount")).ㅤFROMㅤ(Order.class).ㅤAS("o")
				.GROUPㅤBY(Order::customerId);
		assertEquals(
				"select o.customerId, sum(case when o.status = 'PAID' then o.total else 0 end) as paidTotal, "
						+ "coalesce(avg(nullif(o.discount, 0)), 0) as avgDiscount from pro.peter_rader.linqava.Order o group by o.customerId",
				q.getUnsafeHql());
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
				"select o from pro.peter_rader.linqava.Order o where (o.status = 'PAID' and (o.total > 100 or "
						+ "(o.discount < 5 and (o.customerId = 42 or (o.total >= 1000 and o.discount <= 50)))))",
				q.getUnsafeHql());
	}

	// getUnsafeHql() renders a mixed literal + param(...) WHERE clause: the literal is inlined, the
	// param(...) placeholder is left untouched for the caller to bind. via(EntityManager)'s actual
	// auto-parameterization/fallback-persist/exception behavior is exercised for real against a
	// genuine H2-backed Hibernate EntityManager in pro.peter_rader.linqava.h2.QFullFormBehaviorTest —
	// not here, and not against a mocked/proxied EntityManager.
	@Test
	public void testGetHqlRendersMixedLiteralAndNamedParameter() {
		Q<Order> q = SELECTㅤ(Order.class).ㅤFROMㅤ(Order.class).ㅤAS("o").ㅤWHEREㅤ(Order::status).ㅤᆖㅤ("PAID")
				.ㅤANDㅤ(Order::total).ㅤᐳㅤ(param("minTotal"));
		assertEquals("select o from pro.peter_rader.linqava.Order o where o.status = 'PAID' and o.total > :minTotal", q.getUnsafeHql());
	}


	// SELECT c FROM Car c WHERE c.driver.id > 0 AND c.plate.id > 0
	@Test
	public void testComplex1() {
		Q<Car> q = SELECTㅤ(Car.class).ㅤFROMㅤ(Car.class).ㅤAS("c").ㅤWHEREㅤ(Car::driver).ㅤᐅㅤ(Driver::id).ㅤᐳㅤ(0)
				.ㅤANDㅤ(Car::plate).ㅤᐅㅤ(SerialPlate::id).ㅤᐳㅤ(0);
		assertEquals("select c from pro.peter_rader.linqava.Car c where c.driver.id > 0 and c.plate.id > 0", q.getUnsafeHql());
	}// SELECT c FROM Car c WHERE c.driver.id > 0 AND c.plate.id > 0

	@Test
	public void testComplex2() {
		Q<Driver> q = SELECTㅤ(Driver.class).ㅤFROMㅤ(Driver.class).ㅤWHEREㅤ(Driver::id).ㅤᐳㅤ(0);
		assertEquals("select pro.peter_rader.linqava.Driver from pro.peter_rader.linqava.Driver where id > 0", q.getUnsafeHql());
	}

	@Test
	public void testComplex3() {
		Q<Driver> q = SELECTㅤ(Driver.class).ㅤFROMㅤ(Driver.class).ㅤWHEREㅤ(Driver::id).ㅤᐳㅤ(0);
		assertEquals("select pro.peter_rader.linqava.Driver from pro.peter_rader.linqava.Driver where id > 0", q.getUnsafeHql());
	}

	@Test
	public void testComplex4() {
		Q<Car> q = SELECTㅤ(Car.class).ㅤFROMㅤ(Car.class).ㅤAS("c").ㅤWHEREㅤ("c", Car::driver).ㅤᐅㅤ(Driver::id).ㅤᐳㅤ(0)
				.ㅤANDㅤ("c", Car::plate).ㅤᐅㅤ(SerialPlate::id).ㅤᐳㅤ(0);
		assertEquals("select c from pro.peter_rader.linqava.Car c where c.driver.id > 0 and c.plate.id > 0", q.getUnsafeHql());
	}

	// SELECT COUNT(*) = 0 FROM Order — comparing a ScalarExpr threads Boolean through to ScalarQ<Boolean>
	@Test
	public void testCountStarEqualsZero() {
		ScalarQ<Boolean> q = SELECTㅤ(COUNTㅤꁘ().ㅤᆖㅤ(0)).ㅤFROMㅤ(Order.class);
		assertEquals("select count(*) = 0 from pro.peter_rader.linqava.Order", q.getUnsafeHql());
	}

	// SELECT COUNT(*) = COUNT(o.id) FROM Order o
	@Test
	public void testCountStarEqualsExpressionOperand() {
		ScalarQ<Boolean> q = SELECTㅤ(COUNTㅤꁘ().ㅤᆖㅤ(COUNT(Order::id))).ㅤFROMㅤ(Order.class).ㅤAS("o");
		assertEquals("select count(*) = count(o.id) from pro.peter_rader.linqava.Order o", q.getUnsafeHql());
	}

	// SELECT MAX(ti.version), MAX(ik.version), MAX(s.version), MAX(kw.version), MAX(tv.version) FROM
	// TranslationStack s LEFT JOIN s.translationKeywords kw LEFT JOIN kw.translationValues tv
	// LEFT JOIN s.translationImageKeys ik LEFT JOIN ik.translationImages ti WHERE s.id = :sid
	@Test
	public void testMaxVersionAcrossNestedLeftJoinedAssociations() {
		Q<Object> q = SELECTㅤ(MAX("ti", TranslationImage::version), MAX("ik", TranslationImageKey::version),
				MAX("s", TranslationStack::version), MAX("kw", TranslationKeyword::version),
				MAX("tv", TranslationValue::version)).ㅤFROMㅤ(TranslationStack.class).ㅤAS("s")
				.LEFTㅤJOIN(TranslationStack::translationKeywords).ㅤAS("kw")
				.LEFTㅤJOIN("kw", TranslationKeyword::translationValues).ㅤAS("tv")
				.LEFTㅤJOIN(TranslationStack::translationImageKeys).ㅤAS("ik")
				.LEFTㅤJOIN("ik", TranslationImageKey::translationImages).ㅤAS("ti")
				.ㅤWHEREㅤ(TranslationStack::id).ㅤᆖㅤ(param("sid"));
		assertEquals("select max(ti.version), max(ik.version), max(s.version), max(kw.version), max(tv.version) "
				+ "from pro.peter_rader.linqava.TranslationStack s left join s.translationKeywords kw "
				+ "left join kw.translationValues tv left join s.translationImageKeys ik "
				+ "left join ik.translationImages ti where s.id = :sid", q.getUnsafeHql());
	}

	// SELECT COUNT(*) FROM Order — bare COUNT(*) threads Long through to ScalarQ<Long>
	@Test
	public void testCountStarAloneThreadsLongType() {
		ScalarQ<Long> q = SELECTㅤ(COUNTㅤꁘ()).ㅤFROMㅤ(Order.class);
		assertEquals("select count(*) from pro.peter_rader.linqava.Order", q.getUnsafeHql());
	}

	// SELECT COUNT(*) > 1 AND COUNT(*) < 1 AS x FROM Order WHERE customerId = 1 AND status IS NOT NULL
	// — every comparison/combinator/alias on a ScalarExpr threads Boolean through to ScalarQ<Boolean>
	@Test
	public void testBooleanCombinationOfCountComparisonsThreadsBooleanType() {
		ScalarQ<Boolean> q = SELECTㅤ(COUNTㅤꁘ().ㅤᐳㅤ(1).ㅤANDㅤ(COUNTㅤꁘ()).ㅤᐸㅤ(1).ㅤAS("x")).ㅤFROMㅤ(Order.class)
				.ㅤWHEREㅤ(Order::customerId).ㅤᆖㅤ(1).ㅤANDㅤ(Order::status).ISㅤNOTㅤNULL();
		assertEquals("select count(*) > 1 and count(*) < 1 as x from pro.peter_rader.linqava.Order "
				+ "where customerId = 1 and status is not null", q.getUnsafeHql());
	}

	// SELECT a.id, b.id FROM EMailAddressLocalName a JOIN EMailAddressLocalName b
	// ON (LOWER(a.localName) <> b.localName AND a.localName = b.localName AND a.id <> b.id)
	// WHERE b.id IS NOT NULL — a self-join, disambiguated via alias-qualified columns on both sides
	@Test
	public void testSelfJoinWithCompoundOnConditionAndLower() {
		Q<Object> q = SELECTㅤ("a", EMailAddressLocalName::id, "b", EMailAddressLocalName::id)
				.ㅤFROMㅤ(EMailAddressLocalName.class).ㅤAS("a").JOIN(EMailAddressLocalName.class).ㅤAS("b")
				.ㅤONㅤ(ㅤANDㅤ(
						ㅤᐸᐳㅤ(LOWER("a", EMailAddressLocalName::localName), "b", EMailAddressLocalName::localName),
						ㅤᆖㅤ("a", EMailAddressLocalName::localName, "b", EMailAddressLocalName::localName),
						ㅤᐸᐳㅤ("a", EMailAddressLocalName::id, "b", EMailAddressLocalName::id)))
				.ㅤWHEREㅤ("b", EMailAddressLocalName::id).ISㅤNOTㅤNULL();
		assertEquals("select a.id, b.id from pro.peter_rader.linqava.EMailAddressLocalName a join pro.peter_rader.linqava.EMailAddressLocalName b "
				+ "on (lower(a.localName) <> b.localName and a.localName = b.localName and a.id <> b.id) "
				+ "where b.id is not null", q.getUnsafeHql());
	}

	// SELECT o FROM Order o ORDER BY o.total DESC LIMIT 10 OFFSET 20
	@Test
	public void testLimitAndOffset() {
		Q<Order> q = SELECTㅤ(Order.class).ㅤFROMㅤ(Order.class).ㅤAS("o").ㅤORDERㅤBYㅤ(Order::total).DESC()
				.LIMIT(10).OFFSET(20);
		assertEquals("select o from pro.peter_rader.linqava.Order o order by o.total desc limit 10 offset 20", q.getUnsafeHql());
	}

	// FROM Order ORDER BY total LIMIT 5 OFFSET 10 (EntityQ shorthand)
	@Test
	public void testEntityQLimitAndOffset() {
		EntityQ<Order> q = SELECTㅤꁘㅤFROM(Order.class).ㅤORDERㅤBYㅤ(Order::total).LIMIT(5).OFFSET(10);
		assertEquals("from pro.peter_rader.linqava.Order order by total limit 5 offset 10", q.getUnsafeHql());
	}

	// SELECT LOWER(o.status), UPPER(o.status) FROM Order o
	@Test
	public void testLowerAndUpperColOverloads() {
		Q<Object> q = SELECTㅤ(LOWER(Order::status), UPPER(Order::status)).ㅤFROMㅤ(Order.class).ㅤAS("o");
		assertEquals("select lower(o.status), upper(o.status) from pro.peter_rader.linqava.Order o", q.getUnsafeHql());
	}

	// SELECT CAST(o.total AS String) FROM Order o
	@Test
	public void testCastColOverload() {
		Q<Object> q = SELECTㅤ(ㅤCASTㅤ(Order::total, String.class)).ㅤFROMㅤ(Order.class).ㅤAS("o");
		assertEquals("select cast(o.total as String) from pro.peter_rader.linqava.Order o", q.getUnsafeHql());
	}

	// SELECT CAST(COUNT(o.id) AS String) FROM Order o
	@Test
	public void testCastOfExpressionOperand() {
		Q<Object> q = SELECTㅤ(ㅤCASTㅤ(COUNT(Order::id), String.class)).ㅤFROMㅤ(Order.class).ㅤAS("o");
		assertEquals("select cast(count(o.id) as String) from pro.peter_rader.linqava.Order o", q.getUnsafeHql());
	}

	// SELECT DISTINCT o.customerId FROM Order o — DISTINCT(alias, getter) alias-qualified shorthand
	@Test
	public void testDistinctAliasColShorthand() {
		Q<Object> q = SELECTㅤ(DISTINCT("o", Order::customerId)).ㅤFROMㅤ(Order.class).ㅤAS("o");
		assertEquals("select distinct o.customerId from pro.peter_rader.linqava.Order o", q.getUnsafeHql());
	}

	// SELECT o.total + o.discount FROM Order o — Expr arithmetic against a bare column, no typedCol() needed
	@Test
	public void testExprArithmeticWithBareColOperand() {
		Q<Object> q = SELECTㅤ(ᐩ(Order::total, Order::discount)).ㅤFROMㅤ(Order.class).ㅤAS("o");
		assertEquals("select o.total + o.discount from pro.peter_rader.linqava.Order o", q.getUnsafeHql());
	}

	// SELECT o.id FROM Order o JOIN Customer c WHERE o.customerId = c.id
	// — WhereStep comparison against a bare column of a DIFFERENT already-aliased entity
	@Test
	public void testWhereBareColToBareColComparison() {
		Q<Object> q = SELECTㅤ(Order::id).ㅤFROMㅤ(Order.class).ㅤAS("o").JOIN(Customer.class).ㅤAS("c")
				.ㅤWHEREㅤ(Order::customerId).ㅤᆖㅤ(Customer::id);
		assertEquals("select o.id from pro.peter_rader.linqava.Order o join pro.peter_rader.linqava.Customer c where o.customerId = c.id", q.getUnsafeHql());
	}

	// SELECT o.id, c.name FROM Order o JOIN Customer c ON c.id = o.customerId
	// — the static Cond builder compares two bare columns directly, no typedCol() needed on either side
	@Test
	public void testJoinOnWithBareColColCondBuilder() {
		Q<Object> q = SELECTㅤ(Order::id, Customer::name).ㅤFROMㅤ(Order.class).ㅤAS("o")
				.JOIN(Customer.class).ㅤAS("c").ㅤONㅤ(ㅤᆖㅤ(Customer::id, Order::customerId));
		assertEquals("select o.id, c.name from pro.peter_rader.linqava.Order o join pro.peter_rader.linqava.Customer c on c.id = o.customerId", q.getUnsafeHql());
	}

	// SELECT o FROM Order o WHERE o.total > 100 AND o.customerId = o.id
	// — Cond's pending-left continuation (AND(TypedCol)) finished by a bare TypedCol comparison
	@Test
	public void testCondPendingLeftWithBareColContinuation() {
		Q<Order> q = SELECTㅤ(Order.class).ㅤFROMㅤ(Order.class).ㅤAS("o")
				.ㅤWHEREㅤ(ㅤᐳㅤ(Order::total, 100).ㅤANDㅤ(Order::customerId).ᆖ(Order::id));
		assertEquals("select o from pro.peter_rader.linqava.Order o where o.total > 100 and o.customerId = o.id", q.getUnsafeHql());
	}

	// SELECT NULLIF(o.discount, o.total) FROM Order o
	@Test
	public void testNullifWithBareColColOperands() {
		Q<Object> q = SELECTㅤ(NULLIF(Order::discount, Order::total)).ㅤFROMㅤ(Order.class).ㅤAS("o");
		assertEquals("select nullif(o.discount, o.total) from pro.peter_rader.linqava.Order o", q.getUnsafeHql());
	}

	// SELECT o.id, CASE WHEN ... THEN 'GOLD' ELSE o.status END FROM Order o
	@Test
	public void testCaseElseWithBareColOperand() {
		Q<Object> q = SELECTㅤ(Order::id, CASE().WHEN(ㅤᐳᆖㅤ(Order::total, 1000)).THEN("GOLD").ELSE(Order::status).END())
				.ㅤFROMㅤ(Order.class).ㅤAS("o");
		assertEquals("select o.id, case when o.total >= 1000 then 'GOLD' else o.status end from pro.peter_rader.linqava.Order o",
				q.getUnsafeHql());
	}

	// SELECT o.id FROM Order o WHERE o.status IN (o.status) — WhereStep IN/LIKE against a bare column
	@Test
	public void testWhereInAndLikeWithBareColOperand() {
		Q<Object> q = SELECTㅤ(Order::id).ㅤFROMㅤ(Order.class).ㅤAS("o").ㅤWHEREㅤ(Order::status).IN(Order::status);
		assertEquals("select o.id from pro.peter_rader.linqava.Order o where o.status in o.status", q.getUnsafeHql());
	}

	// SELECT CONCAT(o.status, '-', o.id) FROM Order o
	@Test
	public void testConcat() {
		Q<Object> q = SELECTㅤ(CONCAT(Order::status, "-", Order::id)).ㅤFROMㅤ(Order.class).ㅤAS("o");
		assertEquals("select concat(o.status, '-', o.id) from pro.peter_rader.linqava.Order o", q.getUnsafeHql());
	}

	// Chaining GROUPㅤBY twice no longer compiles: GROUPㅤBY(...) returns Grouped<E>, which
	// deliberately doesn't re-expose GROUPㅤBY (see Grouped). E.g. the following is a compile
	// error and can no longer be written:
	//   SELECTㅤ(Order::customerId).ㅤFROMㅤ(Order.class).ㅤAS("o")
	//           .GROUPㅤBY(Order::customerId).GROUPㅤBY(Order::status);
	//
	// The one remaining loophole — reusing the original Q reference directly instead of chaining
	// off its fluent return value — still hits a runtime guard.
	@Test(expected = IllegalStateException.class)
	public void testGroupByCalledTwiceOnReusedReferenceThrows() {
		Q<Object> q = SELECTㅤ(Order::customerId).ㅤFROMㅤ(Order.class).ㅤAS("o");
		q.GROUPㅤBY(Order::customerId);
		q.GROUPㅤBY(Order::status);
	}

}
