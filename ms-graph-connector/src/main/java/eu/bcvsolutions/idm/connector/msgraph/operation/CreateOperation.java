package eu.bcvsolutions.idm.connector.msgraph.operation;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributesAccessor;
import org.jetbrains.annotations.NotNull;

import com.microsoft.graph.models.extensions.Group;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.Invitation;
import com.microsoft.graph.models.extensions.User;

import eu.bcvsolutions.idm.connector.msgraph.GraphConnector;
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

		String userType = attributesAccessor.findString("userType");
		if (userType.equals("Member")) {
			return createNormalMember(attributesAccessor);
		} else if (userType.equals("Guest")) {
			return createGuest(attributesAccessor);
		}
		throw new ConnectorException("userType is not Member or Guest, can't create user. Please make sure you are sending the correct value.");
	}

	/**
	 * Create normal member in cloud
	 * @param attributesAccessor accessor with attributes
	 * @return user object
	 */
	@NotNull
	private User createNormalMember(AttributesAccessor attributesAccessor) {
		User user = Utils.prepareUserObject(attributesAccessor, guardedStringAccessor);

		user = graphClient
				.users()
				.buildRequest()
				.post(user);
		LOG.info("User {0} created", user.userPrincipalName);

		assignLicence(attributesAccessor, user);

		return user;
	}

	private void assignLicence(AttributesAccessor attributesAccessor, User user) {
		List<String> assignedLicenses = attributesAccessor.findStringList("assignedLicenses");
		if (assignedLicenses != null && !assignedLicenses.isEmpty()) {
			Utils.setLicenses(assignedLicenses, user.userPrincipalName, graphClient);
		}
	}

	/**
	 * Create guest user in cloud
	 * @param attributesAccessor accessor with attributes
	 * @return guest user object (only is is filled)
	 */
	public User createGuest(AttributesAccessor attributesAccessor) {
		Invitation invitation = new Invitation();
		invitation.invitedUserEmailAddress = attributesAccessor.findString("mail");
		invitation.invitedUserDisplayName = attributesAccessor.findString("displayName");
		// TODO make it configurable
		invitation.inviteRedirectUrl = "https://google.com";
		// TODO make it configurable
		invitation.sendInvitationMessage = true;

		invitation = graphClient.invitations()
				.buildRequest()
				.post(invitation);

		User user = graphClient.users(invitation.invitedUser.id)
				.buildRequest()
				.select(StringUtils.join(GraphConnector.basicUserAttrs, ','))
				.get();

		assignLicence(attributesAccessor, user);

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
