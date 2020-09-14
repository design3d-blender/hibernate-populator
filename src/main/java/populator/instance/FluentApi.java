package populator.instance;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import populator.persitence.DataHelper;
import populator.reflection.PropertySearch;
import populator.reflection.Reflection;
import populator.reflection.ValidationsSearch;

@SuppressWarnings("unchecked")
public class FluentApi {

	private Reflection reflection = Reflection.get();
	private SmartMap<SmartClass, Object> smartMap = new SmartMap<>();
	public HashMap<String, Object> specialFields = new HashMap<>();
	private EntityManager em;
	private DataHelper dataHelper = new DataHelper();

	/**
	 * @param em the em to set
	 */
	public void setEm(EntityManager em) {
		this.em = em;
	}

	public Map<String, Object> getSpecialFields() {
		return specialFields;
	}

	// when
	public FluentApi putKey(SmartClass smartClass) {
		smartMap.put(smartClass, smartMap.remove(smartClass));
		return this;
	}

	// thenInject
	public void putValue(Object rootValue) {
		if (rootValue == null) {
			if (smartMap.getLastKey().getField() != null) {
				specialFields.put(smartMap.getLastKey().getField().toString() + "0", null);
			} else {
				specialFields.put(smartMap.getLastKey().getBeanType().toString(), null);
			}
		} else {
			if (smartMap.getLastKey().getField() == null)
				smartMap.put(smartMap.getLastKey(), rootValue);
			new PropertySearch(propertyDescription -> {

				Object value = propertyDescription.getParent();
				Field field = propertyDescription.getField();

				if (field != null) {
					Object fromMap = smartMap.get(new SmartClass(field));
					Object fromReal = reflection.getProperty(value, field);

					if (fromReal != null && !fromMap.equals(fromReal))
						specialFields.put(field.toString() + propertyDescription.getIndex(), fromReal);

				} else {
					Field fieldTemp = smartMap.getLastKey().getField();
					if (fieldTemp != null && fieldTemp.getType().isAssignableFrom(value.getClass()))
						specialFields.put(fieldTemp.toString() + propertyDescription.getIndex(), value);
				}

			}).start(rootValue);
		}
	}

	// mock
	/**
	 * Get a mock of the root object and their different fields, while maintaining
	 * bean validations
	 * 
	 * @param smartClass
	 * @return
	 */
	public Object getValue(SmartClass smartClass) {
		Object rootValue = smartMap.get(smartClass);
		HashMap<String, Object> sequenceFields = new HashMap<>();
		ValidationsSearch validations = new ValidationsSearch();
		validations.start(rootValue);
		validations.getSequenceDescription().getSequences().forEach((rootObject, children) -> {
			children.forEach((fieldPlusIndex, sequenceName) -> {
				sequenceFields.put(fieldPlusIndex, dataHelper.getSequenceValue(em, sequenceName));
			});
		});
		new PropertySearch(propertyDescription -> {

			Object rootObject = propertyDescription.getParent();

			if (rootObject.getClass().isEnum())
				return;
			Field field = propertyDescription.getField();
			if (field == null)
				return;
			if (validations.getEmbeddableDescriptions().contains(rootObject.getClass().getSimpleName())) {
				return;
			}
			SmartClass smartClassField = new SmartClass(field);
			Object property = smartMap.get(smartClassField);
			if (specialFields.containsKey(field.toString() + propertyDescription.getIndex())
					|| specialFields.containsKey(smartClassField.getBeanType().toString())) {

				Object specialValue = specialFields.get(field.toString() + propertyDescription.getIndex());
				if (specialValue instanceof BigInteger) {
					BigInteger specialValueTemp = (BigInteger) specialValue;
					if (field.getType() == Integer.class) {
						specialValue = new Integer(specialValueTemp.intValue());
					} else if (field.getType() == Long.class) {
						specialValue = new Long(specialValueTemp.longValue());
					}
				}
				reflection.setProperty(rootObject, field, specialValue);

				// En teoria el AND corta si es falso, asi que no deberia haber
				// NullPointerException
			} else if (sequenceFields.containsKey(field.toString() + propertyDescription.getIndex())
					|| sequenceFields.containsKey(smartClassField.getBeanType().toString())) {

				Object specialValue = sequenceFields.get(field.toString() + propertyDescription.getIndex());
				if (specialValue instanceof BigInteger) {
					BigInteger specialValueTemp = (BigInteger) specialValue;
					if (field.getType() == Integer.class) {
						specialValue = new Integer(specialValueTemp.intValue());
					} else if (field.getType() == Long.class) {
						specialValue = new Long(specialValueTemp.longValue());
					}
				}
				reflection.setProperty(rootObject, field, specialValue);

				// En teoria el AND corta si es falso, asi que no deberia haber
				// NullPointerException
			} else if (validations.getUniquesMap().containsEntry(rootObject.getClass().getSimpleName(), field.getName())) {
				reflection.setProperty(rootObject, field, dataHelper.getNumberOfExistingObject(em, rootObject, field));
			} else {

				reflection.setProperty(rootObject, field, property);
				if (smartClassField.isCollection())
					((Collection<Object>) property).add(smartMap.get(new SmartClass(smartClassField.getBeanType())));
			}

		}).start(rootValue);
		dataHelper.updateSequences(em);
		return rootValue;
	}

}
