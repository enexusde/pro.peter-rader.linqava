/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package pro.peter_rader.linqava.h2;

/**
 * DTO fixture for {@code new pro.peter_rader.linqava.h2.StatusCount(...)} constructor projections,
 * used to verify {@code Q#via(EntityManager, Class)} against a real Hibernate session.
 */
public class StatusCount {

	private final String status;
	private final Long count;

	public StatusCount(final String status, final Long count) {
		this.status = status;
		this.count = count;
	}

	public String getStatus() {
		return status;
	}

	public Long getCount() {
		return count;
	}
}
