/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package pro.peter_rader.linqava.h2;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.ManyToMany;

/**
 * The owning side of a many-to-many relation ({@link HTMLTag}), plus a plain
 * {@code @ElementCollection} map (locale &rarr; localized name) — the second, simpler flavour of
 * "map association" alongside {@link Customer#getOrdersByStatus()}'s entity-valued map. The getter
 * {@code getSKUCode()} is another leading-acronym regression case, like {@link HTMLTag}.
 */
@Entity
public class Product {

	private Long id;
	private String sKUCode;
	private Set<HTMLTag> htmlTags = new LinkedHashSet<>();
	private Map<String, String> localizedNames = new LinkedHashMap<>();

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public String getSKUCode() {
		return sKUCode;
	}

	public Product setSKUCode(final String sKUCode) {
		this.sKUCode = sKUCode;
		return this;
	}

	@ManyToMany
	@JoinTable(name = "product_htmltag", joinColumns = @JoinColumn(name = "product_id"),
			inverseJoinColumns = @JoinColumn(name = "htmltag_id"))
	public Set<HTMLTag> getHTMLTags() {
		return htmlTags;
	}

	public void setHTMLTags(final Set<HTMLTag> htmlTags) {
		this.htmlTags = htmlTags;
	}

	/** A plain element-collection map, keyed by locale (e.g. {@code "de"} &rarr; {@code "Buch"}). */
	@ElementCollection
	@CollectionTable(name = "product_localized_name", joinColumns = @JoinColumn(name = "product_id"))
	@MapKeyColumn(name = "locale")
	@Column(name = "localized_name")
	public Map<String, String> getLocalizedNames() {
		return localizedNames;
	}

	public void setLocalizedNames(final Map<String, String> localizedNames) {
		this.localizedNames = localizedNames;
	}
}
