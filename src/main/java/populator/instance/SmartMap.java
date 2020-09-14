package populator.instance;

import java.util.Iterator;
import java.util.LinkedHashMap;

@SuppressWarnings("serial") 
public class SmartMap<K, V> extends LinkedHashMap<SmartClass, Object> {

	private NewInstance newInstance = new NewInstance();

	@Override public Object get(Object key) {
		SmartClass smartClass = (SmartClass) key;
		Object obj = super.get(smartClass);
		if (obj == null) {
			obj = newInstance.newInstance(smartClass.getTrueType());
			put(smartClass, obj);
		} else if (smartClass.isCollection()) {
			obj = newInstance.newInstance(obj.getClass());
		}
		return obj;
	}

	public SmartClass getLastKey() {
		Iterator<SmartClass> it = this.keySet().iterator();
		SmartClass smartClass = null;
		while (it.hasNext())
			smartClass = it.next();
		return smartClass;
	}

}
