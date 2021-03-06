package eu.bcvsolutions.idm.connector.msgraph.operation;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;

import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.extensions.DirectoryRole;
import com.microsoft.graph.models.extensions.Group;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.User;
import com.microsoft.graph.requests.extensions.IDirectoryRoleCollectionPage;
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
			String encodedId = URLEncoder.encode(id, StandardCharsets.UTF_8.toString());

			User user = graphClient.users(encodedId)
					.buildRequest()
					.select(StringUtils.join(GraphConnector.basicUserAttrs, ','))
					.get();

			user.assignedLicenses = Utils.getLicensesForUser(encodedId, graphClient);

			return user;
		} catch (ClientException | UnsupportedEncodingException e) {
			LOG.info("ClientException:", e);
			if (e instanceof GraphServiceException) {
				GraphServiceException graphServiceException = (GraphServiceException) e;
				if (graphServiceException.getResponseCode() == 404) {
					// User not found return null, we don't want to throw error in this case
					return null;
				}
			}
			throw new ConnectorException("Getting one user failed: ", e);
		}
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
	 * Get one Azure role
	 *
	 * @param id Role Identifier
	 * @return Role object
	 */
	public DirectoryRole getAzureRole(String id) {
		return graphClient.directoryRoles(id).buildRequest().get();
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
		IGroupCollectionPage groupCollectionPage = graphClient.groups().buildRequest().get();
		List<Group> groups = new ArrayList<>(groupCollectionPage.getCurrentPage());

		LOG.info("First page loaded");
		while (groupCollectionPage.getNextPage() != null) {
			LOG.info("Loading next page");
			groupCollectionPage = groupCollectionPage.getNextPage().buildRequest().get();
			groups.addAll(groupCollectionPage.getCurrentPage());
		}

		return groups;
	}

	/**
	 * Get all Azure roles
	 *
	 * @return List of Roles
	 */
	public List<DirectoryRole> getAzureGroups() {
		IDirectoryRoleCollectionPage azureRolesPage = graphClient.directoryRoles().buildRequest().get();
		List<DirectoryRole> azureRoles = new ArrayList<>(azureRolesPage.getCurrentPage());

		LOG.info("First page loaded");
		while (azureRolesPage.getNextPage() != null) {
			LOG.info("Loading next page");
			azureRolesPage = azureRolesPage.getNextPage().buildRequest().get();
			azureRoles.addAll(azureRolesPage.getCurrentPage());
		}

		return azureRoles;
	}
}
