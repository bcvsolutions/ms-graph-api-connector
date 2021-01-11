package eu.bcvsolutions.idm.connector.msgraph.operation;

import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.Uid;

import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.User;

import eu.bcvsolutions.idm.connector.msgraph.util.GuardedStringAccessor;
import eu.bcvsolutions.idm.connector.msgraph.util.Utils;

/**
 * @author Roman Kučera
 * Class for delete operations
 */
public class DeleteOperation {
	private static final Log LOG = Log.getLog(DeleteOperation.class);

	private final IGraphServiceClient graphClient;

	public DeleteOperation(IGraphServiceClient graphClient) {
		this.graphClient = graphClient;
	}

	/**
	 * Delete specific user
	 * @param uid User identification
	 */
	public void deleteUser(Uid uid) {
		LOG.info("User {0} will be deleted", uid.getUidValue());
		graphClient.users(uid.getUidValue()).buildRequest().delete();
		LOG.info("User {0} deleted", uid.getUidValue());
	}

	/**
	 * Delete specific group
	 * @param uid Group identification
	 */
	public void deleteGroup(Uid uid) {
		// TODO implement
		throw new UnsupportedOperationException("Not implemented yet");
//		Group group = new Group();
//		graphClient.groups(uid.getUidValue()).buildRequest().patch(group);
	}
}
