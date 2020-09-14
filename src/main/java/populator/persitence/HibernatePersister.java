package populator.persitence;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.Date;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import populator.reflection.PropertySearch;
import populator.reflection.Reflection;

public class HibernatePersister {

	private EntityManager entityManager;
	private Reflection reflection = Reflection.get();
	private DBHelper dbHelper;
	private List<String> repeatInserts;

	public HibernatePersister(EntityManager entityManager) {
		this.entityManager = entityManager;
		this.repeatInserts = new ArrayList<String>();
	}

	/**
	 * Persist objects onto the database.
	 * 
	 * @param rootEntity
	 * @param specialFields fields overwritten by the user
	 * @param counter
	 * @return local instance of persisted object
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public Object persist(Object rootEntity, Map<String, Object> specialFields, int counter) {
		if (entityManager != null) {
			Connection connection = null;
			try {
				connection = getConnection(entityManager);
				dbHelper = new DBHelper(connection);
				entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE;").executeUpdate();
				return persistReal(rootEntity, entityManager, counter);
			} catch (Throwable t) {

			}
		}
		return rootEntity;
	}

	private Connection getConnection(EntityManager entityManager) {
		// TODO enable others providers
		try {
			Connection connection;
			Object session = entityManager.unwrap(Class.forName("org.hibernate.Session"));
			connection = (Connection) session.getClass().getDeclaredMethod("connection", new Class[0]).invoke(session);
			return connection;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/* PRIVATE */

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	private Object persistReal(Object rootEntity, EntityManager entityManager, int counter) throws Exception {
		final LinkedList<Entry<Object, InsertDeleteQuery>> allToPersist = fillAllToPersist(rootEntity);
		dbHelper.prepareStatments(allToPersist, entityManager);
		LinkedList<Entry<Object, InsertDeleteQuery>> list = new LinkedList<Map.Entry<Object, InsertDeleteQuery>>(
				allToPersist);
		for (Entry<Object, InsertDeleteQuery> entryTemp : list) {
			Object entity = entryTemp.getKey();
			Query insertQuery = entryTemp.getValue().getInsert();
			allToPersist.add(entryTemp); // send to end of queue
			try {
				entityManager.joinTransaction();
				insertQuery.executeUpdate();
				Field fieldId = dbHelper.getId(entity);
				String sqlString = insertQuery.unwrap(org.hibernate.query.Query.class).getQueryString();
				for (int i = 1; i < insertQuery.getParameters().size() + 1; i++) {
					Object value = insertQuery.getParameterValue(i);
					if (value instanceof String || value instanceof Date) {
						sqlString = sqlString.replaceFirst("\\?", "'" + value.toString() + "'");
					} else {
						sqlString = sqlString.replaceFirst("\\?", value.toString());
					}
				}
				repeatInserts.add(sqlString);
				if (fieldId != null) {
					Object id = reflection.getProperty(entity, fieldId);
					if (id != null) {
						notifySubClasses(id, entity, allToPersist);
					} else {
						BigInteger genId = (BigInteger) insertQuery.getSingleResult();
						if (genId != null) {
							if (fieldId.getType() == Long.class) {
								id = new Long(genId.longValue());
								reflection.setProperty(entity, fieldId, id);
							} else if (fieldId.getType() == Integer.class) {
								id = new Integer(genId.intValue());
								reflection.setProperty(entity, fieldId, id);
							} else {
								throw new RuntimeException(
										"Implement type " + fieldId.getType() + " on " + this.getClass().getName());
							}
							notifySubClasses(id, entity, allToPersist);
							if (allToPersist.size() > 1) {
								dbHelper.prepareStatments(allToPersist, entityManager);
								dbHelper.deleteAll(allToPersist);
							}
						}
					}
				}
			} catch (Throwable t) {
				Integer num = new Integer(counter + 1);
				System.out.println(
						"FAIL inserting " + entity.getClass().getSimpleName() + " at cycle: " + num.toString());
			}
		}
		entityManager.joinTransaction();
		entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE;").executeUpdate();
		return rootEntity;
	}

