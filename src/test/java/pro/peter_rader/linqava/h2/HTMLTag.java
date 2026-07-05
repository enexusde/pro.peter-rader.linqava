/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package pro.peter_rader.linqava.h2;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

/**
 * The inverse side of a many-to-many relation, deliberately named with a leading multi-letter
 * acronym ({@code HTML}) so its own getters ({@code getHTMLTagName()}) exercise
 * {@code Names.property()}'s "leave the acronym alone" rule against a real Hibernate metamodel, same
 * as {@link Customer#getVIPCode()} but on the entity's own type name too.
 */
@Entity
public class HTMLTag {

	private Long id;
	private String htmlTagName;
	private Set<Product> products = new LinkedHashSet<>();

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public String getHTMLTagName() {
		return htmlTagName;
	}

	public HTMLTag setHTMLTagName(final String htmlTagName) {
		this.htmlTagName = htmlTagName;
		return this;
	}

	// mappedBy is "HTMLTags", not "htmlTags": Product.getHTMLTags()'s real Hibernate attribute name
	// keeps the acronym's capitals (Introspector.decapitalize semantics) — this is deliberately
	// exact evidence of the same rule the acronym-getter tests in H2IntegrationTest exercise.
	@ManyToMany(mappedBy = "HTMLTags")
	public Set<Product> getProducts() {
		return products;
	}

	public void setProducts(final Set<Product> products) {
		this.products = products;
	}
}
