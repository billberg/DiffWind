package com.diffwind.stats;

import org.junit.Test;
import org.rosuda.JRI.Rengine;

public class JRIChartTest {

	@Test
	public void testStats_D() {
		
		String symbol = "SZ00528";
		

		Rengine re = new Rengine(new String[] { "--vanilla" }, false, null);
        System.out.println("Rengine created, waiting for R");

        // the engine creates R is a new thread, so we should wait until it's
        // ready
        if (!re.waitForR()) {
            System.out.println("Cannot load R");
            return;
        }

	}
}
