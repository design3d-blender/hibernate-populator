package populator.persitence;

import java.sql.PreparedStatement;

public class InsertDeletStatement {

	private PreparedStatement insert;
	private PreparedStatement delete;

	public PreparedStatement getInsert() {
		return insert;
	}

	public void setInsert(PreparedStatement insert) {
		this.insert = insert;
	}

	public PreparedStatement getDelete() {
		return delete;
	}

	public void setDelete(PreparedStatement delete) {
		this.delete = delete;
	}

}
