package eu.bcvsolutions.idm.connector.msgraph.operation;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributesAccessor;

import com.microsoft.graph.auth.confidentialClient.ClientCredentialProvider;
import com.microsoft.graph.auth.enums.NationalCloud;
import com.microsoft.graph.models.extensions.Group;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.PasswordProfile;
import com.microsoft.graph.models.extensions.User;

import eu.bcvsolutions.idm.connector.msgraph.util.GuardedStringAccessor;
import eu.bcvsolutions.idm.connector.msgraph.util.Utils;

/**
 * @author Roman Kučera
 */
public class CreateOperation {
	// TODO add logs

	private static final Log LOG = Log.getLog(CreateOperation.class);

	private final IGraphServiceClient graphClient;
	private final GuardedStringAccessor guardedStringAccessor;

	public CreateOperation(IGraphServiceClient graphClient, GuardedStringAccessor guardedStringAccessor) {
		this.graphClient = graphClient;
		this.guardedStringAccessor = guardedStringAccessor;
	}

	public User createUser(final Set<Attribute> createAttributes) {
		User user = Utils.prepareUserObject(createAttributes, guardedStringAccessor);
		return graphClient.users().buildRequest().post(user);
	}

	public Group createGroup(Set<Attribute> createAttributes) {
		// TODO implement
		throw new UnsupportedOperationException("Not implemented yet");
//		Group group = new Group();
//		return graphClient.groups().buildRequest().post(group);
	}
}
