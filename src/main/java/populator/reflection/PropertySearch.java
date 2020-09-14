package populator.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.GeneratedValue;
import javax.persistence.Table;

public class PropertySearch {

	private PropertyCallBack propertyCallBack;
	private Reflection reflection = Reflection.get();
	private List<String> avoidCyclicField = new ArrayList<String>();

	public PropertySearch(PropertyCallBack propertyCallBack) {
		this.propertyCallBack = propertyCallBack;
	}

	/**
	 * Begins the recursive search for all the fields that need to be set for the
	 * objects that will be persisted.
	 * 
	 * @param rootEntity object from which the recursive search will begin
	 */
	public void start(Object rootEntity) {
		avoidCyclicField.clear();
		startReal(rootEntity, null);
	}

	/**
	 * Real recursive search
	 * @param rootEntity
	 * @param index
	 */
	private void startReal(Object rootEntity, Integer index) {
		if (rootEntity != null) {
			PropertyDescription propertyDescription = new PropertyDescription();
			propertyDescription.setParent(rootEntity);
			if (reflection.isBeanType(rootEntity.getClass())
					&& !Collection.class.isAssignableFrom(rootEntity.getClass())) {
				Table table = rootEntity.getClass().getAnnotation(Table.class);
				if (table != null && table.name().startsWith("V_")) {
					return;
				}
				for (Field field : reflection.getAllDeclaredFields(rootEntity.getClass())) {
					propertyDescription = new PropertyDescription();
					propertyDescription.setParent(rootEntity);
					if (Modifier.isStatic(field.getModifiers()))
						continue;
					String temp = "";
					if (index != null)
						temp = index.toString();
					if (!avoidCyclicField.contains(rootEntity.getClass() + field.toString() + temp)) {
						avoidCyclicField.add(rootEntity.getClass() + field.toString() + temp);
						field.setAccessible(true);
						propertyDescription.setField(field);
						if (reflection.isAnnotationPresent(field, GeneratedValue.class))
							propertyDescription.setIsAutoIncrementId(true);
						if (index != null)
							propertyDescription.setIndex(index);
						propertyCallBack.propertyCallBack(propertyDescription);
						startReal(reflection.getProperty(rootEntity, field), null);
					}

				}
			} else {
				if (index != null)
					propertyDescription.setIndex(index);
				propertyCallBack.propertyCallBack(propertyDescription);
				if (Collection.class.isAssignableFrom(rootEntity.getClass())) {
					Collection<?> collection = (Collection<?>) rootEntity;
					int count = 0;
					if (collection != null) {
						for (Object object : collection) {
							startReal(object, count);
							count++;
						}
					}

				}
			}
		}
	}
}
