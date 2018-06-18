package qlexpress.extend;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.diffwind.expr.ExpressRunnerFactory;
import com.diffwind.expr.function.MAFunction;
import com.diffwind.expr.function.MINFunction;
import com.diffwind.expr.function.RANGEFunction;
import com.diffwind.expr.function.SUMFunction;
import com.diffwind.expr.function.WHICHFunction;
import com.diffwind.expr.operator.EachLTEOperator;
import com.diffwind.expr.operator.EachLTOperator;
import com.diffwind.expr.operator.GTEOperator;
import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressRunner;
import com.ql.util.express.IExpressContext;

/**
 * 本例用于展示如何自定义操作符和方法
 *
 */
public class OperatorTest {

	@Test
	public void testEachLT() throws Exception{
		//定义表达式，相当于 1+(22+22)+(2+2)
		String exp = " [17,13,20,18,19,30.5,21,19,18,19,21,19] eachLT 22";
		ExpressRunner runner = new ExpressRunner();
		//定义操作符addT，其实现为AddTwiceOperator
		runner.addOperator("eachLT", new EachLTOperator());
		//执行表达式，并将结果赋给r
		
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < 100000; i++) {
			Boolean r = (Boolean)runner.execute(exp,null,null,false,false);
			//System.out.println(r);
			//Assert.assertTrue("操作符执行错误",r);
		}
		
		long endTime = System.currentTimeMillis();
		
