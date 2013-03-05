package com.cj.scmconduit.server.api;

import com.cj.scmconduit.server.conduit.ConduitState;

public class ConduitInfoDto {
	public String name, p4path, readOnlyUrl, apiUrl, logUrl;
	public ConduitState status;
	public Integer queueLength;
	public Integer backlogSize;
	public String sshUrl;
	public Long currentP4Changelist;
	public String error;
	public ConduitType type;
}