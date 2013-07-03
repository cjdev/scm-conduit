package com.cj.scmconduit.server.data;

public interface Database {
	KeyValueStore passwordsByUsername();
	KeyValueStore trustedKeysByUsername();
}
