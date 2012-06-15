package com.cj.scmconduit.core;

import com.cj.scmconduit.core.p4.P4Credentials;

public interface Conduit {
	boolean pull(String source, P4Credentials using) throws Exception;
	void push() throws Exception;
	void commit(P4Credentials using) throws Exception;
	void rollback() throws Exception ;
}
