package eu.bcvsolutions.idm.connector.msgraph.operation;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Uid;

import com.microsoft.graph.models.extensions.IGraphServiceClient;

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

		try {
			String encodedId = URLEncoder.encode(uid.getUidValue(), StandardCharsets.UTF_8.toString());
			graphClient.users(encodedId).buildRequest().delete();
			LOG.info("User {0} deleted", uid.getUidValue());
		} catch (UnsupportedEncodingException e) {
			throw new ConnectorException("Deleting one user failed: ", e);
		}

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