	@Transactional
	public Object persistLogged(Class<?> any, int id) {
		entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE;").executeUpdate();
		repeatInserts.forEach(sqlString -> {
			entityManager.joinTransaction();
			entityManager.createNativeQuery(sqlString).executeUpdate();
		});
		entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE;").executeUpdate();
		return entityManager.find(any, id);
	}

	@Transactional
	public Object persistFile(Class<?> any, int id, String file) {
		try {
			File f = new File(file);
			BufferedReader b = new BufferedReader(new FileReader(f));
			String sqlString = "";
			System.out.println("Reading file using Buffered Reader");
			entityManager.joinTransaction();
			entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE;").executeUpdate();
			while ((sqlString = b.readLine()) != null) {
				entityManager.joinTransaction();
				entityManager.createNativeQuery(sqlString).executeUpdate();
			}
			entityManager.joinTransaction();
			entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE;").executeUpdate();
			b.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return entityManager.find(any, id);
	}

	@Transactional
	public void persistFile(String file) {
		try {
			File f = new File(file);
			BufferedReader b = new BufferedReader(new FileReader(f));
			String sqlString = "";
			System.out.println("Reading file using Buffered Reader");
			entityManager.joinTransaction();
			entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE;").executeUpdate();
			while ((sqlString = b.readLine()) != null) {
				entityManager.joinTransaction();
				entityManager.createNativeQuery(sqlString).executeUpdate();
			}
			entityManager.joinTransaction();
			entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE;").executeUpdate();
			b.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void saveFile(String filename) {
		entityManager.createNativeQuery("SCRIPT SIMPLE TO '" + filename + "'").getResultList();
		String inputFileName = filename;
		String outputFileName = "populator.tmp";
		String lineToSearch = "INSERT INTO";
		try {
			File inputFile = new File(inputFileName);
			File outputFile = new File(outputFileName);
			if (outputFile.exists()) {
				outputFile.delete();
				outputFile.createNewFile();
			}
			try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
					BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
				String line = null;
				while ((line = reader.readLine()) != null) {
					if (line.startsWith(lineToSearch)) {
						writer.write(line);
						writer.newLine();
					}
				}
			}
			if (inputFile.delete()) {
				// Rename the output file to the input file
				if (!outputFile.renameTo(inputFile)) {
					throw new IOException("Could not rename " + outputFileName + " to " + inputFileName);
				}
			} else {
				throw new IOException("Could not delete original input file " + inputFileName);
			}
		} catch (IOException ex) {
			// Handle any exceptions
			ex.printStackTrace();
		}
	}

	private LinkedList<Entry<Object, InsertDeleteQuery>> fillAllToPersist(Object rootEntity) {
		final LinkedList<Entry<Object, InsertDeleteQuery>> allToPersist = new LinkedList<Entry<Object, InsertDeleteQuery>>();
		new PropertySearch(propertyDescription -> {

			Object object = propertyDescription.getParent();
			if (object.getClass().isAnnotationPresent(Entity.class)) {
				for (Entry<Object, InsertDeleteQuery> entry : allToPersist)
					if (entry.getKey().equals(object))
						return;
				allToPersist.add(new SimpleEntry<Object, InsertDeleteQuery>(propertyDescription.getParent(), null));
			}

		}).start(rootEntity);
		return allToPersist;
	}

	private void notifySubClasses(Object id, Object entity, LinkedList<Entry<Object, InsertDeleteQuery>> allToPersist) {
		for (Entry<Object, InsertDeleteQuery> entry : allToPersist) {
			if (entry.getKey().getClass().getSuperclass() != Object.class) {
				if (entry.getKey().getClass().getSuperclass() == entity.getClass()) {
					Field fieldId = dbHelper.getId(entry.getKey());
					reflection.setProperty(entry.getKey(), fieldId, id);
				}
			}
		}
	}
}
