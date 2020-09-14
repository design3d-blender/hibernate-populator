package populator.reflection;

import java.util.HashMap;

public class SequenceDescription {

	private HashMap<Object, HashMap<String, String>> sequences;
	

	public SequenceDescription(HashMap<Object, HashMap<String, String>> sequences) {
		super();
		this.sequences = sequences;
	}

	public void addSequence(String fieldPlusIndex, Object parentObject, String sequenceName) {
		if (!this.sequences.containsKey(parentObject)) {
			HashMap<String, String> temp = new HashMap<String, String>();
			temp.put(fieldPlusIndex, sequenceName);
			this.sequences.put(parentObject, temp);
		} else {
			HashMap<String, String> children = this.sequences.get(parentObject);
			if (!children.containsKey(fieldPlusIndex)) {
				children.put(fieldPlusIndex, sequenceName);
			}
		}
	}

	public HashMap<Object, HashMap<String, String>> getSequences() {
		return this.sequences;
	}

}
