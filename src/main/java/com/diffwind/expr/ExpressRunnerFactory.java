package com.diffwind.expr;

import com.diffwind.expr.function.MAFunction;
import com.diffwind.expr.function.MAXFunction;
import com.diffwind.expr.function.MEANFunction;
import com.diffwind.expr.function.MINFunction;
import com.diffwind.expr.function.RANGEFunction;
import com.diffwind.expr.function.SUMFunction;
import com.diffwind.expr.function.WHICHFunction;
import com.diffwind.expr.operator.AnyGTEOperator;
import com.diffwind.expr.operator.AnyGTOperator;
import com.diffwind.expr.operator.AnyLTEOperator;
import com.diffwind.expr.operator.AnyLTOperator;
import com.diffwind.expr.operator.EachGTEOperator;
import com.diffwind.expr.operator.EachGTOperator;
import com.diffwind.expr.operator.EachLTEOperator;
import com.diffwind.expr.operator.EachLTOperator;
import com.diffwind.expr.operator.GTEOperator;
import com.diffwind.expr.operator.GTOperator;
import com.diffwind.expr.operator.LTEOperator;
import com.diffwind.expr.operator.LTOperator;
import com.ql.util.express.ExpressRunner;

public class ExpressRunnerFactory {
	
	public static ExpressRunner getInstance() {
		
		try {
			ExpressRunner runner = new ExpressRunner();
			// 自定义运算符
			runner.addOperator("EACHGT", new EachGTOperator());
			runner.addOperator("EACHGTE", new EachGTEOperator());
			runner.addOperator("EACHLT", new EachLTOperator());
			runner.addOperator("EACHLTE", new EachLTEOperator());

			runner.addOperator("ANYGT", new AnyGTOperator());
			runner.addOperator("ANYGTE", new AnyGTEOperator());
			runner.addOperator("ANYLT", new AnyLTOperator());
			runner.addOperator("ANYLTE", new AnyLTEOperator());

			runner.addOperator("GT", new GTOperator());
			runner.addOperator("GTE", new GTEOperator());
			runner.addOperator("LT", new LTOperator());
			runner.addOperator("LTE", new LTEOperator());

			// 自定义函数
			runner.addFunction("MIN", new MINFunction());
			runner.addFunction("MAX", new MAXFunction());
			runner.addFunction("SUM", new SUMFunction());
			runner.addFunction("MEAN", new MEANFunction());
			runner.addFunction("RANGE", new RANGEFunction());
			runner.addFunction("WHICH", new WHICHFunction());
			runner.addFunction("MA", new MAFunction());

			return runner;
		} catch (Exception e) {
			throw new RuntimeException("严重错误: 初始化ExpressRunner失败", e);
		}

	}
}
