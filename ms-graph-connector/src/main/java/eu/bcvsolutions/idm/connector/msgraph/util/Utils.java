package eu.bcvsolutions.idm.connector.msgraph.util;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributesAccessor;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;

import com.microsoft.graph.models.extensions.DirectoryObject;
import com.microsoft.graph.models.extensions.Group;
import com.microsoft.graph.models.extensions.PasswordProfile;
import com.microsoft.graph.models.extensions.User;

/**
 * @author Roman Kučera
 */
public final class Utils {

	private static final Log LOG = Log.getLog(Utils.class);

	private Utils() {

	}

	public static ConnectorObject handleUser(User user, ObjectClass objectClass) {
		ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
		builder.setUid(user.userPrincipalName);
		builder.setName(user.userPrincipalName);
		builder.setObjectClass(objectClass);

		Field[] declaredFields = User.class.getDeclaredFields();
		addAttributeToBuilder(user, builder, declaredFields);

		return builder.build();
	}

	public static ConnectorObject handleGroup(Group group, ObjectClass objectClass) {
		ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
		builder.setUid(group.id);
		builder.setName(group.id);
		builder.setObjectClass(objectClass);

		Field[] declaredFields = Group.class.getDeclaredFields();
		addAttributeToBuilder(group, builder, declaredFields);

		return builder.build();
	}

	private static void addAttributeToBuilder(DirectoryObject object, ConnectorObjectBuilder builder, Field[] declaredFields) {
		Class<?> clazz = object.getClass();
		Arrays.stream(declaredFields).forEach(field -> {
			try {
				Field objectField = clazz.getField(field.getName());
				if (objectField.getType() == String.class || objectField.getType() == Boolean.class || objectField.getType() == Integer.class) {
					Object fieldValue = objectField.get(object);
					builder.addAttribute(field.getName(), fieldValue);
				} else {
					LOG.info("Field type {0} not supported now", objectField.getType().getName());
				}
			} catch (NoSuchFieldException | IllegalAccessException e) {
				LOG.error("Error when getting field {0}: {1}", field.getName(), e);
			}
		});
	}

	public static User prepareUserObject(Set<Attribute> updateAttributes, GuardedStringAccessor guardedStringAccessor) {
		AttributesAccessor attributesAccessor = new AttributesAccessor(updateAttributes);

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
			} catch (NoSuchFieldException | IllegalAccessException e) {
				// TODO better message now for example if atribut is __NAME__ it will throw NoSuchFieldException but in log is this bad message
				LOG.error("Error during object preparation", e);
			}
		});
		return user;
	}

	private static void setPasswordToUser(GuardedStringAccessor guardedStringAccessor, AttributesAccessor attributesAccessor, User user) {
		if (attributesAccessor.getPassword() != null) {
			PasswordProfile passwordProfile = new PasswordProfile();
			passwordProfile.forceChangePasswordNextSignIn = attributesAccessor.findBoolean("forceChangePasswordNextSignIn");

			attributesAccessor.getPassword().access(guardedStringAccessor);

			passwordProfile.password = new String(guardedStringAccessor.getArray());

			guardedStringAccessor.clearArray();

			user.passwordProfile = passwordProfile;
		}
	}
}
