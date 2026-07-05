/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package pro.peter_rader.linqava.h2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;

/**
 * A real, persistable JPA entity (property access) used to exercise linqava end-to-end against a
 * genuine Hibernate {@code EntityManager} backed by H2 — not the no-op {@code Model.java} fixtures
 * used by {@code QueryTest}, which only ever exist to be turned into HQL strings.
 */
@Entity
public class Customer {

	private Long id;
	private String name;
	private String country;
	private String vipCode;
	private List<Order> orders = new ArrayList<>();
	private Map<String, Order> ordersByStatus = new LinkedHashMap<>();

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public Customer setName(final String name) {
		this.name = name;
		return this;
	}

	public String getCountry() {
		return country;
	}

	public Customer setCountry(final String country) {
		this.country = country;
		return this;
	}

	/** Deliberately named with a leading acronym, to regression-test {@code Names.property()} against a real Hibernate metamodel. */
	public String getVIPCode() {
		return vipCode;
	}

	public Customer setVIPCode(final String vipCode) {
		this.vipCode = vipCode;
		return this;
	}

	@OneToMany(mappedBy = "customer")
	public List<Order> getOrders() {
		return orders;
	}

	public void setOrders(final List<Order> orders) {
		this.orders = orders;
	}

	/**
	 * A genuine map <em>association</em> (not an {@code @ElementCollection}): the same physical
	 * {@code Order} rows as {@link #getOrders()}, indexed by each order's own {@code status}, via
	 * {@code @MapKey}.
	 */
	@OneToMany(mappedBy = "customer")
	@MapKey(name = "status")
	public Map<String, Order> getOrdersByStatus() {
		return ordersByStatus;
	}

	public void setOrdersByStatus(final Map<String, Order> ordersByStatus) {
		this.ordersByStatus = ordersByStatus;
	}
}
