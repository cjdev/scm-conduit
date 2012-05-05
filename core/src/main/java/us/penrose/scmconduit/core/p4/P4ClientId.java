package us.penrose.scmconduit.core.p4;

public class P4ClientId {
	private final String value;
		
	public P4ClientId(String value) {
		super();
		if(value==null) throw new NullPointerException();
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
