package eu.bcvsolutions.idm.connector.msgraph.operation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.identityconnectors.common.logging.Log;

import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.models.extensions.Group;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.User;
import com.microsoft.graph.requests.extensions.IGroupCollectionPage;
import com.microsoft.graph.requests.extensions.IUserCollectionPage;

import eu.bcvsolutions.idm.connector.msgraph.GraphConnector;
import eu.bcvsolutions.idm.connector.msgraph.util.Utils;

/**
 * @author Roman Kučera
 * Class for search operations
 */
public class SearchOperation {
	private static final Log LOG = Log.getLog(SearchOperation.class);

	private final IGraphServiceClient graphClient;

	public SearchOperation(IGraphServiceClient graphClient) {
		this.graphClient = graphClient;
	}

	/**
	 * Get one user, It will load all attributes which are in schema
	 *
	 * @param id User identification
	 * @return User object
	 */
	public User getUser(String id) {
		try {
			User user = graphClient.users(id)
					.buildRequest()
					.select(StringUtils.join(GraphConnector.basicUserAttrs, ','))
					.get();

			user.assignedLicenses = Utils.getLicensesForUser(id, graphClient);

			return user;
		} catch (ClientException e) {
			LOG.info("ClientException:", e);
		}
		return null;
	}

	/**
	 * Get one group
	 *
	 * @param id Group Identifier
	 * @return Group object
	 */
	public Group getGroup(String id) {
		return graphClient.groups(id).buildRequest().get();
	}

	/**
	 * Get all users
	 *
	 * @return List of Users
	 */
	public List<User> getUsers() {
		List<User> users = new ArrayList<>();
		IUserCollectionPage userCollectionPage = graphClient.users().buildRequest().get();
		users.addAll(userCollectionPage.getCurrentPage());

		LOG.info("First page loaded");
		while (userCollectionPage.getNextPage() != null) {
			LOG.info("Loading next page");
			userCollectionPage = userCollectionPage.getNextPage().buildRequest().get();
			users.addAll(userCollectionPage.getCurrentPage());
		}

		return users;
	}

	/**
	 * Get all groups
	 *
	 * @return List of Groups
	 */
	public List<Group> getGroups() {
		List<Group> groups = new ArrayList<>();
		IGroupCollectionPage groupCollectionPage = graphClient.groups().buildRequest().get();
		groups.addAll(groupCollectionPage.getCurrentPage());

		LOG.info("First page loaded");
		while (groupCollectionPage.getNextPage() != null) {
			LOG.info("Loading next page");
			groupCollectionPage = groupCollectionPage.getNextPage().buildRequest().get();
			groups.addAll(groupCollectionPage.getCurrentPage());
		}

		return groups;
	}
}
