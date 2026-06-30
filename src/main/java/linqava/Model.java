package linqava;

import java.time.Instant;
import java.util.List;

/*
 * Demo entities used by the linqava query test-cases. The getter names deliberately match the
 * column names used in the original HQL/JPQL queries, so that method references such as
 * User::Name read like the SQL they replace.
 */

class User {
	int id() { return 0; }
	String Name() { return null; }
	String name() { return null; }
	boolean active() { return false; }
	String email() { return null; }
}

class Order {
	int id() { return 0; }
	int customerId() { return 0; }
	double total() { return 0; }
	Instant createdAt() { return null; }
	String status() { return null; }
	double discount() { return 0; }
}

class Customer {
	int id() { return 0; }
	String name() { return null; }
	String country() { return null; }
	List<Order> orders() { return null; }
	List<Product> wishlist() { return null; }
}

class Product {
	int id() { return 0; }
	int categoryId() { return 0; }
	double price() { return 0; }
	String title() { return null; }
}

class Category {
	int id() { return 0; }
	int parentId() { return 0; }
}

class Employee {
	int id() { return 0; }
	Integer managerId() { return null; }
}

class OrderItem {
	int orderId() { return 0; }
	int productId() { return 0; }
}

class Supplier {
	int id() { return 0; }
	String email() { return null; }
	boolean preferred() { return false; }
}

class Payment {
	int id() { return 0; }
}

class CreditCardPayment extends Payment {
	String cardType() { return null; }
}

class BankTransferPayment extends Payment {
	String iban() { return null; }
}

class CustomerSummary {
	CustomerSummary(Object id, Object name, Object orderCount) {
	}
}
