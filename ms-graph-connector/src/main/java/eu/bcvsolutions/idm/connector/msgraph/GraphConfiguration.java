package eu.bcvsolutions.idm.connector.msgraph;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;

public class GraphConfiguration extends AbstractConfiguration {

    @Override
    public void validate() {

    }

    public String getMessage(String key) {
        return getConnectorMessages().format(key, key);
    }
}
