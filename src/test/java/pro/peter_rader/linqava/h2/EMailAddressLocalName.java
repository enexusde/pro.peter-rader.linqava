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

/**
 * A real, persistable JPA entity (property access) — see {@link Customer}. Deliberately joined to
 * itself in {@code H2IntegrationTest} to exercise a self-join with an alias-qualified {@code ON}
 * condition, where bare (unqualified) column references would be ambiguous between the two aliases.
 */
@Entity
public class EMailAddressLocalName {

	private Long id;
	private String localName;

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public String getLocalName() {
		return localName;
	}

	public EMailAddressLocalName setLocalName(final String localName) {
		this.localName = localName;
		return this;
	}
}
