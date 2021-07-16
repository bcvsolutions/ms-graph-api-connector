package eu.bcvsolutions.idm.connector.msgraph.util;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.AttributesAccessor;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;

import com.microsoft.graph.models.extensions.AssignedLicense;
import com.microsoft.graph.models.extensions.DirectoryObject;
import com.microsoft.graph.models.extensions.DirectoryRole;
import com.microsoft.graph.models.extensions.Group;
import com.microsoft.graph.models.extensions.IBaseGraphServiceClient;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.LicenseDetails;
import com.microsoft.graph.models.extensions.PasswordProfile;
import com.microsoft.graph.models.extensions.User;
import com.microsoft.graph.requests.extensions.IDirectoryObjectCollectionWithReferencesPage;
import com.microsoft.graph.requests.extensions.ILicenseDetailsCollectionPage;

/**
 * @author Roman Kučera
 * <p>
 * Utils class which contains methods which we need to execute in multiple places
 */
public final class Utils {

	private static final Log LOG = Log.getLog(Utils.class);

	private Utils() {

	}

	/**
	 * Convert User to ConnectorObject
	 *
	 * @param user        User object
	 * @param objectClass user object class
	 * @return Connector object with data
	 */
	public static ConnectorObject handleUser(User user, ObjectClass objectClass) {
		ConnectorObjectBuilder builder = new ConnectorObjectBuilder();

		if (user != null && !StringUtils.isBlank(user.userPrincipalName)) {
			LOG.info("We will process user: {0}", user.userPrincipalName);
			builder.setUid(user.userPrincipalName);
			builder.setName(user.userPrincipalName);
			builder.setObjectClass(objectClass);

			Field[] declaredFields = User.class.getDeclaredFields();
			addAttributeToBuilder(user, builder, declaredFields);
		} else {
			LOG.info("User object is null or userPrincipalName attribute is null or empty");
		}

		return builder.build();
	}

	/**
	 * Convert Group to ConnectorObject
	 *
	 * @param group       Group object
	 * @param objectClass group object class
	 * @param graphClient
	 * @return Connector object with data
	 */
	public static ConnectorObject handleGroup(Group group, ObjectClass objectClass, IGraphServiceClient graphClient) {
		ConnectorObjectBuilder builder = new ConnectorObjectBuilder();

		if (group != null && !StringUtils.isBlank(group.id)) {
			LOG.info("We will process group: {0}", group.id);
			builder.setUid(group.id);
			builder.setName(group.id);
			builder.setObjectClass(objectClass);

			Field[] declaredFields = Group.class.getDeclaredFields();
			addAttributeToBuilder(group, builder, declaredFields);

			//Add members and owners
			List<DirectoryObject> members = getAllRecords(graphClient.groups(group.id).members().buildRequest().get());
			List<DirectoryObject> owners = getAllRecords(graphClient.groups(group.id).owners().buildRequest().get());

			List<String> membersAsString = members.stream().map(user -> user.getRawObject().get("userPrincipalName").getAsString()).collect(Collectors.toList());
			List<String> ownersAsString = owners.stream().map(user -> user.getRawObject().get("userPrincipalName").getAsString()).collect(Collectors.toList());

			builder.addAttribute("members", membersAsString);
			builder.addAttribute("owners", ownersAsString);
		} else {
			LOG.info("Group object is null or id attribute is null or empty");
		}

		return builder.build();
	}

	public static ConnectorObject handleAzureRole(DirectoryRole azureRole, ObjectClass objectClass, IGraphServiceClient graphClient) {
		ConnectorObjectBuilder builder = new ConnectorObjectBuilder();

		if (azureRole != null && !StringUtils.isBlank(azureRole.id)) {
			LOG.info("We will process azure role: {0}", azureRole.id);
			builder.setUid(azureRole.id);
			builder.setName(azureRole.id);
			builder.setObjectClass(objectClass);
			builder.addAttribute("displayName", azureRole.displayName);
			builder.addAttribute("description", azureRole.description);

			//Add members and owners
			List<DirectoryObject> members = getAllRecords(graphClient.directoryRoles(azureRole.id).members().buildRequest().get());
			List<String> membersAsString = members.stream().map(user -> user.getRawObject().get("userPrincipalName").getAsString()).collect(Collectors.toList());
			builder.addAttribute("members", membersAsString);
		}

		return builder.build();
	}

