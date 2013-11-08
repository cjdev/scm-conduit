package com.cj.scmconduit.core.p4;

import java.io.InputStream;
import java.util.List;

import com.cj.scmconduit.core.p4.P4SyncOutputParser.Change;

public interface P4 {

	List<P4Changelist> changesBetween(P4RevRangeSpec range);

	List<Change> syncTo(P4RevSpec rev);
    List<Change> syncTo(P4RevSpec rev, boolean force);

	String doCommand(String ... parts);

	String doCommand(InputStream in, String ... args);

}