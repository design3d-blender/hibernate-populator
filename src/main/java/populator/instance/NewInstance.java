package populator.instance;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked") 
public class NewInstance {

	@SuppressWarnings("rawtypes")
	public Object newInstance(Class<?> clazz) {
		try {
			if (clazz.isEnum())
				return Enum.valueOf((Class) clazz, clazz.getDeclaredFields()[0].getName());
			return clazz.newInstance();
		} catch (Exception e) {
			for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
				try {
					return constructor.newInstance(getConstructorParams(constructor));
				} catch (Exception e1) {
					// trying another constructor
				}
			}
		}
		throw new RuntimeException("Is " + clazz.getSimpleName()
				+ " an abstract class or interface? Try setup populator.when(" + clazz.getSimpleName()
				+ ").thenInstance(?)");
	}

	private Object[] getConstructorParams(Constructor<?> constructor) {
		List<Object> constructorParams = new ArrayList<Object>();
		for (Class<?> parameterClass : constructor.getParameterTypes())
			constructorParams.add(newInstance(parameterClass));
		return constructorParams.toArray();
	}

}
