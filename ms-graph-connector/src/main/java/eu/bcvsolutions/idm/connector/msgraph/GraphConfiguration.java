package eu.bcvsolutions.idm.connector.msgraph;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;

import com.microsoft.graph.auth.enums.NationalCloud;

/**
 * @author Roman Kučera
 */
public class GraphConfiguration extends AbstractConfiguration {

	private String clientId;
	private String[] scopes;
	private GuardedString clientSecret;
	private String tenant;
	private String nationalCloud;

	@ConfigurationProperty(displayMessageKey = "graph.connector.clientId.display",
			helpMessageKey = "graph.connector.clientId.help", required = true, order = 1)
	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	@ConfigurationProperty(displayMessageKey = "graph.connector.scopes.display",
			helpMessageKey = "graph.connector.scopes.help", order = 2)
	public String[] getScopes() {
		if (scopes == null) {
			return new String[0];
		}
		return scopes;
	}

	public void setScopes(String[] scopes) {
		this.scopes = scopes;
	}

	@ConfigurationProperty(displayMessageKey = "graph.connector.secret.display",
			helpMessageKey = "graph.connector.secret.help", required = true, confidential = true, order = 3)
	public GuardedString getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(GuardedString clientSecret) {
		this.clientSecret = clientSecret;
	}

	@ConfigurationProperty(displayMessageKey = "graph.connector.tenant.display",
			helpMessageKey = "graph.connector.tenant.help", required = true, order = 4)
	public String getTenant() {
		return tenant;
	}

	public void setTenant(String tenant) {
		this.tenant = tenant;
	}

	@ConfigurationProperty(displayMessageKey = "graph.connector.cloud.display",
			helpMessageKey = "graph.connector.cloud.help", required = true, order = 5)
	public String getNationalCloud() {
		return nationalCloud;
	}

	public void setNationalCloud(String nationalCloud) {
		this.nationalCloud = nationalCloud;
	}

	@Override
	public void validate() {
		if (StringUtil.isBlank(clientId)) {
			throw new ConfigurationException("ClientId must not be blank!");
		}
		if (StringUtil.isBlank(tenant)) {
			throw new ConfigurationException("Tenant must not be blank!");
		}
		if (StringUtil.isBlank(nationalCloud)) {
			throw new ConfigurationException("National cloud must not be blank!");
		} else {
			NationalCloud.valueOf(nationalCloud);
		}
		if (clientSecret == null) {
			throw new ConfigurationException("Client secret must not be blank!");
		}
//		if (scopes == null || scopes.length == 0) {
//			throw new ConfigurationException("Scopes must not be blank");
//		}
	}

	public String getMessage(String key) {
		return getConnectorMessages().format(key, key);
	}
}
