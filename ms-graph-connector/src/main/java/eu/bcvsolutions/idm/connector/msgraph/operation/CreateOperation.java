package eu.bcvsolutions.idm.connector.msgraph.operation;

import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributesAccessor;

import com.microsoft.graph.models.extensions.Group;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.User;

import eu.bcvsolutions.idm.connector.msgraph.util.GuardedStringAccessor;
import eu.bcvsolutions.idm.connector.msgraph.util.Utils;

/**
 * @author Roman Kučera
 * Class for creating operations
 */
public class CreateOperation {

	private static final Log LOG = Log.getLog(CreateOperation.class);

	private final IGraphServiceClient graphClient;
	private final GuardedStringAccessor guardedStringAccessor;

	public CreateOperation(IGraphServiceClient graphClient, GuardedStringAccessor guardedStringAccessor) {
		this.graphClient = graphClient;
		this.guardedStringAccessor = guardedStringAccessor;
	}

	/**
	 * Create user from attributes
	 *
	 * @param createAttributes Set of attributes from which we create user
	 * @return User object of created user
	 */
	public User createUser(final Set<Attribute> createAttributes) {
		AttributesAccessor attributesAccessor = new AttributesAccessor(createAttributes);
		User user = Utils.prepareUserObject(attributesAccessor, guardedStringAccessor);

		user = graphClient
				.users()
				.buildRequest()
				.post(user);
		LOG.info("User {0} created", user.userPrincipalName);

		Utils.setLicenses(attributesAccessor, user.userPrincipalName, graphClient);

		return user;
	}

	/**
	 * Craete group from attributes
	 *
	 * @param createAttributes Set of attributes from which we create group
	 * @return Group object of created group
	 */
	public Group createGroup(Set<Attribute> createAttributes) {
		// TODO implement
		throw new UnsupportedOperationException("Not implemented yet");
//		Group group = new Group();
//		return graphClient.groups().buildRequest().post(group);
	}
}