	private static List<DirectoryObject> getAllRecords(IDirectoryObjectCollectionWithReferencesPage page) {
		List<DirectoryObject> records = new ArrayList<>(page.getCurrentPage());

		while (page.getNextPage() != null) {
			page = page.getNextPage().buildRequest().get();
			records.addAll(page.getCurrentPage());
		}
		return records;
	}

	/**
	 * Using reflexion to map attributes from User and Group into Connector object
	 *
	 * @param object         User or Group object, but it should work for more general DirectoryObject
	 * @param builder        Connector object builder where the attributes will be added
	 * @param declaredFields Array of fields of specific object
	 */
	private static void addAttributeToBuilder(DirectoryObject object, ConnectorObjectBuilder builder, Field[] declaredFields) {
		Class<?> clazz = object.getClass();
		Arrays.stream(declaredFields).forEach(field -> {
			try {
				Field objectField = clazz.getField(field.getName());
				if (isBasicDataType(objectField)) {
					Object fieldValue = objectField.get(object);
					builder.addAttribute(field.getName(), fieldValue);
				} else if ("java.util.List<com.microsoft.graph.models.extensions.AssignedLicense>".equals(objectField.getGenericType().getTypeName())) {
					LOG.info("Convert assigned licenses so connector can handle it");
					List<AssignedLicense> licenses = (List<AssignedLicense>) objectField.get(object);
					if (licenses != null) {
						List<String> licensesAsString = licenses.stream().map(assignedLicense -> assignedLicense.skuId.toString()).collect(Collectors.toList());
						builder.addAttribute(field.getName(), licensesAsString);
					}
				} else {
					LOG.info("Field type {0} not supported now", objectField.getType().getName());
				}
			} catch (NoSuchFieldException | IllegalAccessException e) {
				LOG.error("Error when getting field {0}: {1}", field.getName(), e);
			}
		});
	}

	/**
	 * Check if field is String, Integer or Boolean
	 *
	 * @param objectField Field object
	 * @return true if field is String, Integer or Boolean
	 */
	public static boolean isBasicDataType(Field objectField) {
		return objectField.getType() == String.class || objectField.getType() == Boolean.class || objectField.getType() == Integer.class;
	}

	/**
	 * Create User object based on attributes which connector received
	 *
	 * @param attributesAccessor    AttributeAccessor with specific attributes for User
	 * @param guardedStringAccessor GuardedStringAccessor so we can get password
	 * @return User object with filled attributes
	 */
	public static User prepareUserObject(AttributesAccessor attributesAccessor, GuardedStringAccessor guardedStringAccessor) {
		User user = new User();
		setPasswordToUser(guardedStringAccessor, attributesAccessor, user);

		Class<?> clazz = user.getClass();
		attributesAccessor.listAttributeNames().forEach(attribute -> {
			try {
				Field field = clazz.getField(attribute);
				if (field.getType() == String.class) {
					field.set(user, attributesAccessor.findString(attribute));
				} else if (field.getType() == Boolean.class) {
					field.set(user, attributesAccessor.findBoolean(attribute));
				} else if (field.getType() == Integer.class) {
					field.set(user, attributesAccessor.findInteger(attribute));
				} else {
					LOG.info("Type {0} is not supported now", field.getType().getName());
				}
			} catch (NoSuchFieldException ex) {
				LOG.error("Specific attribute does not exist", ex);
			} catch (IllegalAccessException e) {
				LOG.error("Error during object preparation", e);
			}
		});
		return user;
	}

	/**
	 * It will set password to User object
	 *
	 * @param guardedStringAccessor GuardedStringAccessor so we can get password
	 * @param attributesAccessor    AttributeAccessor with specific attributes for User
	 * @param user                  User object for who we set the password
	 */
	private static void setPasswordToUser(GuardedStringAccessor guardedStringAccessor, AttributesAccessor attributesAccessor, User user) {
		if (attributesAccessor.getPassword() != null) {
			PasswordProfile passwordProfile = new PasswordProfile();
			passwordProfile.forceChangePasswordNextSignIn = attributesAccessor.findBoolean("forceChangePasswordNextSignIn");

			attributesAccessor.getPassword().access(guardedStringAccessor);

			passwordProfile.password = new String(guardedStringAccessor.getArray());
			user.passwordProfile = passwordProfile;

			guardedStringAccessor.clearArray();
		}
	}

