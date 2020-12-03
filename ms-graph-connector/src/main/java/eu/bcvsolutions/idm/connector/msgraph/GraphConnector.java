package eu.bcvsolutions.idm.connector.msgraph;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AbstractFilterTranslator;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.SyncOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;

import com.microsoft.graph.auth.confidentialClient.ClientCredentialProvider;
import com.microsoft.graph.auth.enums.NationalCloud;
import com.microsoft.graph.models.extensions.Group;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.User;
import com.microsoft.graph.requests.extensions.GraphServiceClient;

import eu.bcvsolutions.idm.connector.msgraph.operation.CreateOperation;
import eu.bcvsolutions.idm.connector.msgraph.operation.DeleteOperation;
import eu.bcvsolutions.idm.connector.msgraph.operation.SearchOperation;
import eu.bcvsolutions.idm.connector.msgraph.operation.UpdateOperation;
import eu.bcvsolutions.idm.connector.msgraph.util.GuardedStringAccessor;
import eu.bcvsolutions.idm.connector.msgraph.util.Utils;

/**
 * @author Roman Kučera
 * MS Graph connector main class
 */
@ConnectorClass(configurationClass = GraphConfiguration.class, displayNameKey = "graph.connector.display")
public class GraphConnector implements Connector,
		CreateOp, UpdateOp, DeleteOp, SchemaOp, SyncOp, TestOp, SearchOp<String> {

	private static final Log LOG = Log.getLog(GraphConnector.class);

	private GraphConfiguration configuration;
	private IGraphServiceClient graphClient;
	private GuardedStringAccessor guardedStringAccessor;

	@Override
	public GraphConfiguration getConfiguration() {
		return configuration;
	}

	@Override
	public void init(final Configuration configuration) {
		this.configuration = (GraphConfiguration) configuration;
		LOG.ok("Connector {0} successfully inited", getClass().getName());
	}

	@Override
	public void dispose() {
		// dispose of any resources the this connector uses.
	}

	@Override
	public Uid create(
			final ObjectClass objectClass,
			final Set<Attribute> createAttributes,
			final OperationOptions options) {
		if (graphClient == null) {
			initGraphClient();
		}

		if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
			CreateOperation createOperation = new CreateOperation(graphClient, guardedStringAccessor);
			User user = createOperation.createUser(createAttributes);
			return new Uid(user.userPrincipalName);
		}

		if (objectClass.is(ObjectClass.GROUP_NAME)) {
			CreateOperation createOperation = new CreateOperation(graphClient, guardedStringAccessor);
			Group group = createOperation.createGroup(createAttributes);
			return new Uid(group.id);
		}

		throw new ConnectorException("Object was not created for unknown reason, see log for further details");
	}

	@Override
	public Uid update(
			final ObjectClass objectClass,
			final Uid uid,
			final Set<Attribute> replaceAttributes,
			final OperationOptions options) {

		if (graphClient == null) {
			initGraphClient();
		}

		if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
			UpdateOperation updateOperation = new UpdateOperation(graphClient, guardedStringAccessor);
			updateOperation.updateUser(replaceAttributes, uid);
			return uid;
		}

		if (objectClass.is(ObjectClass.GROUP_NAME)) {
			UpdateOperation updateOperation = new UpdateOperation(graphClient, guardedStringAccessor);
			updateOperation.updateGroup(replaceAttributes, uid);
			return uid;
		}

		throw new ConnectorException("Object was not created for unknown reason, see log for further details");
	}

	@Override
	public void delete(
			final ObjectClass objectClass,
			final Uid uid,
			final OperationOptions options) {

		if (graphClient == null) {
			initGraphClient();
		}

		if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
			DeleteOperation deleteOperation = new DeleteOperation(graphClient, guardedStringAccessor);
			deleteOperation.deleteUser(uid);
			return;
		}

		if (objectClass.is(ObjectClass.GROUP_NAME)) {
			DeleteOperation deleteOperation = new DeleteOperation(graphClient, guardedStringAccessor);
			deleteOperation.deleteGroup(uid);
			return;
		}

		throw new ConnectorException("Object was not deleted for unknown reason, see log for further details");
	}

	@Override
	public Schema schema() {
		ObjectClassInfoBuilder accountObjectClassBuilder = new ObjectClassInfoBuilder();
		accountObjectClassBuilder.setType(ObjectClass.ACCOUNT_NAME);
		prepareSchema(accountObjectClassBuilder, User.class.getDeclaredFields());

		ObjectClassInfoBuilder groupObjectClassBuilder = new ObjectClassInfoBuilder();
		groupObjectClassBuilder.setType(ObjectClass.GROUP_NAME);
		prepareSchema(groupObjectClassBuilder, Group.class.getDeclaredFields());

		SchemaBuilder schemaBuilder = new SchemaBuilder(GraphConnector.class);
		schemaBuilder.defineObjectClass(accountObjectClassBuilder.build());
		schemaBuilder.defineObjectClass(groupObjectClassBuilder.build());
		return schemaBuilder.build();
	}

	private void prepareSchema(ObjectClassInfoBuilder objectClassBuilder, Field[] declaredFieldsGroups) {
//		objectClassBuilder.addAttributeInfo(AttributeInfoBuilder.define("disabledPlans").setMultiValued(true).setType(Byte[].class).build());
		objectClassBuilder.addAttributeInfo(AttributeInfoBuilder.define("addLicenses").setMultiValued(true).setType(String.class).build());
		objectClassBuilder.addAttributeInfo(AttributeInfoBuilder.define("removeLicenses").setMultiValued(true).setType(String.class).build());
		Arrays.stream(declaredFieldsGroups).forEach(field -> {
			if (field.getType() == String.class || field.getType() == Boolean.class || field.getType() == Integer.class) {
				objectClassBuilder.addAttributeInfo(AttributeInfoBuilder.build(field.getName(), field.getType()));
			} else {
				if (field.getName().equals("passwordProfile")) {
					objectClassBuilder.addAttributeInfo(AttributeInfoBuilder.build("forceChangePasswordNextSignIn", Boolean.class));
					objectClassBuilder.addAttributeInfo(AttributeInfoBuilder.build("forceChangePasswordNextSignInWithMfa", Boolean.class));
					objectClassBuilder.addAttributeInfo(AttributeInfoBuilder.build("__PASSWORD__", GuardedString.class));
				} else{
//					TODO this works for custom object but I dont want to use it for now, probably more object will need some manual mapping as passwordProfile
//					prepareSchema(objectClassBuilder, field.getType().getDeclaredFields());
				}
			}
		});
	}

	@Override
	public void sync(
			final ObjectClass objectClass,
			final SyncToken token,
			final SyncResultsHandler handler,
			final OperationOptions options) {
	}

	@Override
	public SyncToken getLatestSyncToken(final ObjectClass objectClass) {
		return new SyncToken(null);
	}

	@Override
	public void test() {
		if (graphClient == null) {
			initGraphClient();
		}
		// TODO make some better test request
		graphClient.users("0eba5861-8bbf-4126-aa10-1d67d15d8d63").buildRequest().get();
	}

	@Override
	public FilterTranslator<String> createFilterTranslator(
			final ObjectClass objectClass,
			final OperationOptions options) {

		if (objectClass.is(ObjectClass.ACCOUNT_NAME) || objectClass.is(ObjectClass.GROUP_NAME)) {
			return new AbstractFilterTranslator<String>() {
				@Override
				protected String createEqualsExpression(EqualsFilter filter, boolean not) {
					if (not) {
						throw new UnsupportedOperationException("This type of equals expression is not allow for now.");
					}

					Attribute attr = filter.getAttribute();

					if (attr == null || !attr.is(Uid.NAME)) {
						throw new IllegalArgumentException("Attribute is null or not UID attribute.");
					}

					return ((Uid) attr).getUidValue();
				}
			};
		}
		return null;
	}

	@Override
	public void executeQuery(
			final ObjectClass objectClass,
			final String query,
			final ResultsHandler handler,
			final OperationOptions options) {

		if (graphClient == null) {
			initGraphClient();
		}
		SearchOperation searchOperation = new SearchOperation(graphClient);
		if (query != null) {
			LOG.info("Get one record");
			if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
				User user = searchOperation.getUser(query);
				if (user != null) {
					handler.handle(Utils.handleUser(user, objectClass));
				}
			} else if (objectClass.is(ObjectClass.GROUP_NAME)) {
				Group group = searchOperation.getGroup(query);
				if (group != null) {
					handler.handle(Utils.handleGroup(group, objectClass));
				}
			} else {
				LOG.warn("Unsupported object class {0}", objectClass);
			}
		} else {
			LOG.info("Get all");
			if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
				List<User> users = searchOperation.getUsers();
				users.forEach(user -> handler.handle(Utils.handleUser(user, objectClass)));
			} else if (objectClass.is(ObjectClass.GROUP_NAME)) {
				List<Group> groups = searchOperation.getGroups();
				groups.forEach(group -> handler.handle(Utils.handleGroup(group, objectClass)));
			} else {
				LOG.warn("Unsupported object class {0}", objectClass);
			}
		}
	}

	private void initGraphClient() {
		guardedStringAccessor = new GuardedStringAccessor();
		this.configuration.getClientSecret().access(guardedStringAccessor);

		ClientCredentialProvider authProvider = new ClientCredentialProvider(
				this.configuration.getClientId(),
				Arrays.asList(this.configuration.getScopes()),
				new String(guardedStringAccessor.getArray()),
				this.configuration.getTenant(),
				NationalCloud.valueOf(this.getConfiguration().getNationalCloud()));

		guardedStringAccessor.clearArray();

		graphClient = GraphServiceClient
				.builder()
				.authenticationProvider(authProvider)
				.buildClient();
	}
}
