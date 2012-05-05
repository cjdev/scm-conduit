package us.penrose.scmconduit.core.p4;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.List;

import org.junit.Test;

import us.penrose.scmconduit.core.p4.P4SyncOutputParser.Change;
import us.penrose.scmconduit.core.p4.P4SyncOutputParser.ChangeType;

public class P4SyncOutputParserTest {

	@Test
	public void test() {
		
		String eol = "\n";
		String text = 
				"//cj/cjo/main/database-dw/src/projects/active/ddl/PR_USES_DW/create_dw_partition_runner_config.sql#4 - updating /home/joe/projects/temp-rename/cjo/database-dw/src/projects/active/ddl/PR_USES_DW/create_dw_partition_runner_config.sql" + eol + 
				"//cj/cjo/main/performancereporting/src/main/java/cj/performancereporting/dao/WeekOverWeekPerformanceReportQuery.java#1 - added as /home/joe/projects/temp-rename/cjo/performancereporting/src/main/java/cj/performancereporting/dao/WeekOverWeekPerformanceReportQuery.java" + eol + 
				"//cj/cjo/main/database-dw/src/cjodw/stored_code/util/maintainCClicksPartitions.sql#33 - deleted as /home/joe/projects/temp-rename/cjo/database-dw/src/cjodw/stored_code/util/maintainCClicksPartitions.sql" + eol;
		
		P4SyncOutputParser p = new P4SyncOutputParser();
		
		List<Change> changes = p.parse(new StringReader(text));
		
		assertEquals(3, changes.size());
		
		Change[] expectations = {
				new Change(
						ChangeType.UPDATE, 
						"//cj/cjo/main/database-dw/src/projects/active/ddl/PR_USES_DW/create_dw_partition_runner_config.sql", 
						"/home/joe/projects/temp-rename/cjo/database-dw/src/projects/active/ddl/PR_USES_DW/create_dw_partition_runner_config.sql", 
						4
				),
				new Change(
						ChangeType.ADD, 
						"//cj/cjo/main/performancereporting/src/main/java/cj/performancereporting/dao/WeekOverWeekPerformanceReportQuery.java", 
						"/home/joe/projects/temp-rename/cjo/performancereporting/src/main/java/cj/performancereporting/dao/WeekOverWeekPerformanceReportQuery.java", 
						1
				),
				new Change(
						ChangeType.DELETE, 
						"//cj/cjo/main/database-dw/src/cjodw/stored_code/util/maintainCClicksPartitions.sql", 
						"/home/joe/projects/temp-rename/cjo/database-dw/src/cjodw/stored_code/util/maintainCClicksPartitions.sql", 
						33
				),
				
		};
		
		for(int x=0;x<expectations.length;x++){
			assertChangeEquals(expectations[x], changes.get(x));
		}
		
	}

	private void assertChangeEquals(Change expected, Change change) {
		assertEquals(expected.type, change.type);
		assertEquals(expected.depotPath, change.depotPath);
		assertEquals(expected.fileVersion, change.fileVersion);
		assertEquals(expected.workspacePath, change.workspacePath);
	}

}
