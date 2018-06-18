package com.diffwind.util;

import java.util.Arrays;

import org.junit.Test;

public class StringUtilTest {

	@Test
	public void splitTest() {
		//String record = "600201,生物股份,2008-04-08,,,,2008-04-08,2008-03-31";
		String record = "600104,上汽集团,2016-10-29,,,,2016-10-29,2016-09-30";
		String[] info = record.split(",");
		
		System.out.println(Arrays.toString(info));
	}
	
	@Test
	public void replaceTest() {
		String s1 = "三、营业利润";
		String s2 = "(二)稀释每股收益";
		
		String ret = s1.replaceAll("一、|二、|三、|四、|五、|六、|七、|八、|九、|十、|(一)|(二)|(三)|(四)|(五)|(六)|(七)|(八)|(九)|(十)", "");

		System.out.println(ret);
		
		ret = s2.replaceAll("一、|二、|三、|四、|五、|六、|七、|八、|九、|十、|\\(一\\)|\\(二\\)|\\(三\\)|\\(四\\)|\\(五\\)|\\(六\\)|\\(七\\)|\\(八\\)|\\(九\\)|\\(十\\)", "");

		System.out.println(ret);
	}
}
