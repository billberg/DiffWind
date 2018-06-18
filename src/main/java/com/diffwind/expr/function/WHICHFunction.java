package com.diffwind.expr.function;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.ql.util.express.Operator;

public class WHICHFunction extends Operator{
	
	public Object executeInner(Object[] list) throws Exception {
		if (list.length == 0){
			throw new Exception("操作数异常");
		}
		
		List<Object> newlist = new ArrayList<Object>();
		for (int i = 0; i < list.length; i++) {
			if (list[i].getClass().isArray()) {
				newlist.addAll(Arrays.asList((Object[])list[i]));
			} else {
				newlist.add(list[i]);
			}
		}

		int index = 0;
		Iterator<Object> iter = newlist.iterator();
		while (iter.hasNext()) {
			index++;
			Object i = iter.next();
			if ((Boolean)i ) {
				break;
			} 
		}
		return index;
	}

}
