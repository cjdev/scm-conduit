package com.cj.scmconduit.core.p4;

public class P4RevSpec {
	public static P4RevSpec forChangelist(long changelist){
		return new P4RevSpec("@" + changelist);
	}

	public static P4RevSpec head(){
		return new P4RevSpec("#head");
	}
	
	private final String value;
	
	private P4RevSpec(String value) {
		super();
		this.value = value;
	}


	public String toString(){
		return value;
	}
}
