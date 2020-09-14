package populator.instance;

import javax.persistence.EntityManager;

import populator.persitence.HibernatePersister;
import populator.reflection.Reflection;

@SuppressWarnings("unchecked")
public class Populator {

	private FluentApi fluentApi = Defaults.get(0);
	private Reflection reflection = Reflection.get();
	private HibernatePersister hibernatePersister;
	private int counter = 0;

	public Populator(EntityManager entityManager) {
		hibernatePersister = new HibernatePersister(entityManager);
		fluentApi.setEm(entityManager);
	}

	/**
	 * When populating the database, specify the class that should be overwriten
	 * with a custom value.
	 * 
	 * @param anyClass .class of object to be overwriten
	 * @return Populator object for daisy chaining of rules
	 */
	public Populator when(Class<?> anyClass) {
		fluentApi.putKey(new SmartClass(anyClass));
		return this;
	}

	/**
	 * When populating the database, specify the class that should be overwriten
	 * with a custom value.
	 * 
	 * @param anyClass  .class of object that contains the field to be overwriten
	 * @param fieldname name of the field to be overwriten
	 * @return Populator object for daisy chaining of rules
	 */
	public Populator when(Class<?> anyClass, String fieldName) {
		fluentApi.putKey(new SmartClass(reflection.getField(anyClass, fieldName)));
		return this;
	}

	/**
	 * When populating the database, specify the object that will replace the
	 * previuosly specified class of field.
	 * 
	 * @param any
	 * @return same Populator object for daisy chaining of rules
	 */
	public Populator thenInject(Object any) {
		fluentApi.putValue(any);
		return this;
	}

	/**
	 * Populate the database using the specified rules.
	 * 
	 * @param any .class of the root object to persist
	 * @return local instance of the root object
	 */
	public <T> T populate(Class<T> any) {
		Object obj = fluentApi.getValue(new SmartClass(any));
		hibernatePersister.persist(obj, fluentApi.getSpecialFields(), getCounter());
		incCounter();
		return (T) obj;
	}

	/**
	 * Populate the database using a backup object, useful for changes between
	 * single tests.
	 * 
	 * @param any .class of the root object to persist
	 * @param id  ID of the root object that shouldbe returned
	 * @return persisted object that matched with the given ID
	 */
	public Object populateWithLogs(Class<?> any, int id) {
		return hibernatePersister.persistLogged(any, id);
	}

	/**
	 * Save an SQL file containing the populated objets on the database.
	 * 
	 * @param file path + filename for the log file
	 */
	public void saveFile(String file) {
		hibernatePersister.saveFile(file);
	}

	/**
	 * Populate the database using a backup file, useful for multiple runs on the
	 * same test class.
	 * 
	 * @param file path + filename for the log file
	 * @param any  .class of the root object to persist
	 * @param id   ID of the root object that shouldbe returned
	 * @return persisted object that matched with the given ID
	 */
	public Object populateFromFile(String file, Class<?> any, int id) {
		return hibernatePersister.persistFile(any, id, file);
	}

	/**
	 * Populate the database using a backup file, useful for multiple runs on the
	 * same test class.
	 * 
	 * @param file path + filename for the log file
	 */
	public void populateFromFile(String file) {
		hibernatePersister.persistFile(file);
	}
	
	public int getCounter() {
		return counter;
	}

	public void setCounter(int counter) {
		this.counter = counter;
	}

	public void incCounter() {
		setCounter(getCounter() + 1);
	}

	public void clear(Class<?> clazz) {
		throw new RuntimeException("Not implemented yet");
	}

}
