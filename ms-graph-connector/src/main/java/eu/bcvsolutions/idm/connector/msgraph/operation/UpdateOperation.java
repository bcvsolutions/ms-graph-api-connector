package eu.bcvsolutions.idm.connector.msgraph.operation;

import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributesAccessor;
import org.identityconnectors.framework.common.objects.Uid;

import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.User;

import eu.bcvsolutions.idm.connector.msgraph.GraphConfiguration;
import eu.bcvsolutions.idm.connector.msgraph.util.GuardedStringAccessor;
import eu.bcvsolutions.idm.connector.msgraph.util.Utils;

/**
 * @author Roman Kučera
 * Class for update operations
 */
public class UpdateOperation {

	private static final Log LOG = Log.getLog(UpdateOperation.class);

	private final IGraphServiceClient graphClient;
	private final GuardedStringAccessor guardedStringAccessor;
	private final GraphConfiguration graphConfiguration;

	public UpdateOperation(IGraphServiceClient graphClient, GuardedStringAccessor guardedStringAccessor, GraphConfiguration graphConfiguration) {
		this.graphClient = graphClient;
		this.guardedStringAccessor = guardedStringAccessor;
		this.graphConfiguration = graphConfiguration;
	}

	/**
	 * Update specific User
	 *
	 * @param updateAttributes Set of attributes which should be updated
	 * @param uid              User identification
	 */
	public void updateUser(final Set<Attribute> updateAttributes, Uid uid) {
		AttributesAccessor attributesAccessor = new AttributesAccessor(updateAttributes);
		User user = Utils.prepareUserObject(attributesAccessor, guardedStringAccessor);
		if (graphConfiguration.isDisablePasswordChangeAfterFirstLogin()) {
			LOG.info("Disable of password change after first login for password change operation is enable in connector configuration.");
			user.passwordProfile.forceChangePasswordNextSignIn = false;
		}
		graphClient
				.users(uid.getUidValue())
				.buildRequest()
				.patch(user);
		LOG.info("User {0} updated", uid.getUidValue());

		Utils.setLicenses(attributesAccessor, uid.getUidValue(), graphClient);
	}

	/**
	 * Update specific group
	 *
	 * @param updateAttributes Set of attributes which should be updated
	 * @param uid              Group identification
	 */
	public void updateGroup(Set<Attribute> updateAttributes, Uid uid) {
		// TODO implement
		throw new UnsupportedOperationException("Not implemented yet");
//		Group group = new Group();
//		graphClient.groups(uid.getUidValue()).buildRequest().patch(group);
	}
}
