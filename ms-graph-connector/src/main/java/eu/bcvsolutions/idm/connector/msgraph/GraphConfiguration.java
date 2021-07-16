package eu.bcvsolutions.idm.connector.msgraph;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;

import com.microsoft.graph.auth.enums.NationalCloud;

/**
 * @author Roman Kučera
 * Configuration of connector
 */
public class GraphConfiguration extends AbstractConfiguration {

	private String clientId;
	private String[] scopes;
	private GuardedString clientSecret;
	private String tenant;
	private String nationalCloud;
	private boolean disablePasswordChangeAfterFirstLogin;
	private int proxyPort = 0;
	private String proxyHostname;
	private String proxyUsername;
	private GuardedString proxyPassword;
	private boolean loadAzureRoles;

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

	@ConfigurationProperty(displayMessageKey = "graph.connector.disableChange.display",
			helpMessageKey = "graph.connector.disableChange.help", order = 6)
	public boolean isDisablePasswordChangeAfterFirstLogin() {
		return disablePasswordChangeAfterFirstLogin;
	}

	public void setDisablePasswordChangeAfterFirstLogin(boolean disablePasswordChangeAfterFirstLogin) {
		this.disablePasswordChangeAfterFirstLogin = disablePasswordChangeAfterFirstLogin;
	}

	@ConfigurationProperty(displayMessageKey = "graph.connector.proxyPort.display",
			helpMessageKey = "graph.connector.proxyPort.help", order = 7)
	public int getProxyPort() {
		return proxyPort;
	}

	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	@ConfigurationProperty(displayMessageKey = "graph.connector.proxyHostname.display",
			helpMessageKey = "graph.connector.proxyHostname.help", order = 8)
	public String getProxyHostname() {
		return proxyHostname;
	}

	public void setProxyHostname(String proxyHostname) {
		this.proxyHostname = proxyHostname;
	}

	@ConfigurationProperty(displayMessageKey = "graph.connector.proxyUsername.display",
			helpMessageKey = "graph.connector.proxyUsername.help", order = 9)
	public String getProxyUsername() {
		return proxyUsername;
	}

	public void setProxyUsername(String proxyUsername) {
		this.proxyUsername = proxyUsername;
	}

	@ConfigurationProperty(displayMessageKey = "graph.connector.proxyPass.display",
			helpMessageKey = "graph.connector.proxyPass.help", confidential = true, order = 10)
	public GuardedString getProxyPassword() {
		return proxyPassword;
	}

	public void setProxyPassword(GuardedString proxyPassword) {
		this.proxyPassword = proxyPassword;
	}

	@ConfigurationProperty(displayMessageKey = "graph.connector.azureRoles.display",
			helpMessageKey = "graph.connector.azureRoles.help", order = 11)
	public boolean isLoadAzureRoles() {
		return loadAzureRoles;
	}

	public void setLoadAzureRoles(boolean loadAzureRoles) {
		this.loadAzureRoles = loadAzureRoles;
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
	}

	public String getMessage(String key) {
		return getConnectorMessages().format(key, key);
	}
}
