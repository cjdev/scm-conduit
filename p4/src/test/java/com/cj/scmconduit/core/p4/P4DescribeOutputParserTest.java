package com.cj.scmconduit.core.p4;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.cj.scmconduit.core.p4.P4DescribeOutputParser.Change;
import com.cj.scmconduit.core.p4.P4DescribeOutputParser.ChangeType;

public class P4DescribeOutputParserTest {

    @Test
    public void two() {
        // given
        Reader input =  new StringReader(
                            "Change 5 by larry@larrys-client on 2013/03/03 10:01:58\n" + 
                            "\n" + 
                            "    Edited b.txt\n" + 
                            "\n" + 
                            "Affected files ...\n" + 
                            "\n" + 
                            "... //depot/a.txt#3 edit\n" + 
                            "... //depot/b.txt#2 edit\n" +
                            "\n" + 
                            "Differences ...\n" +
                            "==== //depot/a.txt#2 (text) ====\n" +
                            "\n" +
                            "1c1\n" +
                            "< hello world---\n" +
                            "> yo yo yo\n" +
                            "");
        
        P4DescribeOutputParser testSubject = new P4DescribeOutputParser();
        
        // when
        List<Change> result = testSubject.parse(input);
        Assert.assertEquals(2, result.size());
        {
            Change change = result.get(0);
            Assert.assertEquals("//depot/a.txt", change.depotPath);
            Assert.assertEquals(3, change.fileVersion.intValue());
            Assert.assertEquals(ChangeType.EDIT, change.type);
        }
        {
            Change change = result.get(1);
            Assert.assertEquals("//depot/b.txt", change.depotPath);
            Assert.assertEquals(2, change.fileVersion.intValue());
            Assert.assertEquals(ChangeType.EDIT, change.type);
        }
    }
/*
 * 


Change 4 by larry@larrys-client on 2013/03/03 10:16:59

    Edited a.txt

Affected files ...

... //depot/a.txt#2 edit

Differences ...

==== //depot/a.txt#2 (text) ====

@@ -1,1 +1,1 @@
-hello world
\ No newline at end of file
+yo yo yo
\ No newline at end of file


 * 
 */
}
