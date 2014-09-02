package com.cj.scmconduit.server.session;

public interface SessionDisposalStrategy {
    void dispose(CodeSubmissionSession sessionToDisposeOf);
}