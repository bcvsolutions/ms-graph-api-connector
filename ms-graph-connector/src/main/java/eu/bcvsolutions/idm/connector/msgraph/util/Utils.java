package eu.bcvsolutions.idm.connector.msgraph.util;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;

import com.microsoft.graph.models.extensions.DirectoryObject;
import com.microsoft.graph.models.extensions.Group;
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
		builder.setUid(user.id);
		builder.setName(user.id);
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
}
