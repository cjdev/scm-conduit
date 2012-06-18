package com.cj.scmconduit.core.p4;

public class P4RevRangeSpec {
	
	public static P4RevRangeSpec between(P4RevSpec a, P4RevSpec b){
		return new P4RevRangeSpec(a + "," + b);
	}
	
	public static P4RevRangeSpec everythingAfter(long changelist){
		return new P4RevRangeSpec("@>" + changelist);
	}
	
	private final String value;
	
	private P4RevRangeSpec(String value) {
		super();
		this.value = value;
	}


	public String toString(){
		return value;
	}
}
