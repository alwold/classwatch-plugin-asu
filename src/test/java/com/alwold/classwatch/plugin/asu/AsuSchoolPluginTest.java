package com.alwold.classwatch.plugin.asu;

import com.alwold.classwatch.school.RetrievalException;
import com.alwold.classwatch.school.SchoolPlugin;

/**
 *
 * @author alwold
 */
public class AsuSchoolPluginTest {
	public void testGetClassStatus() throws RetrievalException {
		SchoolPlugin plugin = new AsuSchoolPlugin();
		plugin.getClassStatus("2127", "12345");
	}
	
}
