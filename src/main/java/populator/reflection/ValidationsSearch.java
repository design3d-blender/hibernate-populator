package populator.reflection;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.google.common.base.CaseFormat;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

/**
 * Class in charge of finding and storing all bean validations that may appear
 * on the objects that will be persisted
 * 
 * @author Juan Mu;oz
 *
 */
public class ValidationsSearch {

	public SequenceDescription sequenceDescription = new SequenceDescription(
			new HashMap<Object, HashMap<String, String>>());
	private Reflection reflection = Reflection.get();
	private HashSet<String> avoidCyclicField = new HashSet<String>();
	private HashSet<String> avoidCyclicMethod = new HashSet<String>();
	private HashSet<String> performanceSet = new HashSet<String>();
	public SetMultimap<String, String> uniquesMap = HashMultimap.create();
	private HashSet<String> embeddableDescriptions = new HashSet<String>();

	/**
	 * @return the sequenceDescription
	 */
	public SequenceDescription getSequenceDescription() {
		return sequenceDescription;
	}

	/**
	 * @param sequenceDescription the sequenceDescription to set
	 */
	public void setSequenceDescription(SequenceDescription sequenceDescription) {
		this.sequenceDescription = sequenceDescription;
	}

	/**
	 * @return the uniquesMap
	 */
	public SetMultimap<String, String> getUniquesMap() {
		return uniquesMap;
	}

	/**
	 * @param uniquesMap the uniquesMap to set
	 */
	public void setUniquesMap(SetMultimap<String, String> uniquesMap) {
		this.uniquesMap = uniquesMap;
	}

	/**
	 * Begins the recursive search for the different validations and constraints
	 * contained within the objects that will be persisted.
	 * 
	 * @param rootEntity object from which the recursive search will begin
	 */
	public void start(Object rootEntity) {
		avoidCyclicField.clear();
		avoidCyclicMethod.clear();
		startReal(rootEntity, null);
	}

	/**
	 * Checks if an objects contains a given field by name.
	 * 
	 * @param object
	 * @param fieldName
	 * @return
	 */
	public boolean doesObjectContainField(Class<?> object, String fieldName) {
		return Arrays.stream(object.getDeclaredFields()).anyMatch(field -> field.getName().equals(fieldName));
	}

