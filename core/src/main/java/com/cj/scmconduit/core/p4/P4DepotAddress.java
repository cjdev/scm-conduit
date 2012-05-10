package com.cj.scmconduit.core.p4;

public class P4DepotAddress {
	private final String value;
		
	public P4DepotAddress(String value) {
		super();
		if(value==null) throw new NullPointerException();
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
