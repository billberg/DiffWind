package com.diffwind.expr.operator;

import com.ql.util.express.Operator;

public class GTOperator extends Operator{
	
	public Object executeInner(Object[] list) throws Exception {
		boolean aIsArray = false, bIsArray = false;
		Object[] a = null, b = null;
		if (list[0].getClass().isArray()) {
			aIsArray = true;
			a = (Object[])list[0];
		} else {
			a = new Object[]{list[0]};
		}
		
		if (list[1].getClass().isArray()) {
			bIsArray = true;
			b = (Object[])list[1];
		} else {
			b = new Object[]{list[1]};
		}
		
		if (!aIsArray && !bIsArray) {
			boolean result = false;
			if (((Number)a[0]).doubleValue() > ((Number)b[0]).doubleValue()) {
				result = true;
			} 
			return result;
		}
		
		if (aIsArray && bIsArray) {
		    if (a.length != b.length)
		    	throw new Exception("操作数长度不一致");
		
			Boolean[] result = new Boolean[a.length];
			for (int i = 0; i < a.length; i++) {
				if (((Number)a[i]).doubleValue() > ((Number)b[i]).doubleValue()) {
					result[i] = true;
				} else {
					result[i] = false;
				}
			}
			return result;
		}
		
		if (aIsArray) {
			Boolean[] result = new Boolean[a.length];
			for (int i = 0; i < a.length; i++) {
				if (((Number)a[i]).doubleValue() > ((Number)b[0]).doubleValue()) {
					result[i] = true;
				} else {
					result[i] = false;
				}
			}
			return result;
		}
		
		if (bIsArray) {
			Boolean[] result = new Boolean[b.length];
			for (int i = 0; i < b.length; i++) {
				if (((Number)a[0]).doubleValue() > ((Number)b[i]).doubleValue()) {
					result[i] = true;
				}  else {
					result[i] = false;
				}
			}
			return result;
		}
		
		return null;
	}

}