	/**
	 * Get the field name of a method if said method is a getter
	 * 
	 * @param method
	 * @return
	 */
	public Field getFieldFromMethod(Method method) {
		try {
			Class<?> clazz = method.getDeclaringClass();
			BeanInfo info = Introspector.getBeanInfo(clazz);
			PropertyDescriptor[] props = info.getPropertyDescriptors();
			String descriptorName;
			String columnName;
			if (method.isAnnotationPresent(Column.class) || method.isAnnotationPresent(JoinColumn.class)) {
				for (PropertyDescriptor pd : props) {
					if (method.equals(pd.getReadMethod())) {
						descriptorName = pd.getName();
						if (doesObjectContainField(clazz, descriptorName)) {
							return clazz.getDeclaredField(descriptorName);
						}
					}
				}
				if (!method.isAnnotationPresent(JoinColumn.class)) {
					Column column = (Column) method.getAnnotation(Column.class);
					columnName = column.name();
					columnName = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, columnName);
					if (doesObjectContainField(clazz, columnName)) {
						return clazz.getDeclaredField(columnName);
					}
				} else {
					JoinColumn column = (JoinColumn) method.getAnnotation(JoinColumn.class);
					columnName = column.name();
					columnName = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, columnName);
					if (doesObjectContainField(clazz, columnName)) {
						return clazz.getDeclaredField(columnName);
					}
				}

			}
		} catch (IntrospectionException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

//	public HashSet<EmbeddableDescription> getEmbeddableDescription(Class<?> embeddedObject, Object rootObject) {
//		HashSet<EmbeddableDescription> embeddableDescriptions = new HashSet<EmbeddableDescription>();
//		EmbeddableDescription embeddableDescription = new EmbeddableDescription();
//		embeddableDescription.setParentName(rootObject.getClass().getSimpleName());
//		embeddableDescription.setObjectName(embeddedObject.getSimpleName());
//		Arrays.stream(embeddedObject.getDeclaredFields()).forEach(field -> {
//			if (field.isAnnotationPresent(Column.class)) {
//				Column column = (Column) field.getAnnotation(Column.class);
//				if (column.name() != null && column.name() != "") {
//					Arrays.stream(rootObject.getClass().getDeclaredFields()).forEach(rootField -> {
//						if (rootField.isAnnotationPresent(Column.class)) {
//							Column rootColumn = (Column) field.getAnnotation(Column.class);
//							if (rootColumn.name() != null && rootColumn.name() != ""
//									&& rootColumn.name().equals(column.name())) {
//								embeddableDescription.setParentFieldName(rootField.getName());
//								embeddableDescription.setFieldName(field.getName());
//							}
//						}
//					});
//					Arrays.stream(rootObject.getClass().getMethods()).forEach(rootMethod -> {
//						if (rootMethod.isAnnotationPresent(Column.class)) {
//							Column rootColumn = (Column) field.getAnnotation(Column.class);
//							if (rootColumn.name() != null && rootColumn.name() != ""
//									&& rootColumn.name().equals(column.name())) {
//								embeddableDescription.setParentFieldName(getFieldFromMethod(rootMethod).getName());
//								embeddableDescription.setFieldName(field.getName());
//							}
//						}
//					});
//				}
//			}
//			if (embeddableDescription.getFieldName() != null && embeddableDescription.getParentFieldName() != null
//					&& embeddableDescription.getParentName() != null && embeddableDescription.getObjectName() != null) {
//				embeddableDescriptions.add(embeddableDescription);
//			}
//		});
//		Arrays.stream(embeddedObject.getMethods()).forEach(method -> {
//			if (method.isAnnotationPresent(Column.class)) {
//				Column column = (Column) method.getAnnotation(Column.class);
//				if (column.name() != null && column.name() != "") {
//					String fieldName = getFieldFromMethod(method).getName();
//					Arrays.stream(rootObject.getClass().getDeclaredFields()).forEach(rootField -> {
//						if (rootField.isAnnotationPresent(Column.class)) {
//							Column rootColumn = (Column) method.getAnnotation(Column.class);
//							if (rootColumn.name() != null && rootColumn.name() != ""
//									&& rootColumn.name().equals(column.name())) {
//								embeddableDescription.setParentFieldName(rootField.getName());
//								embeddableDescription.setFieldName(fieldName);
//							}
//						}
//					});
//					Arrays.stream(rootObject.getClass().getMethods()).forEach(rootMethod -> {
//						if (rootMethod.isAnnotationPresent(Column.class)) {
//							Column rootColumn = (Column) method.getAnnotation(Column.class);
//							if (rootColumn.name() != null && rootColumn.name() != ""
//									&& rootColumn.name().equals(column.name())) {
//								embeddableDescription.setParentFieldName(getFieldFromMethod(rootMethod).getName());
//								embeddableDescription.setFieldName(fieldName);
//							}
//						}
//					});
//				}
//			}
//			if (embeddableDescription.getFieldName() != null && embeddableDescription.getParentFieldName() != null
//					&& embeddableDescription.getParentName() != null && embeddableDescription.getObjectName() != null) {
//				embeddableDescriptions.add(embeddableDescription);
//			}
//		});
//		return embeddableDescriptions;
//	}

	/**
	 * Real recursive search.
	 * 
	 * @param rootEntity
	 * @param index
	 */
	private void startReal(Object rootEntity, Integer index) {
		if (rootEntity != null) {
			if (!performanceSet.contains(rootEntity.getClass().getSimpleName())) {
				performanceSet.add(rootEntity.getClass().getSimpleName());
				if (reflection.isBeanType(rootEntity.getClass())
						&& !Collection.class.isAssignableFrom(rootEntity.getClass())) {
					Table table = rootEntity.getClass().getAnnotation(Table.class);
					HashSet<String> constrainedColumns = new HashSet<String>();
					if (table != null) {
						if (table.name().startsWith("V_")) {
							return;
						}
						UniqueConstraint[] ucs = table.uniqueConstraints();
						if (ucs != null) {
							for (UniqueConstraint uniqueConstraint : ucs) {
								for (String columnName : uniqueConstraint.columnNames()) {
									constrainedColumns.add(columnName);
								}
							}
						}
					}
					for (Method method : rootEntity.getClass().getDeclaredMethods()) {
						if (method.isAnnotationPresent(Column.class) || method.isAnnotationPresent(JoinColumn.class)
								|| method.isAnnotationPresent(OneToMany.class)
								|| method.isAnnotationPresent(ManyToOne.class)) {
							Field field = getFieldFromMethod(method);
							if (field == null) {
								continue;
							}
							if (Modifier.isStatic(field.getModifiers()))
								continue;
							String indexString = "0";
							if (index != null)
								indexString = index.toString();
							if (!avoidCyclicMethod.contains(rootEntity.getClass() + method.toString() + indexString)) {
								avoidCyclicMethod.add(rootEntity.getClass() + method.toString() + indexString);
								field.setAccessible(true);
								if (method.isAnnotationPresent(GeneratedValue.class)) {
									Annotation annotation = method.getAnnotation(GeneratedValue.class);
									if (annotation instanceof GeneratedValue) {
										GeneratedValue genVal = (GeneratedValue) annotation;
										if (genVal.strategy() == GenerationType.AUTO) {
											sequenceDescription.addSequence(field.toString() + indexString, rootEntity,
													"hibernate_sequence");
										} else if (genVal.strategy() == GenerationType.SEQUENCE) {
											SequenceGenerator seq = null;
											if (method.isAnnotationPresent(SequenceGenerator.class)) {
												annotation = method.getAnnotation(SequenceGenerator.class);
												if (annotation instanceof SequenceGenerator) {
													seq = (SequenceGenerator) annotation;
													sequenceDescription.addSequence(field.toString() + indexString,
															rootEntity, seq.sequenceName());
												}
											} else if (rootEntity.getClass()
													.isAnnotationPresent(SequenceGenerator.class)) {
												annotation = rootEntity.getClass()
														.getAnnotation(SequenceGenerator.class);
												if (annotation instanceof SequenceGenerator) {
													seq = (SequenceGenerator) annotation;
													sequenceDescription.addSequence(field.toString() + indexString,
															rootEntity, seq.sequenceName());
												}
											}
										}
									}
								} else if (method.isAnnotationPresent(Column.class)) {
									Column column = (Column) method.getAnnotation(Column.class);
									if (column.unique() || constrainedColumns.contains(column.name())) {
										uniquesMap.put(rootEntity.getClass().getSimpleName(), field.getName());
									}
								} else if (method.isAnnotationPresent(EmbeddedId.class)) {
//									embeddableDescriptions
//											.addAll(getEmbeddableDescription(field.getType(), rootEntity));
									embeddableDescriptions.add(field.getType().getSimpleName());
								}
							}
						}

					}
					for (Field field : reflection.getAllDeclaredFields(rootEntity.getClass())) {
						if (Modifier.isStatic(field.getModifiers()))
							continue;
						String indexString = "0";
						if (index != null)
							indexString = index.toString();
						if (!avoidCyclicField.contains(rootEntity.getClass() + field.toString() + indexString)) {
							avoidCyclicField.add(rootEntity.getClass() + field.toString() + indexString);
							field.setAccessible(true);
							if (field.isAnnotationPresent(GeneratedValue.class)) {
								Annotation annotation = field.getAnnotation(GeneratedValue.class);
								if (annotation instanceof GeneratedValue) {
									GeneratedValue genVal = (GeneratedValue) annotation;
									if (genVal.strategy() == GenerationType.AUTO) {
										sequenceDescription.addSequence(field.toString() + indexString, rootEntity,
												"hibernate_sequence");
									} else if (genVal.strategy() == GenerationType.SEQUENCE) {
										SequenceGenerator seq = null;
										if (field.isAnnotationPresent(SequenceGenerator.class)) {
											annotation = field.getAnnotation(SequenceGenerator.class);
											if (annotation instanceof SequenceGenerator) {
												seq = (SequenceGenerator) annotation;
												sequenceDescription.addSequence(field.toString() + indexString,
														rootEntity, seq.sequenceName());
											}
										} else if (rootEntity.getClass().isAnnotationPresent(SequenceGenerator.class)) {
											annotation = rootEntity.getClass().getAnnotation(SequenceGenerator.class);
											if (annotation instanceof SequenceGenerator) {
												seq = (SequenceGenerator) annotation;
												sequenceDescription.addSequence(field.toString() + indexString,
														rootEntity, seq.sequenceName());
											}
										}
									}
								}
							} else if (field.isAnnotationPresent(Column.class)) {
								Column column = (Column) field.getAnnotation(Column.class);
								if (column.unique() || constrainedColumns.contains(column.name())) {
									uniquesMap.put(rootEntity.getClass().getSimpleName(), field.getName());
								}
							} else if (field.isAnnotationPresent(EmbeddedId.class)) {
//								embeddableDescriptions.addAll(getEmbeddableDescription(field.getType(), rootEntity));
								embeddableDescriptions.add(field.getType().getSimpleName());
							}
							Object nextObject = reflection.getProperty(rootEntity, field);
							if (nextObject == null) {
								if (Collection.class.isAssignableFrom(field.getType())) {
									ParameterizedType fieldTypepe = (ParameterizedType) field.getGenericType();
									Class<?> clazz = (Class<?>) fieldTypepe.getActualTypeArguments()[0];
									if (clazz.getName().equals("java.io.Serializable")) {
										continue;
									}
									try {
										nextObject = clazz.newInstance();
									} catch (InstantiationException | IllegalAccessException e) {
//										e.printStackTrace();
									}
								} else {
									String className = field.getType().getName();
									if (className.equals("java.io.Serializable")) {
										continue;
									}
									Class<?> clazz;
									try {
										clazz = Class.forName(className);
										nextObject = clazz.newInstance();

									} catch (ClassNotFoundException | InstantiationException
											| IllegalAccessException e) {
//										e.printStackTrace();
									}
								}
							} else if (nextObject instanceof List) {
								ParameterizedType fieldTypepe = (ParameterizedType) field.getGenericType();
								Class<?> clazz = (Class<?>) fieldTypepe.getActualTypeArguments()[0];
								if (clazz.getName().equals("java.io.Serializable")) {
									continue;
								}
								try {
									nextObject = clazz.newInstance();
								} catch (InstantiationException | IllegalAccessException e) {
//									e.printStackTrace();
								}
							} else if (nextObject.getClass().isAnnotationPresent(Embeddable.class)) {
								continue;
							}

							startReal(nextObject, null);
						}
					}
				} else {
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

	/**
	 * @return the embeddableDescriptions
	 */
	public HashSet<String> getEmbeddableDescriptions() {
		return embeddableDescriptions;
	}

	/**
	 * @param embeddableDescriptions the embeddableDescriptions to set
	 */
	public void setEmbeddableDescriptions(HashSet<String> embeddableDescriptions) {
		this.embeddableDescriptions = embeddableDescriptions;
	}
}
