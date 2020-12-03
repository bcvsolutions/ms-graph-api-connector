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
 */
public class DeleteOperation {
	// TODO add logs

	private static final Log LOG = Log.getLog(DeleteOperation.class);

	private final IGraphServiceClient graphClient;
	private final GuardedStringAccessor guardedStringAccessor;

	public DeleteOperation(IGraphServiceClient graphClient, GuardedStringAccessor guardedStringAccessor) {
		this.graphClient = graphClient;
		this.guardedStringAccessor = guardedStringAccessor;
	}

	public void deleteUser(Uid uid) {
		graphClient.users(uid.getUidValue()).buildRequest().delete();
	}

	public void deleteGroup(Uid uid) {
		// TODO implement
		throw new UnsupportedOperationException("Not implemented yet");
//		Group group = new Group();
//		graphClient.groups(uid.getUidValue()).buildRequest().patch(group);
	}
}
