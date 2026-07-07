/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _ 
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package pro.peter_rader.linqava;

import java.time.Instant;
import java.util.List;

/*
 * Demo entities used by the linqava query test-cases. The getter names deliberately match the
 * column names used in the original HQL/JPQL queries, so that method references such as
 * User::Name read like the SQL they replace.
 */

class User {
	int id() {
		return 0;
	}

	String Name() {
		return null;
	}

	String name() {
		return null;
	}

	boolean active() {
		return false;
	}

	String email() {
		return null;
	}
}

class Order {
	int id() {
		return 0;
	}

	int customerId() {
		return 0;
	}

	double total() {
		return 0;
	}

	Instant createdAt() {
		return null;
	}

	String status() {
		return null;
	}

	double discount() {
		return 0;
	}
}

class Customer {
	int id() {
		return 0;
	}

	String name() {
		return null;
	}

	String country() {
		return null;
	}

	List<Order> orders() {
		return null;
	}

	List<Product> wishlist() {
		return null;
	}
}

class Product {
	int id() {
		return 0;
	}

	int categoryId() {
		return 0;
	}

	double price() {
		return 0;
	}

	String title() {
		return null;
	}
}

class Category {
	int id() {
		return 0;
	}

	int parentId() {
		return 0;
	}
}

class Employee {
	int id() {
		return 0;
	}

	Integer managerId() {
		return null;
	}
}

class OrderItem {
	int orderId() {
		return 0;
	}

	int productId() {
		return 0;
	}
}

class Supplier {
	int id() {
		return 0;
	}

	String email() {
		return null;
	}

	boolean preferred() {
		return false;
	}
}

class Payment {
	int id() {
		return 0;
	}
}

class CreditCardPayment extends Payment {
	String cardType() {
		return null;
	}
}

class BankTransferPayment extends Payment {
	String iban() {
		return null;
	}
}

class Driver {
	int id() {
		return 0;
	}
}

class Car {
	int id() {
		return 0;
	}

	Driver driver() {
		return null;
	}

	SerialPlate plate() {
		return null;
	}

}

class SerialPlate {
	int id() {
		return 0;
	}
}

class CustomerSummary {
	@SuppressWarnings("PMD.UnusedFormalParameter")
	CustomerSummary(Object id, Object name, Object orderCount) {
	}
}

class TranslationStack {
	int id() {
		return 0;
	}

	int version() {
		return 0;
	}

	List<TranslationKeyword> translationKeywords() {
		return null;
	}

	List<TranslationImageKey> translationImageKeys() {
		return null;
	}
}

class TranslationKeyword {
	int version() {
		return 0;
	}

	List<TranslationValue> translationValues() {
		return null;
	}
}

class TranslationValue {
	int version() {
		return 0;
	}
}

class TranslationImageKey {
	int version() {
		return 0;
	}

	List<TranslationImage> translationImages() {
		return null;
	}
}

class TranslationImage {
	int version() {
		return 0;
	}
}

class EMailAddressLocalName {
	long id() {
		return 0;
	}

	String localName() {
		return null;
	}
}

/*
 * A fixture with real JavaBeans accessors (get/is prefixes), unlike the other demo entities above,
 * to verify that Names#property strips the prefix and decapitalizes the remainder.
 */
class LegacyBean {
	String getName() {
		return null;
	}

	boolean isActive() {
		return false;
	}

	String getURLName() {
		return null;
	}
}
