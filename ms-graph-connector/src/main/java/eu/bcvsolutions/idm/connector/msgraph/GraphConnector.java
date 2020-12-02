package eu.bcvsolutions.idm.connector.msgraph;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.operations.ResolveUsernameApiOp;
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
import org.identityconnectors.framework.spi.operations.AuthenticateOp;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.SyncOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateAttributeValuesOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;

import com.microsoft.graph.auth.confidentialClient.ClientCredentialProvider;
import com.microsoft.graph.auth.enums.NationalCloud;
import com.microsoft.graph.models.extensions.Group;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.User;
import com.microsoft.graph.requests.extensions.GraphServiceClient;

import eu.bcvsolutions.idm.connector.msgraph.operation.SearchOperation;
import eu.bcvsolutions.idm.connector.msgraph.util.GuardedStringAccessor;
import eu.bcvsolutions.idm.connector.msgraph.util.Utils;

/**
 * @author Roman Kučera
 * MS Graph connector main class
 */
@ConnectorClass(configurationClass = GraphConfiguration.class, displayNameKey = "graph.connector.display")
public class GraphConnector implements Connector,
		CreateOp, UpdateOp, UpdateAttributeValuesOp, DeleteOp,
		AuthenticateOp, ResolveUsernameApiOp, SchemaOp, SyncOp, TestOp, SearchOp<String> {

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

		return new Uid(UUID.randomUUID().toString());
	}

	@Override
	public Uid update(
			final ObjectClass objectClass,
			final Uid uid,
			final Set<Attribute> replaceAttributes,
			final OperationOptions options) {

		return uid;
	}

	@Override
	public Uid addAttributeValues(
			final ObjectClass objclass,
			final Uid uid,
			final Set<Attribute> valuesToAdd,
			final OperationOptions options) {

		return uid;
	}

	@Override
	public Uid removeAttributeValues(
			final ObjectClass objclass,
			final Uid uid,
			final Set<Attribute> valuesToRemove,
			final OperationOptions options) {

		return uid;
	}

	@Override
	public void delete(
			final ObjectClass objectClass,
			final Uid uid,
			final OperationOptions options) {
	}

	@Override
	public Uid authenticate(
			final ObjectClass objectClass,
			final String username,
			final GuardedString password,
			final OperationOptions options) {

		return new Uid(username);
	}

	@Override
	public Uid resolveUsername(
			final ObjectClass objectClass,
			final String username,
			final OperationOptions options) {

		return new Uid(username);
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

	private void prepareSchema(ObjectClassInfoBuilder groupObjectClassBuilder, Field[] declaredFieldsGroups) {
		Arrays.stream(declaredFieldsGroups).forEach(field -> {
			if (field.getType() == String.class || field.getType() == Boolean.class || field.getType() == Integer.class) {
				groupObjectClassBuilder.addAttributeInfo(AttributeInfoBuilder.build(field.getName(), field.getType()));
			} else {
				LOG.info("Property type {0} not supported now", field.getType().getName());
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
				handler.handle(Utils.handleUser(user, objectClass));
			} else if (objectClass.is(ObjectClass.GROUP_NAME)) {
				Group group = searchOperation.getGroup(query);
				handler.handle(Utils.handleGroup(group, objectClass));
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
		char[] pass = guardedStringAccessor.getArray();

		ClientCredentialProvider authProvider = new ClientCredentialProvider(
				this.configuration.getClientId(),
				Arrays.asList(this.configuration.getScopes()),
				new String(pass),
				this.configuration.getTenant(),
				NationalCloud.valueOf(this.getConfiguration().getNationalCloud()));

		guardedStringAccessor.clearArray();

		graphClient = GraphServiceClient
				.builder()
				.authenticationProvider(authProvider)
				.buildClient();
	}
}
