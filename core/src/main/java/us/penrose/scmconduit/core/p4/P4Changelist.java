package us.penrose.scmconduit.core.p4;

public class P4Changelist {
	final long id;
	final String description;
	final String whoString;
	final P4Time when;
	
	public P4Changelist(long id, String description, String whoString, P4Time when) {
		super();
		this.id = id;
		this.description = description;
		this.whoString = whoString;
		this.when = when;
	}

	public long id() {
		return id;
	}
	
	public P4Time getWhen() {
		return when;
	}
	
	public String description() {
		return description;
	}
	
	public String whoString() {
		return whoString;
	}
	
	@Override
	public String toString() {
		return "changelist #" + id + " by " + whoString + ": " + description;
	}
}