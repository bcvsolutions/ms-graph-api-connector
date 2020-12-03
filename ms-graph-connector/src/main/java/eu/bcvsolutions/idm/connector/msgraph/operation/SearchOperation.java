package eu.bcvsolutions.idm.connector.msgraph.operation;

import java.util.ArrayList;
import java.util.List;

import org.identityconnectors.common.logging.Log;

import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.models.extensions.Group;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.User;
import com.microsoft.graph.requests.extensions.IGroupCollectionPage;
import com.microsoft.graph.requests.extensions.IUserCollectionPage;

/**
 * @author Roman Kučera
 */
public class SearchOperation {
	// TODO add logs

	private static final Log LOG = Log.getLog(SearchOperation.class);

	private final IGraphServiceClient graphClient;

	public SearchOperation(IGraphServiceClient graphClient) {
		this.graphClient = graphClient;
	}

	public User getUser(String id) {
		try {
			return graphClient.users(id).buildRequest().get();
		} catch (ClientException e) {
			LOG.info("ClientException", e);
		}
		return null;
	}

	public Group getGroup(String id) {
		return graphClient.groups(id).buildRequest().get();
	}

	public List<User> getUsers() {
		List<User> users = new ArrayList<>();
		IUserCollectionPage userCollectionPage = graphClient.users().buildRequest().get();
		users.addAll(userCollectionPage.getCurrentPage());

		while (userCollectionPage.getNextPage() != null) {
			userCollectionPage = userCollectionPage.getNextPage().buildRequest().get();
			users.addAll(userCollectionPage.getCurrentPage());
		}

		return users;
	}

	public List<Group> getGroups() {
		List<Group> groups = new ArrayList<>();
		IGroupCollectionPage groupCollectionPage = graphClient.groups().buildRequest().get();
		groups.addAll(groupCollectionPage.getCurrentPage());

		while (groupCollectionPage.getNextPage() != null) {
			groupCollectionPage = groupCollectionPage.getNextPage().buildRequest().get();
			groups.addAll(groupCollectionPage.getCurrentPage());
		}

		return groups;
	}
}
