package com.diffwind.expr.function;

import java.util.Arrays;

import com.ql.util.express.Operator;

public class MAFunction extends Operator{
	
	public Object executeInner(Object[] list) throws Exception {
		if (!list[0].getClass().isArray() || list.length != 2){
			throw new Exception("操作数异常, 正确的语法: MA(Double[] ts,int w)");
		}
		
		Double[] ts = (Double[])list[0];
		Integer w = (Integer)list[1];
		if (w > ts.length) {
			throw new Exception(String.format("操作数异常: w超出数组范围(ts.length=%s,w=%s): MA(Double[] ts,int w)",ts.length,w));
		}
		
		Double[] ma = Arrays.copyOf(ts, ts.length);
		Double sum = 0d;
		for (int i = 0; i < w; i++) sum += ts[i];
		
		ma[0] = sum/w;
		for (int i = 0; i < (ts.length - w); i++) {
			//数据缺失
			if (ts[i+w] == null || Double.isNaN((ts[i+w])) )
					break;
			
			sum = sum - ts[i] + ts[i+w];
			ma[i+1] = sum/w;
		}
		
		return ma;
		
	}

}