		System.out.println("ms: " + (endTime-startTime));
	}
	
	@Test
	public void testEachLTE() throws Exception{
		//定义表达式，相当于 1+(22+22)+(2+2)
		//String exp = " [17,13,20,18,19,30] eachLT 30";
		String exp = " [17,13,20,18,19,30] eachLTE 30";
		ExpressRunner runner = new ExpressRunner();
		//定义操作符addT，其实现为AddTwiceOperator
		runner.addOperator("eachLT", new EachLTOperator());
		runner.addOperator("eachLTE", new EachLTEOperator());
		//执行表达式，并将结果赋给r
		
		long startTime = System.currentTimeMillis();
		Boolean r = (Boolean)runner.execute(exp,null,null,false,false);
		System.out.println(r);
		Assert.assertTrue("操作符执行错误",r);
		
		long endTime = System.currentTimeMillis();
		
		System.out.println("ms: " + (endTime-startTime));
	}
	
	
	@Test
	public void testGTE() throws Exception{
		//定义表达式，相当于 1+(22+22)+(2+2)
		//String exp = " [17,13,20,18,19,30] eachLT 30";
		String exp = "[17,13,20,18,19,30] GTE 30";
		ExpressRunner runner = new ExpressRunner();
		//定义操作符addT，其实现为AddTwiceOperator
		runner.addOperator("eachLT", new EachLTOperator());
		runner.addOperator("eachLTE", new EachLTEOperator());
		runner.addOperator("GTE", new GTEOperator());
		//执行表达式，并将结果赋给r
		
		long startTime = System.currentTimeMillis();
		//Boolean[] r = (Boolean[])runner.execute(exp,null,null,false,false);
		boolean[] r = (boolean[])runner.execute(exp,null,null,false,false);
		System.out.println(Arrays.toString(r));
		//Assert.assertTrue("操作符执行错误",r);
		
		long endTime = System.currentTimeMillis();
		
		System.out.println("ms: " + (endTime-startTime));
	}
	
	@Test
	public void testMIN() throws Exception{
		String exp = "MIN([17,13,20,18,19,30],10.05)";
		ExpressRunner runner = new ExpressRunner();
		//定义操作符addT，其实现为AddTwiceOperator
		runner.addOperator("eachLT", new EachLTOperator());
		runner.addOperator("eachLTE", new EachLTEOperator());
		runner.addOperator("GTE", new GTEOperator());
		runner.addFunction("MIN", new MINFunction());
		//执行表达式，并将结果赋给r
		
		long startTime = System.currentTimeMillis();
		//Boolean[] r = (Boolean[])runner.execute(exp,null,null,false,false);
		Object r = runner.execute(exp,null,null,false,false);
		System.out.println(r);
		//Assert.assertTrue("操作符执行错误",r);
		
		long endTime = System.currentTimeMillis();
		
		System.out.println("ms: " + (endTime-startTime));
	}
	
	@Test
	public void testSUM() throws Exception{
		//String exp = "SUM([true,false,false,true,false],true) == 3";
		String exp = "SUM([1,2,3],2) == 8";
		ExpressRunner runner = new ExpressRunner();
		//定义操作符addT，其实现为AddTwiceOperator
		runner.addOperator("eachLT", new EachLTOperator());
		runner.addOperator("eachLTE", new EachLTEOperator());
		runner.addOperator("GTE", new GTEOperator());
		runner.addFunction("MIN", new MINFunction());
		runner.addFunction("SUM", new SUMFunction());
		//执行表达式，并将结果赋给r
		
		long startTime = System.currentTimeMillis();
		//Boolean[] r = (Boolean[])runner.execute(exp,null,null,false,false);
		Object r = runner.execute(exp,null,null,false,false);
		System.out.println(r);
		//Assert.assertTrue("操作符执行错误",r);
		
		long endTime = System.currentTimeMillis();
		
		System.out.println("ms: " + (endTime-startTime));
	}
	
	
	@Test
	public void testRANGE() throws Exception{
		//String exp = "RANGE([true,false,false,true,false],3)";
		//String exp = "RANGE([true,false,false,true,false],1,3)";
		String exp = "SUM(RANGE([5,3,5,5,2],1,3) GTE 4) >= 2";
		ExpressRunner runner = new ExpressRunner();
		//自定义运算符
		runner.addOperator("EACHLT", new EachLTOperator());
		runner.addOperator("EACHLTE", new EachLTEOperator());
		runner.addOperator("GTE", new GTEOperator());
		//自定义函数
		runner.addFunction("MIN", new MINFunction());
		runner.addFunction("SUM", new SUMFunction());
		runner.addFunction("RANGE", new RANGEFunction());
		//执行表达式，并将结果赋给r
		
		long startTime = System.currentTimeMillis();
		Object r = runner.execute(exp,null,null,false,false);
		//Object[] r = (Object[])runner.execute(exp,null,null,false,false);
		System.out.println(r);
		//Assert.assertTrue("操作符执行错误",r);
		
		long endTime = System.currentTimeMillis();
		
		System.out.println("ms: " + (endTime-startTime));
	}
	
	@Test
	public void testStatsFunctions() throws Exception{
		//min
		//String minExp = "min([17,13,20,18,19,30])"; //非法
		//String minExp = "min(17,13,20,18,19,30)"; //合法
		String minExp = "min(a,15)";
		
		//max
		//String maxExp = "max([17,13,20,18,19,30])";
		String maxExp = "max(17,13,20,18,19,30)";
		
		//mean
		
		//sum
				
		ExpressRunner runner = new ExpressRunner();
		
		long startTime = System.currentTimeMillis();
		
		Integer[] a = new Integer[]{1,2,3,4,5};
		IExpressContext<String,Object> expressContext = new DefaultContext<String,Object>();
		expressContext.put("a", a);
		Object r = runner.execute(minExp,expressContext,null,false,false);
		System.out.println(r);
		//Assert.assertTrue("操作符执行错误",r);
		
		long endTime = System.currentTimeMillis();
		
		System.out.println("ms: " + (endTime-startTime));
	}
	
	@Test
	public void testWHICH() throws Exception{
		//String exp = "SUM([true,false,false,true,false],true) == 3";
		String weightedroeClass = "WHICH[weightedroe > 0 and weightedroe < 5,"
				+ "weightedroe >= 5 and weightedroe < 10,"
				+ "weightedroe >= 10 and weightedroe < 15,"
				+ "weightedroe >= 15 and weightedroe < 20, "
				+ "weightedroe >= 20 and weightedroe < 25,"
				+ "weightedroe >= 25 and weightedroe < 35,"
				+ "weightedroe >= 35]";
		
		String netprofitClassExpr = "WHICH[netprofit > 0 and netprofit < 5.0e8,"
				+ "netprofit >= 5.0e8 and netprofit < 1.0e9,"
				+ "netprofit >= 1.0e9 and netprofit < 1.5e9,"
				+ "netprofit >= 1.5e9 and netprofit < 2.0e9,"
				+ "netprofit >= 2.0e9 and netprofit < 3.5e9,"
				+ "netprofit >= 3.5e9]";
		
		/*
		ExpressRunner runner = new ExpressRunner();
		//定义操作符addT，其实现为AddTwiceOperator
		runner.addOperator("eachLT", new EachLTOperator());
		runner.addOperator("eachLTE", new EachLTEOperator());
		runner.addOperator("GTE", new GTEOperator());
		runner.addFunction("MIN", new MINFunction());
		runner.addFunction("SUM", new SUMFunction());
		runner.addFunction("WHICH", new WHICHFunction());
		//执行表达式，并将结果赋给r
		*/
		ExpressRunner runner = ExpressRunnerFactory.getInstance();
		
		long startTime = System.currentTimeMillis();
		
		IExpressContext<String,Object> expressContext = new DefaultContext<String,Object>();
		expressContext.put("netprofit", 1.3E9);
		
		//Boolean[] r = (Boolean[])runner.execute(exp,null,null,false,false);
		Object r = runner.execute(netprofitClassExpr,expressContext,null,false,false);
		System.out.println(r);
		//Assert.assertTrue("操作符执行错误",r);
		
		long endTime = System.currentTimeMillis();
		
		System.out.println("ms: " + (endTime-startTime));
	}
	
	
	@Test
	public void testMA() throws Exception{
		//String exp = "RANGE([true,false,false,true,false],3)";
		//String exp = "RANGE([true,false,false,true,false],1,3)";
		String exp = "MA(ts, 5)";
		ExpressRunner runner = new ExpressRunner();
		//自定义运算符
		runner.addOperator("EACHLT", new EachLTOperator());
		runner.addOperator("EACHLTE", new EachLTEOperator());
		runner.addOperator("GTE", new GTEOperator());
		//自定义函数
		runner.addFunction("MIN", new MINFunction());
		runner.addFunction("SUM", new SUMFunction());
		runner.addFunction("RANGE", new RANGEFunction());
		runner.addFunction("MA", new MAFunction());
		//执行表达式，并将结果赋给r
		
		long startTime = System.currentTimeMillis();
		
		IExpressContext<String,Object> expressContext = new DefaultContext<String,Object>();
		expressContext.put("ts", new Double[]{10d,11d,15d,20d,17d,10.5d});
		
		Double[] r = (Double[])runner.execute(exp,expressContext,null,false,false);
		//Object[] r = (Object[])runner.execute(exp,null,null,false,false);
		System.out.println(Arrays.toString(r));
		//Assert.assertTrue("操作符执行错误",r);
		
		long endTime = System.currentTimeMillis();
		
		System.out.println("ms: " + (endTime-startTime));
	}
	
}
