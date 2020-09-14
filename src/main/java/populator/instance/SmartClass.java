package populator.instance;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.WildcardType;
import java.util.Collection;

public class SmartClass {
 
	private Class<?> collectionType;
	private Class<?> beanType;
	private Field field;

	public Class<?> getCollectionType() {
		return collectionType;
	}

	public void setCollectionType(Class<?> collectionType) {
		this.collectionType = collectionType;
	}

	public Class<?> getBeanType() {
		return beanType;
	}

	public void setBeanType(Class<?> beanType) {
		this.beanType = beanType;
	}

	public Field getField() {
		return field;
	}

	public void setField(Field field) {
		this.field = field;
	}

	public SmartClass(Class<?> clazz) {
		if (isCollection(clazz)) {
			collectionType = clazz;
		} else {
			beanType = clazz;
		}
	}

	public SmartClass(Field field) {
		field.setAccessible(true);
		this.field = field;
		Class<?> fieldType = field.getType();
		if (isCollection(fieldType)) {
			collectionType = fieldType;
			beanType = getCollectionGenericType(field);
		} else {
			beanType = fieldType;
		}
	}

	public boolean isCollection() {
		return collectionType != null;
	}

	public Class<?> getTrueType() {
		return collectionType != null ? collectionType : beanType;
	}

	private Class<?> getCollectionGenericType(Field field) {
		Object obj = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
		if (obj instanceof Class<?>) {
			return (Class<?>) obj;
		} else if (obj instanceof WildcardType) {
			return Object.class;
		} else {
			throw new RuntimeException("Collection generic type problem");
		}
	}

	private boolean isCollection(Class<?> clazz) {
		return Collection.class.isAssignableFrom(clazz);
	}

	@Override public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getTrueType() == null) ? 0 : getTrueType().hashCode());
		return result;
	}

	@Override public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SmartClass other = (SmartClass) obj;
		if (getTrueType() == null) {
			if (other.getTrueType() != null)
				return false;
		} else if (!getTrueType().equals(other.getTrueType()))
			return false;
		return true;
	}

}