	/**
	 * It will create List of AssignedLicense object which is needed for API
	 *
	 * @param addLicenses List of licenses which contain String representations of UUID
	 * @return List of AssignedLicense
	 */
	private static List<AssignedLicense> prepareAddLicence(List<String> addLicenses) {
//		Byte[] disabledPlansByteArray = attributesAccessor.findByteArray("disabledPlans");
//		Map<String, String> disabledPlans = new HashMap<>();
//		try (ByteArrayInputStream byteIn = new ByteArrayInputStream(ArrayUtils.toPrimitive(disabledPlansByteArray));
//			 ObjectInputStream in = new ObjectInputStream(byteIn)) {
//			disabledPlans = (Map<String, String>) in.readObject();
//		} catch (IOException | ClassNotFoundException e) {
//			LOG.error("Error during map parsing", e);
//		}
		List<AssignedLicense> addLicensesList = new ArrayList<>();
		addLicenses.forEach(licence -> {
			// TODO plans disabling is not supported now so we are setting empty list
			List<UUID> disabledPlansList = new ArrayList<>();

			AssignedLicense license = new AssignedLicense();
			license.disabledPlans = disabledPlansList;
			license.skuId = UUID.fromString(licence);
			addLicensesList.add(license);
		});

		return addLicensesList;
	}

	/**
	 * Update licenses for users. It will perform get to end system and then make diff and decide which licences should be removed and which should be added
	 *
	 * @param addLicenses List with licenses
	 * @param uid         User identification
	 * @param graphClient client for Graph API
	 */
	public static void setLicenses(List<String> addLicenses, String uid, IBaseGraphServiceClient graphClient) {
		List<UUID> removeLicenses = new ArrayList<>();

		try {
			String encodedId = URLEncoder.encode(uid, StandardCharsets.UTF_8.toString());
			if (addLicenses != null) {
				LOG.info("We got some licenses for User");
				// prepare list of licenses which should be removed
				removeLicenses.addAll(getLicensesForUser(encodedId, graphClient)
						.stream()
						.filter(assignedLicense -> !addLicenses.contains(assignedLicense.skuId.toString()))
						.map(assignedLicense -> assignedLicense.skuId)
						.collect(Collectors.toList()));

				LOG.info("We will update licenses for user {0}", uid);
				List<AssignedLicense> assignedLicenses = prepareAddLicence(addLicenses);

				graphClient.users(encodedId)
						.assignLicense(assignedLicenses, removeLicenses)
						.buildRequest()
						.post();
			} else {
				LOG.info("No licenses for User nothing to do");
			}
		} catch (UnsupportedEncodingException e) {
			throw new ConnectorException("Assigning licences failed: ", e);
		}
	}

	/**
	 * Get licenses for user from Graph API
	 *
	 * @param id          User identification
	 * @param graphClient client for Graph API
	 * @return List of AssignedLicense of users licenses
	 */
	public static List<AssignedLicense> getLicensesForUser(String id, IBaseGraphServiceClient graphClient) {
		List<LicenseDetails> licenseDetails = new ArrayList<>();
		LOG.info("Getting licenses");
		ILicenseDetailsCollectionPage licenseDetailsCollectionPage = graphClient.users(id).licenseDetails().buildRequest().get();
		licenseDetails.addAll(licenseDetailsCollectionPage.getCurrentPage());

		LOG.info("First page loaded");
		while (licenseDetailsCollectionPage.getNextPage() != null) {
			LOG.info("Loading next page");
			licenseDetailsCollectionPage = licenseDetailsCollectionPage.getNextPage().buildRequest().get();
			licenseDetails.addAll(licenseDetailsCollectionPage.getCurrentPage());
		}

		return licenseDetails.stream()
				.map(detail -> {
					AssignedLicense assignedLicense = new AssignedLicense();
					assignedLicense.skuId = detail.skuId;
					assignedLicense.disabledPlans = new ArrayList<>();
					return assignedLicense;
				}).collect(Collectors.toList());
	}
}
