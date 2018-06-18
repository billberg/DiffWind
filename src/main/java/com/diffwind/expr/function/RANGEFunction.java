package com.diffwind.expr.function;

import java.util.Arrays;

import com.ql.util.express.Operator;

public class RANGEFunction extends Operator{
	
	public Object executeInner(Object[] list) throws Exception {
		if (!list[0].getClass().isArray() || (list.length != 2 && list.length != 3)){
			throw new Exception("操作数异常, 正确的语法: RANGE(Object[] arr,int length) 或  RANGE(Object[] arr, int startIndex, int length)");
		}
		
		Object[] arr = (Object[])list[0];
		if (list.length == 2) {
			Integer length = (Integer)list[1];
			if (length > arr.length) {
				throw new Exception(String.format("操作数异常: length超出数组范围(arr.length=%s,length=%s): RANGE(Object[] arr,int length)",arr.length,length));
			}
			return Arrays.copyOfRange(arr, 0, length);
		}
		
		//startIndex从1开始
		if (list.length == 3) {
			Integer startIndex = (Integer)list[1] - 1;
			Integer length = (Integer)list[2];
			if (startIndex + length > arr.length) {
				throw new Exception(String.format("操作数异常: length超出数组范围(arr.length=%s,startIndex=%s,length=%s): RANGE(Object[] arr, int startIndex, int length)",arr.length,startIndex,length));
			}
			return Arrays.copyOfRange(arr, startIndex, startIndex+length);
		}
		
		throw new Exception("操作数异常, 正确的语法: RANGE(Object[] arr,int length) 或  RANGE(Object[] arr, int startIndex, int length)");
	}

}
