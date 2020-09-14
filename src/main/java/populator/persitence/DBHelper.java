package populator.persitence;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Query;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import populator.instance.SmartClass;
import populator.reflection.Reflection;

public class DBHelper {

	Connection connection;
	List<String> constraints;
	Reflection reflection = Reflection.get();

	public DBHelper(Connection connection) {
		this.connection = connection;
	}

	public DBHelper() {

	}

	public List<String> getTables() throws SQLException {
		List<String> tables = new ArrayList<String>();
		ResultSet rs = connection.getMetaData().getTables(null, null, "%", new String[] { "TABLE" });
		while (rs.next())
			tables.add(rs.getString(3));
		return tables;
	}

	public void disableConstaints() {
		try {
			constraints = new ArrayList<String>();
			if (isMysql(connection)) {
				Statement stmt = connection.createStatement();
				stmt.execute("SET FOREIGN_KEY_CHECKS=0");
				stmt.close();
			} else if (isH2(connection)) {
				Statement stmt = connection.createStatement();
				stmt.execute("SET REFERENTIAL_INTEGRITY FALSE;");
				stmt.close();
			} else if (isSQLServer(connection)) {
				Statement stmt = connection.createStatement();
				stmt.execute("ALTER TABLE ? NOCHECK CONSTRAINT all");
				stmt.close();
			} else if (isOracle(connection)) {
				StringBuilder procedure = new StringBuilder();
				procedure.append(" BEGIN");
				procedure.append("   FOR c IN");
				procedure.append("   (SELECT c.owner, c.table_name, c.constraint_name");
				procedure.append("    FROM user_constraints c, user_tables t");
				procedure.append("    WHERE c.table_name = t.table_name");
				procedure.append("    AND c.status = 'ENABLED'");
				procedure.append("    ORDER BY c.constraint_type DESC)");
				procedure.append("   LOOP");
				procedure.append(
						"     dbms_utility.exec_ddl_statement('alter table ' || c.owner || '.' || c.table_name || ' disable constraint ' || c.constraint_name);");
				procedure.append("  END LOOP;");
				procedure.append(" END;");
				Statement stmt = connection.createStatement();
				stmt.executeUpdate(procedure.toString());
				stmt.close();
			} else {
				throw new RuntimeException(
						"disableConstaints not implemented for " + connection.getMetaData().getDatabaseProductName());
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void enableConstaints() {
		try {
			if (isMysql(connection)) {
				Statement stmt = connection.createStatement();
				stmt.execute("SET FOREIGN_KEY_CHECKS=1");
				stmt.close();
			} else if (isH2(connection)) {
				Statement stmt = connection.createStatement();
				stmt.execute("SET REFERENTIAL_INTEGRITY TRUE;");
				stmt.close();
			} else if (isSQLServer(connection)) {
				Statement stmt = connection.createStatement();
				stmt.execute("ALTER TABLE ? CHECK CONSTRAINT all");
				stmt.close();
			} else if (isOracle(connection)) {
				StringBuilder procedure = new StringBuilder();
				procedure.append(" BEGIN");
				procedure.append("   FOR c IN");
				procedure.append("   (SELECT c.owner, c.table_name, c.constraint_name");
				procedure.append("    FROM user_constraints c, user_tables t");
				procedure.append("    WHERE c.table_name = t.table_name");
				procedure.append("    AND c.status = 'DISABLED'");
				procedure.append("    ORDER BY c.constraint_type)");
				procedure.append("   LOOP");
				procedure.append(
						"     dbms_utility.exec_ddl_statement('alter table ' || c.owner || '.' || c.table_name || ' enable constraint ' || c.constraint_name);");
				procedure.append("   END LOOP;");
				procedure.append(" END;");
				Statement stmt = connection.createStatement();
				stmt.executeUpdate(procedure.toString());
				stmt.close();
			} else {
				throw new RuntimeException(
						"enableConstaints not implemented for " + connection.getMetaData().getDatabaseProductName());
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isMysql(Connection connection) {
		try {
			return connection.getMetaData().getURL().contains(":mysql:");
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isH2(Connection connection) {
		try {
			return connection.getMetaData().getURL().contains(":h2:");
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isSQLServer(Connection connection) {
		try {
			return connection.getMetaData().getURL().contains(":sqlserver:");
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isOracle(Connection connection) {
		try {
			return connection.getMetaData().getURL().contains(":oracle:");
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void prepareStatments(LinkedList<Entry<Object, InsertDeleteQuery>> allToPersist, EntityManager em) throws SQLException {
		LinkedList<Entry<Object, InsertDeleteQuery>> hidden = new LinkedList<Map.Entry<Object, InsertDeleteQuery>>();
		for (Entry<Object, InsertDeleteQuery> entry : allToPersist) {
//			if (entry.getValue() != null) {
//				entry.getValue().getInsert().close();
//				entry.getValue().getDelete().close();
//			}
			createStatement(entry, em);
			List<Entry<Object, InsertDeleteQuery>> entryTemp = createStatementForHiddenTables(entry.getKey(),
					allToPersist, em);
			hidden.addAll(entryTemp);
		}

		for (Entry<Object, InsertDeleteQuery> hid : hidden) {
			boolean add = true;
			for (Entry<Object, InsertDeleteQuery> entryTemp : allToPersist) {
				if (entryTemp.getKey().equals(hid.getKey())) {
					entryTemp.setValue(hid.getValue());
					add = false;
					break;
				}
			}
			if (add)
				allToPersist.add(hid);
		}

	}

	public Field getId(Object object) {
		List<Field> fields;
		if (object instanceof Class<?>) {
			fields = reflection.getAllDeclaredFields((Class<?>) object);
		} else {
			fields = reflection.getAllDeclaredFields(object.getClass());
		}

		for (Field field : fields)
			if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())
					&& !Modifier.isPublic(field.getModifiers()))
				return field;
		return null;
	}

	public Field getIdField(Object object) {
		List<Field> fields;
		if (object instanceof Class<?>) {
			fields = reflection.getAllDeclaredFields((Class<?>) object);
		} else {
			fields = reflection.getAllDeclaredFields(object.getClass());
		}

		for (Field field : fields)
			if (field.isAnnotationPresent(Id.class))
				return field;
		return null;
	}

	public Method getIdMethod(Object object) {
		Method method = null;
		Class<?>[] parameter = null;
		try {
			method = object.getClass().getMethod("getId", parameter);
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (method.isAnnotationPresent(SequenceGenerator.class))
			return method;
		return null;
	}

	public void deleteAll(LinkedList<Entry<Object, InsertDeleteQuery>> deletes) {
		for (Entry<Object, InsertDeleteQuery> entry : deletes) {
			Query deleteQuery = entry.getValue().getDelete();
			try {
				deleteQuery.executeUpdate();
			} catch (Exception e) {
			} finally {
				deletes.add(entry);
			}
		}
	}

	/* PRIVATE */

	private void createStatement(Entry<Object, InsertDeleteQuery> entry, EntityManager em) {
		if (entry.getKey().getClass() == String.class)
			return;
		Map<String, Object> values = getMap(entry.getKey(), em);
		StringBuilder sql = new StringBuilder();
		sql.append(" INSERT INTO " + getTableName(entry.getKey().getClass()));
		sql.append(" (" + prepareColumn(values, entry.getKey()) + ")");
		sql.append(" VALUES (" + prepareHolder(values, entry.getKey()) + ")");
		try {
			InsertDeleteQuery insertDeletQuery = new InsertDeleteQuery();
			String sqlString = sql.toString();
			Query insertQuery = em.createNativeQuery(sqlString);
			insertDeletQuery.setInsert(insertQuery);
			int count = 1;
			Object idValue;
			Field fieldId = getIdField(entry.getKey());
			String id;
			Method method = null;
			Class<?>[] parameter = null;
			try {
				method = entry.getKey().getClass().getMethod("getId", parameter);
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (fieldId == null) {
				id = getColumnNameMethod(method, entry.getKey().getClass().getSimpleName());
				idValue = method;
			} else {
				id = getColumnName(fieldId, entry.getKey().getClass().getSimpleName());
				idValue = fieldId;
			}
			for (Entry<String, Object> entryTemp : values.entrySet()) {
				Object temp = entryTemp.getValue();
				if (temp != null && temp.getClass().isEnum()) {
					Field ordinal = reflection.getField(temp.getClass(), "ordinal");
					insertQuery.setParameter(count, reflection.getProperty(temp, ordinal));
					// TODO:
					// try
					// find
					// @Enumerated
				} else {
					if (temp instanceof Date) {
						Date dt = (Date) temp;
						insertQuery.setParameter(count, new java.sql.Date(dt.getTime()));
					} else {
						insertQuery.setParameter(count, temp);
					}
				}
				count++;
			}
			
			/********** DELETE *********/

			String del = "delete from " + getTableName(entry.getKey().getClass()) + " where " + id + " = ?";
			Query deleteQuery = em.createNativeQuery(del);
			if (idValue.getClass().equals(Field.class)) {
				deleteQuery.setParameter(1, reflection.getProperty(entry.getKey(), (Field) idValue));
			}
			else {
				deleteQuery.setParameter(1, reflection.getProperty(entry.getKey(), (Method) idValue));
			}
			insertDeletQuery.setDelete(deleteQuery);

			entry.setValue(insertDeletQuery);

		} catch (

		Throwable t) {
			throw new RuntimeException(t);
		}
	}

	private List<Entry<Object, InsertDeleteQuery>> createStatementForHiddenTables(Object obj,
			LinkedList<Entry<Object, InsertDeleteQuery>> allToPersist, EntityManager em) {
		List<Entry<Object, InsertDeleteQuery>> entryTemp = new LinkedList<Map.Entry<Object, InsertDeleteQuery>>();
		List<Field> fields = reflection.getAllDeclaredFields(obj.getClass());
		for (Field field : fields) {
			OneToMany otm = (OneToMany) reflection.getAnnotation(field, OneToMany.class);
			ManyToMany mtm = (ManyToMany) reflection.getAnnotation(field, ManyToMany.class);
//			ManyToOne mto = (ManyToOne) reflection.getAnnotation(field, ManyToOne.class);
			if (otm != null && otm.mappedBy().equals("")) {
				Class<?> clazzTemp = obj.getClass();
				if (obj.getClass().getSuperclass() != Object.class) {
					Inheritance inheritance = obj.getClass().getSuperclass().getAnnotation(Inheritance.class);
					if (inheritance != null && inheritance.strategy() == InheritanceType.JOINED) {
						clazzTemp = obj.getClass().getSuperclass();
					}
				}
				String table1 = getTableName(clazzTemp);
				Class<?> beanType = new SmartClass(field).getBeanType();
				String table2 = getTableName(beanType);
				String table1Table2 = table1 + "_" + table2;
				Field fieldId = getId(obj);
				Field fieldIdDep = getId(new SmartClass(field).getBeanType());
				String sql = "INSERT INTO " + table1Table2 + " (" + table1 + "_"
						+ getColumnName(getId(obj), obj.getClass().getSimpleName()) + ","
						+ getColumnName(field, obj.getClass().getSimpleName()) + "_"
						+ getColumnName(fieldIdDep, obj.getClass().getSimpleName()) + ") VALUES (?,?)";
				try {
					Collection<?> coll = (Collection<?>) reflection.getProperty(obj, field);

					for (Object dep : coll) {
						Query insertQuery = em.createNativeQuery(sql);
						Object id = reflection.getProperty(obj, fieldId);
						insertQuery.setParameter(1, id);
						Field fieldIdDep2 = getId(dep);
						Object idDep = reflection.getProperty(dep, fieldIdDep2);
						insertQuery.setParameter(2, idDep);
						String del = "delete from " + table1Table2 + " where " + table1 + "_"
								+ getColumnName(getId(obj), obj.getClass().getSimpleName()) + " = ?";
						Query deleteQuery = em.createNativeQuery(del);
						insertQuery.setParameter(1, id);
						InsertDeleteQuery insertDeletQuery = new InsertDeleteQuery();
						insertDeletQuery.setInsert(insertQuery);
						insertDeletQuery.setDelete(deleteQuery);

						entryTemp.add(
								new SimpleEntry<Object, InsertDeleteQuery>(table1Table2 + idDep, insertDeletQuery));
					}
				} catch (Throwable t) {
					throw new RuntimeException(t);
				}
			}
			if (mtm != null && mtm.mappedBy().equals("")) {

				String tableInter = null;
				String column1 = null;
				String column2 = null;

				JoinTable jt = (JoinTable) reflection.getAnnotation(field, JoinTable.class);
				if (jt != null && jt.name() != null) {
					tableInter = jt.name();
					// TODO que array raro
					column1 = jt.joinColumns()[0].name();
					column2 = jt.inverseJoinColumns()[0].name();
				} else {
					Class<?> clazzTemp = obj.getClass();
					if (obj.getClass().getSuperclass() != Object.class) {
						Inheritance inheritance = obj.getClass().getSuperclass().getAnnotation(Inheritance.class);
						if (inheritance != null && inheritance.strategy() == InheritanceType.JOINED) {
							clazzTemp = obj.getClass().getSuperclass();
						}
					}
					String table1 = getTableName(clazzTemp);

					Class<?> beanType = new SmartClass(field).getBeanType();
					String table2 = getTableName(beanType);
					tableInter = table1 + "_" + table2;
					Field fieldIdDep = getId(new SmartClass(field).getBeanType());
					column1 = table1 + "_" + getColumnName(getId(obj), obj.getClass().getSimpleName());
					column2 = getColumnName(field, obj.getClass().getSimpleName()) + "_"
							+ getColumnName(fieldIdDep, obj.getClass().getSimpleName());

				}

				String sql = "INSERT INTO " + tableInter + " (" + column1 + "," + column2 + ") VALUES (?,?)";
				try {
					Collection<?> coll = (Collection<?>) reflection.getProperty(obj, field);
					for (Object dep : coll) {
						Query insertQuery = em.createNativeQuery(sql);
						Field fieldId = getId(obj);
						Object id = reflection.getProperty(obj, fieldId);
						insertQuery.setParameter(1, id);
						Field fieldIdDep2 = getId(dep);
						Object idDep = reflection.getProperty(dep, fieldIdDep2);
						insertQuery.setParameter(2, idDep);

						String del = "delete from " + tableInter + " where " + column1 + " = ?";
						Query deleteQuery = em.createNativeQuery(del);
						deleteQuery.setParameter(1, id);

						InsertDeleteQuery insertDeletQuery = new InsertDeleteQuery();
						insertDeletQuery.setInsert(insertQuery);
						insertDeletQuery.setDelete(deleteQuery);

						entryTemp.add(new SimpleEntry<Object, InsertDeleteQuery>(tableInter + idDep, insertDeletQuery));
					}
				} catch (Throwable t) {
					throw new RuntimeException(t);
				}
			}
		}

		return entryTemp;
	}

	private String prepareColumn(Map<String, Object> values, Object obj) {
		String columns = "";
		for (Entry<String, Object> entry : values.entrySet())
			columns += "," + entry.getKey();
		return columns.substring(1);
	}

	private String getTableName(Class<?> clazz) {
		if (clazz.isAnnotationPresent(Table.class)) {
			Table table = clazz.getAnnotation(Table.class);
			if (table.name() != null)
				return table.name();
		}
		return clazz.getSimpleName();
	}

	private String prepareHolder(Map<String, Object> values, Object obj) {
		String columns = "";
		for (int i = 0; i < values.size(); i++) {
			columns += ",?";
		}
		return columns.substring(1);
	}

	private Map<String, Object> getMap(Object object, EntityManager em) {
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		List<Field> fields = reflection.getAllDeclaredFields(object.getClass());
		
		//If you use an object to specify if the object has logical deletion, add it so here
//		if (LogicalDeletion.class.isAssignableFrom(object.getClass())) {
//			Integer statusInteger = 0;
//			map.put("STATUS", statusInteger);
//		}
		for (Field field : fields) {
			if (object.getClass().getSuperclass() != Object.class) {
				Inheritance inheritance = object.getClass().getSuperclass().getAnnotation(Inheritance.class);
				if (inheritance != null && inheritance.strategy() == InheritanceType.JOINED) {
					if (!field.isAnnotationPresent(Id.class)) {
						try {
							object.getClass().getSuperclass().getDeclaredField(field.getName());
							continue;
						} catch (Exception e) {
							// not superclass field
						}
					}
				}
			}
			if ((field.isAnnotationPresent(Column.class) || field.isAnnotationPresent(JoinColumn.class)
					|| field.isAnnotationPresent(EmbeddedId.class))) {

				if (field.isAnnotationPresent(EmbeddedId.class)) {
					String className = field.getType().getName();
					if (className.equals("java.io.Serializable")) {
						continue;
					}
					Class<?> clazz;
					Object embeddedObj = null;
					try {
						clazz = Class.forName(className);
						embeddedObj = clazz.newInstance();

					} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					Map<String, Object> tempMap = getMap(embeddedObj, em);
					map.putAll(tempMap);
					continue;
				}

				if (field.isAnnotationPresent(Id.class)) {
					String columnName = getColumnName(field, object.getClass().getSimpleName());
					if (field.isAnnotationPresent(Column.class)) {
						Annotation idColumnAnnotation = field.getAnnotation(Column.class);
						if (idColumnAnnotation instanceof Column) {
							Column columnId = (Column) idColumnAnnotation;
							columnName = columnId.name();
						}
					}

					if (!Collection.class.isAssignableFrom(field.getType())) {
						Object obj = reflection.getProperty(object, field);
						map.put(columnName, obj);
					}
				}
				if (!field.isAnnotationPresent(Transient.class) && !Modifier.isStatic(field.getModifiers())
						&& !Modifier.isFinal(field.getModifiers())) {
					String columnName = getColumnName(field, object.getClass().getSimpleName());
					if (!Collection.class.isAssignableFrom(field.getType())) {
						Object obj = reflection.getProperty(object, field);
						if (obj != null && obj.getClass() == String.class) {
							String value = obj.toString();
							Integer length = getLength(field, object.getClass().getSimpleName());
							if (length != null && value.length() > length) {
								obj = value.substring(0, length);
							}
						}
						if (obj != null && obj.getClass().isAnnotationPresent(Entity.class)) {
							obj = reflection.getProperty(obj, getId(obj));
						}
						map.put(columnName, obj);
					}
				}

			}
		}
		for (Method method : object.getClass().getMethods()) {
			if (method.isAnnotationPresent(Column.class) || method.isAnnotationPresent(JoinColumn.class)
					|| method.isAnnotationPresent(EmbeddedId.class)) {

				if (method.isAnnotationPresent(EmbeddedId.class)) {
					String className = method.getReturnType().getName();
					if (className.equals("java.io.Serializable")) {
						continue;
					}
					Class<?> clazz;
					Object embeddedObj = null;
					try {
						clazz = Class.forName(className);
						embeddedObj = clazz.newInstance();

					} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					Map<String, Object> tempMap = getMap(embeddedObj, em);
					map.putAll(tempMap);
					continue;
				}
				if (method.isAnnotationPresent(Id.class)) {
					String columnName = getColumnNameMethod(method, object.getClass().getSimpleName());
					if (method.isAnnotationPresent(Column.class)) {
						Annotation annotation = method.getAnnotation(Column.class);
						if (annotation instanceof Column) {
							Column columnId = (Column) annotation;
							columnName = columnId.name();
						}
					}
					if (!Collection.class.isAssignableFrom(method.getReturnType())) {
						Object obj = reflection.getProperty(object, method);
						map.put(columnName, obj);
					}

				}
				if (!method.isAnnotationPresent(Transient.class)) {
					String columnName = getColumnNameMethod(method, object.getClass().getSimpleName());
					if (!Collection.class.isAssignableFrom(method.getReturnType())) {
						Object obj = reflection.getProperty(object, method);
						if (obj != null && obj.getClass() == String.class) {
							String value = obj.toString();
							Integer length = getLength(method, object.getClass().getSimpleName());
							if (isUnique(method, object.getClass().getSimpleName())) {
								StringBuffer b = new StringBuffer();
								char[] chars = ((String) obj).toCharArray();
								for (char c : chars) {
									if (c != ' ')
										c = (char) (c + 1);
									b.append(c);
								}
							}
							if (length != null && value.length() > length) {
								obj = value.substring(0, length);
							}
						}
						if (obj != null && obj.getClass().isAnnotationPresent(Entity.class)) {
							obj = reflection.getProperty(obj, getId(obj));
						}
						map.put(columnName, obj);
					}
				}
			}
		}
		return map;
	}

	private String getColumnName(Field field, String objName) {
		String columnName = field.getName();
		if (reflection.isAnnotationPresent(field, Column.class, objName)) {
			Column column = (Column) reflection.getAnnotation(field, Column.class, objName);
			if (column.name() != null && !"".equals(column.name()))
				columnName = column.name();
		} else if (reflection.isAnnotationPresent(field, JoinColumn.class, objName)) {
			JoinColumn joinColumn = (JoinColumn) reflection.getAnnotation(field, JoinColumn.class, objName);
			if (joinColumn.name() != null && !"".equals(joinColumn.name()))
				columnName = joinColumn.name();
		} else if (reflection.isAnnotationPresent(field, ManyToOne.class, objName)
				|| reflection.isAnnotationPresent(field, OneToOne.class, objName)) {
			columnName = columnName + "_id"; // hibernate auto ddl
		}
		// TODO Joincolumns, inverse join columns
		return columnName;
	}

	private String getColumnNameMethod(Method method, String objName) {
		String columnName = method.getName().substring(3);
		if (method.isAnnotationPresent(Column.class)) {
			Annotation annotation = method.getAnnotation(Column.class);
			if (annotation instanceof Column) {
				Column column = (Column) annotation;
				if (column.name() != null && !"".equals(column.name()))
					columnName = column.name();
			}

		} else if (method.isAnnotationPresent(JoinColumn.class)) {
			Annotation annotation = method.getAnnotation(JoinColumn.class);
			if (annotation instanceof JoinColumn) {
				JoinColumn joinColumn = (JoinColumn) annotation;
				if (joinColumn.name() != null && !"".equals(joinColumn.name()))
					columnName = joinColumn.name();
			}
		} else if (method.isAnnotationPresent(ManyToOne.class) || method.isAnnotationPresent(OneToOne.class)) {
			columnName = columnName + "_id"; // hibernate auto ddl
		}
		// TODO Joincolumns, inverse join columns
		return columnName;
	}

	private Integer getLength(Field field, String objName) {
		if (reflection.isAnnotationPresent(field, Column.class, objName)) {
			Column column = (Column) reflection.getAnnotation(field, Column.class, objName);
			if (column != null) {
				return column.length();
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	private Integer getLength(Method method, String objName) {
		if (method.isAnnotationPresent(Column.class)) {
			Annotation annotation = method.getAnnotation(Column.class);
			if (annotation instanceof Column) {
				Column column = (Column) annotation;
				return column.length();
			}
		} else {
			return null;
		}
		return null;
	}

	private boolean isUnique(Method method, String objName) {
		if (method.isAnnotationPresent(Column.class)) {
			Annotation annotation = method.getAnnotation(Column.class);
			if (annotation instanceof Column) {
				Column column = (Column) annotation;
				return column.unique();
			}
		} else {
			return false;
		}
		return false;
	}

}