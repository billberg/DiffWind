package com.diffwind.expr.operator;

import com.ql.util.express.Operator;

public class AnyLTOperator extends Operator{
	
	public Object executeInner(Object[] list) throws Exception {
		Object[] a = (Object[])list[0];
		Object b = (Object)list[1];
		for (int i = 0; i < a.length; i++) {
			if (((Number)a[i]).doubleValue() < ((Number)b).doubleValue()) {
				return true;
			}
		}
		
		return false;
	}

}
