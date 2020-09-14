package populator.persitence;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.HashSet;

import javax.persistence.EntityManager;

/**
 * Object used for gathering the required values from persistance context needed
 * for populating.
 * 
 * @author Juan Mu;oz
 *
 */
public class DataHelper {
	private HashSet<String> sequences = new HashSet<String>();

	/**
	 * Returns the value +1 of the amount of already persisted objects linking to
	 * the specified field.
	 * 
	 * @param entityManager
	 * @param rootObject    oblject that contains the field
	 * @param field
	 * @return value that should be set to the field.
	 */
	public Object getNumberOfExistingObject(final EntityManager entityManager, Object rootObject, Field field) {
		String str = "select max(" + field.getName() + ") from " + rootObject.getClass().getName();
		Object object = entityManager.createQuery(str).getSingleResult();
		if (object != null) {
			if (object instanceof Byte) {
				Byte temp = (Byte) object;
				object = ++temp;
			} else if (object instanceof Short) {
				Short temp = (Short) object;
				object = ++temp;
			} else if (object instanceof Integer) {
				Integer temp = (Integer) object;
				object = ++temp;
			} else if (object instanceof Long) {
				Long temp = (Long) object;
				object = ++temp;
			} else if (object instanceof Float) {
				Float temp = (Float) object;
				object = ++temp;
			} else if (object instanceof Double) {
				Double temp = (Double) object;
				object = ++temp;
			} else if (object instanceof String) {
				String result = (String) object;
				StringBuffer b = new StringBuffer();
				char[] chars = result.toCharArray();
				for (char c : chars) {
					if (c != ' ')
						c = (char) (c + 1);
					b.append(c);
				}
				object = b.toString();
			} else {
				throw new RuntimeException("Type not predicted (" + object.getClass() + "). Change PrepareIds.java!");
			}

		} else {
			if (field.getType() == String.class) {
				object = "a";
			} else if (field.getType() == Byte.class) {
				object = new Byte((byte) 0);
			} else if (field.getType() == Short.class) {
				object = new Short((short) 0);
			} else if (field.getType() == Integer.class) {
				object = new Integer(0);
			} else if (field.getType() == Long.class) {
				object = new Long(0);
			} else if (field.getType() == Float.class) {
				object = new Float(0);
			} else if (field.getType() == Double.class) {
				object = new Double(0);
			}
		}
		return object;
	}

	/**
	 * Returns the value of the specified sequence.
	 * 
	 * @param em
	 * @param sequenceName name of the sequence
	 * @return object sequence value
	 */
	public Object getSequenceValue(final EntityManager em, String sequenceName) {
		String queryString = "SELECT " + sequenceName + ".CURRVAL FROM DUAL";
		Object object = em.createNativeQuery(queryString).getSingleResult();
		Object temp = null;
		if (object != null) {
			if (object instanceof Byte) {
				temp = (Byte) object;
			} else if (object instanceof BigInteger) {
				temp = (BigInteger) object;
			} else if (object instanceof Short) {
				temp = (Short) object;
			} else if (object instanceof Integer) {
				temp = (Integer) object;
			} else if (object instanceof Long) {
				temp = (Long) object;
			} else if (object instanceof Float) {
				temp = (Float) object;
			} else if (object instanceof Double) {
				temp = (Double) object;
			} else {
				throw new RuntimeException("Type not predicted (" + object.getClass() + "). Change PrepareIds.java!");
			}

		}
		if (object == null) {
			return new Integer(0);
		} else {
			this.sequences.add(sequenceName);
			return temp;
		}
	}

	/**
	 * Update all used sequences, getting them ready for the next row of objects.
	 * 
	 * @param em
	 */
	public void updateSequences(final EntityManager em) {
		for (String sequenceName : sequences) {
			String queryString = "SELECT " + sequenceName + ".NEXTVAL FROM DUAL";
			em.createNativeQuery(queryString).getSingleResult();
		}
		sequences.clear();
	}

}
