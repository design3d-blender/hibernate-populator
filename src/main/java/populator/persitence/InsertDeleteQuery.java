package populator.persitence;

import javax.persistence.Query;

public class InsertDeleteQuery {

	private Query insert;
	private Query delete;

	public Query getInsert() {
		return insert;
	}

	public void setInsert(Query insert) {
		this.insert = insert;
	}

	public Query getDelete() {
		return delete;
	}

	public void setDelete(Query delete) {
		this.delete = delete;
	}

}
