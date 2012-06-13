package com.cj.scmconduit.core.git;

public class GitFileStatus {
	public final StatusCode stagedStatus;
	public final StatusCode unstagedStatus;
	public final String file;
	
	public GitFileStatus(StatusCode stagedStatus, StatusCode unstagedStatus, String file) {
		super();
		this.stagedStatus = stagedStatus;
		this.unstagedStatus = unstagedStatus;
		this.file = file;
	}
	
	public boolean isUnknown(){
		return stagedStatus == null && unstagedStatus == null;
	}
}
