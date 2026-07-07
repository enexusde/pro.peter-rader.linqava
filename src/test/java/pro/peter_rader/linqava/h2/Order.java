/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package pro.peter_rader.linqava.h2;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A real, persistable JPA entity (property access) — see {@link Customer}.
 *
 * <p>{@code @Table} is required because {@code ORDER} is a reserved SQL keyword; without a quoted
 * table name, H2 rejects {@code insert into Order (...)} with a syntax error. The HQL entity name
 * (used by linqava/Hibernate in queries) stays the unquoted simple name {@code Order}; only the
 * generated SQL table identifier needs quoting.</p>
 */
@Entity
@Table(name = "\"Order\"")
public class Order {

	private Long id;
	private Customer customer;
	private String status;
	private double total;
	private Double discount;

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	@ManyToOne
	public Customer getCustomer() {
		return customer;
	}

	public Order setCustomer(final Customer customer) {
		this.customer = customer;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public Order setStatus(final String status) {
		this.status = status;
		return this;
	}

	public double getTotal() {
		return total;
	}

	public Order setTotal(final double total) {
		this.total = total;
		return this;
	}

	public Double getDiscount() {
		return discount;
	}

	public Order setDiscount(final Double discount) {
		this.discount = discount;
		return this;
	}
}
