package com.cj.scmconduit.core.p4;

import static org.junit.Assert.*;

import org.junit.Test;

public class P4ClientSpecServerPathTest {
    
    @Test
    public void happyPath() {
        String line = "//myDepot/some/path";
        P4ClientSpecServerPath result = P4ClientSpecServerPath.parse(line);
        assertEquals("myDepot", result.depotName);
        assertEquals("/some/path", result.path);
    }
    @Test
    public void supportsEntireDepos() {
        String line = "//pd";
        P4ClientSpecServerPath result = P4ClientSpecServerPath.parse(line);
        assertEquals("pd", result.depotName);
        assertEquals("", result.path);
    }

}
