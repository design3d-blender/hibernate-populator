package populator.reflection;

import java.lang.reflect.Field;

public class PropertyDescription {

	private Object parent;
	private Field field;
	private Boolean isAutoIncrementId = false;
	private Integer index = 0;

	public Integer getIndex() {
		return index;
	}

	public void setIndex(Integer index) {
		this.index = index;
	}

	public Object getParent() {
		return parent;
	}

	public void setParent(Object parent) {
		this.parent = parent;
	}

	public Field getField() {
		return field;
	}

	public void setField(Field field) {
		this.field = field;
	}

	public Boolean getIsAutoIncrementId() {
		return isAutoIncrementId;
	}

	public void setIsAutoIncrementId(Boolean isAutoIncrementId) {
		this.isAutoIncrementId = isAutoIncrementId;
	}

}
