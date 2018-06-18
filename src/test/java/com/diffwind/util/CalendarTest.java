package com.diffwind.util;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import org.junit.Test;

import com.diffwind.util.DateUtil;

public class CalendarTest {
	
	@Test
	public void testMonth() throws ParseException {

		Date date = DateUtil.yyyyMMdd10.get().parse("2017-12-31");
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		
		cal.add(Calendar.MONTH, 4);
		
		System.out.println(DateUtil.yyyyMMdd10.get().format(cal.getTime()) );
		
		for (int i = 1; i < 20; i++) {
			cal.add(Calendar.MONTH, 1);
			
			System.out.println(DateUtil.yyyyMMdd10.get().format(cal.getTime()) );
		}
		
		
		
	}

}
