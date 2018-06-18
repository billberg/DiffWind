package com.diffwind.util;

import java.text.ParseException;
import java.util.GregorianCalendar;

import org.junit.Test;

import com.diffwind.util.DateUtil;

public class GregorianCalendarTest {
	
	@Test
	public void test() throws ParseException {
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(DateUtil.yyyyMMdd.get().parse("20170101"));
		gc.add(GregorianCalendar.DAY_OF_MONTH, -1);
		
		System.out.println(gc.getTime());
	}

}
