package com.diffwind.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtil {

	// SimpleDateFormat是线程不安全的，采用这种方式避免
	public static ThreadLocal<DateFormat> yyyyMMdd = new ThreadLocal<DateFormat>() {
		@Override
		protected synchronized DateFormat initialValue() {
			return new SimpleDateFormat("yyyyMMdd");
		}
	};

	public static ThreadLocal<DateFormat> yyyyMMddLine = new ThreadLocal<DateFormat>() {
		@Override
		protected synchronized DateFormat initialValue() {
			return new SimpleDateFormat("yyyy/MM/dd");
		}
	};

	public static ThreadLocal<DateFormat> YYMMDD = new ThreadLocal<DateFormat>() {
		@Override
		protected synchronized DateFormat initialValue() {
			return new SimpleDateFormat("yyMMdd");
		}
	};

	public static ThreadLocal<DateFormat> HHmmss = new ThreadLocal<DateFormat>() {
		@Override
		protected synchronized DateFormat initialValue() {
			return new SimpleDateFormat("HHmmss");
		}
	};

	public static ThreadLocal<DateFormat> yyyyMMdd10 = new ThreadLocal<DateFormat>() {
		@Override
		protected synchronized DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd");
		}
	};

	public static ThreadLocal<DateFormat> formatter14 = new ThreadLocal<DateFormat>() {
		@Override
		protected synchronized DateFormat initialValue() {
			return new SimpleDateFormat("yyyyMMddHHmmss");
		}
	};

	public static ThreadLocal<DateFormat> formatter18 = new ThreadLocal<DateFormat>() {
		@Override
		protected synchronized DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		}
	};
	
	public static ThreadLocal<DateFormat> yyyyMM = new ThreadLocal<DateFormat>() {
		@Override
		protected synchronized DateFormat initialValue() {
			return new SimpleDateFormat("yyyyMM");
		}
	};
	
	public static ThreadLocal<DateFormat> yyyy = new ThreadLocal<DateFormat>() {
		@Override
		protected synchronized DateFormat initialValue() {
			return new SimpleDateFormat("yyyy");
		}
	};
	
	public static ThreadLocal<DateFormat> MMdd = new ThreadLocal<DateFormat>() {
		@Override
		protected synchronized DateFormat initialValue() {
			return new SimpleDateFormat("MMdd");
		}
	};
	
	public static Date getLastMonth() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -1);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		return cal.getTime();
	}
	
	public static int getDaysBetween(Date startDate, Date endDate) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(startDate);
		
		Calendar cal2 = Calendar.getInstance();
		cal2.setTime(endDate);

		return (int)((cal2.getTimeInMillis() - cal.getTimeInMillis())/(1000*3600*24));
	}
	
	public static int getHoursBetween(Date startDate, Date endDate) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(startDate);
		
		Calendar cal2 = Calendar.getInstance();
		cal2.setTime(endDate);

		return (int)((cal2.getTimeInMillis() - cal.getTimeInMillis())/(1000*3600));
	}
	
	//根据日期找到上一个最近的财报日
		public static Date getLastFinanceReportDate(Date date) {
			Calendar cal = Calendar.getInstance();
			
			cal.setTime(date);
			int year = cal.get(Calendar.YEAR);
			int month = cal.get(Calendar.MONTH) + 1;
			
			int financeReportYear, financeReportMonth;
			if (month <= 3) {
				financeReportYear = year - 1;
				financeReportMonth = 12;
			} else {
				financeReportYear = year;
				financeReportMonth = 3*((month-1)/3);
			}
		
			cal.set(financeReportYear, financeReportMonth - 1, 1);
		    cal.set(Calendar.DATE, cal.getActualMaximum(Calendar.DATE));
			
			return cal.getTime();
		}
		
		
		public static Date getLastDayOfMonth(Date date) {
			Calendar cal = Calendar.getInstance();
			
			cal.setTime(date);
			//int year = cal.get(Calendar.YEAR);
			//int month = cal.get(Calendar.MONTH);
			//cal.set(year, month, 1);
		    cal.set(Calendar.DATE, cal.getActualMaximum(Calendar.DATE));
			
			return cal.getTime();
		}
		
		public static Date getLastDayOfMonth(int year, int month) {
			Calendar cal = Calendar.getInstance();
			cal.set(year, month-1, 1);
		    cal.set(Calendar.DATE, cal.getActualMaximum(Calendar.DATE));
			
			return cal.getTime();
		}
}
