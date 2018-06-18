package com.diffwind.stats.JRIchart;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.log4j.Logger;
import org.rosuda.JRI.Rengine;
import org.rosuda.REngine.REngineException;

public class TimeSeriesChart {

	private static Logger logger = Logger.getLogger(TimeSeriesChart.class);

	private static Rengine re;
	static {
		try {
			re = new Rengine(new String[] { "--vanilla" }, false, null);
	        System.out.println("Rengine created, waiting for R");

	        // the engine creates R is a new thread, so we should wait until it's
	        // ready
	        if (!re.waitForR()) {
	            System.out.println("Cannot load R");
	        }
	        
	        //re.eval("Sys.setlocale(category='LC_ALL', locale = 'English_United States.1252')");
	        //查看sessionInfo()
	        //windows
	        //re.eval("Sys.setlocale(category = 'LC_ALL',locale = 'chinese')");
	        //Mac
	        re.eval("Sys.setlocale(category = 'LC_ALL',locale = 'zh_CN.UTF-8')");
	        
		} catch (Exception e) {
			throw new RuntimeException("TimeSeriesChart error", e);
		}
	}

	/**
	 * 注: 对于同类指标，应使用同样的坐标；不同类指标采用不同坐标的图叠加
	 * 
	 * re有bug导致程序阻塞而不抛异常，推测是dev.off()没有正常关闭导致
	 * @param fileName
	 * @param mainTitle
	 * @param tsDates
	 * @param matrixHeaders
	 * @param tsMatrix
	 * @throws REngineException
	 */
	public synchronized static void chart(String fileName, String mainTitle, String[] tsDates, String[] markedDates, String[] matrixHeaders, double[][] tsMatrix)
			throws REngineException {
        
		re.assign("dateStr", tsDates);
		re.eval("date <- as.Date(dateStr)");
		re.assign("markedDateStr", markedDates);
		re.eval("markedDate <- as.Date(markedDateStr)");
		
		re.assign("y1", tsMatrix[0]);
		re.assign("y2", tsMatrix[1]);
		re.assign("y3", tsMatrix[2]);
		re.assign("text.legend", matrixHeaders);

		String ylab = matrixHeaders[0]+"/"+matrixHeaders[1]+"/"+matrixHeaders[2];
		re.assign("ylab", ylab);
		
		// String fileName =
		// "/Users/zhangjz/projects/DiffWind/"+code+".xma"+tsData.w_ma+"."+tsData.date[wBegin-1]+"-"+tsData.date[wEnd-1]+".png";
		/*
		try {
			fileName = URLEncoder.encode(fileName, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}*/
		String outputFileName = "/Users/zhangjz/projects/DiffWind.output/stats.chart/" + fileName + ".png";
		/*
		try {
			byte[] gbkBytes = outputFileName.getBytes("GBK");
			outputFileName = new String(gbkBytes);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		*/
		
		re.eval("png('" + outputFileName + "',family='SimSun', width=as.integer(length(date)/1000*800),height=800,res=160)");
		re.eval("plot(date,y1,type='l',col='gray',xaxt='n',xlab='',ylab=ylab,family='STKaiti')");
		//re.eval("abline(h=1, lty=3,col='gray')");
		//re.eval("xat <- seq(1,length(date),by=60)");
		//re.eval("axis(side=1, at=xat,labels=date[xat],las=2,cex.axis=0.7)");
		re.eval("axis.Date(1, at=seq(min(date), max(date), by='3 mon'), format='%Y/%m/%d',las=2,cex.axis=0.7)");
		re.eval("abline(v=seq(min(date), max(date), by='3 mon'), lty=3,col='gray')");
		re.eval("abline(v=markedDate, lty=3,col='orange')");
		// c.eval("lines(close[w],type='l',col='green')");
		// c.eval("abline(v=wPos, lty=3,col='grey')");
		// c.eval("text(wPos,close[w][wPos],round(offset[w][wPos],2),
		// srt=60,cex=0.5,col='red')");
		// c.eval("text(wPos,close[w][wPos],round(offset[w][wPos]*wHsl[w][wPos]/100,2),
		// srt=90,cex=0.5,col='red')");
		// c.eval("qqplot(x, y)");// 绘图
		/*
		 * par(new=T) cumsmal <- (-cumsmal) miny2 <- min(cumsmal) maxy2 <-
		 * max(min(cumsmal)+max(2000,1.3*(max(cumsmal)-min(cumsmal))),max(
		 * cumsmal))
		 * plot(cumsmal,ylim=c(miny2,maxy2),type='l',col='red',xaxt='n',axes=F,
		 * xlab='',ylab='') axis(4, pretty(c(miny2,maxy2)), col='red')
		 * 
		 * title(main=paste(code," MA"
		 * ,nd,sep=""),sub=paste(hdata$txn_date[1],"~",hdata$txn_date[nr],nr,
		 * "days"))
		 */
		
		re.assign("mainTitle", mainTitle);
		re.eval("title(main=mainTitle,family='STKaiti')");//MingLiU,STKaiti
		//re.eval("title(main='hello')");
		
		//re.eval("legend(date[length(date)-60],max(y1),pch=c(15,15,NA,NA),lty=c(NA,NA,1,1),legend=text.legend,col=c('gray','green','red'),ncol=2)");
		re.eval("legend(date[length(date)-60],max(y1),pch=c(NA,NA,NA),lty=c(1,1,1),legend=text.legend,col=c('gray','green','red'),ncol=2)");

		//对于同类指标，使用同样的坐标；不同类指标采用不同坐标的图叠加
		re.eval("par(new=T)");
		re.eval("plot(date,y2,type='l',col='green',xaxt='n',xlab='',ylab='',axes=F)");
		// c.eval("axis(side=2,lwd=0.7,lwd.ticks=0.7,tck=.01,
		// line=1.5,cex.axis=0.7,col='green',col.ticks='green')");
		re.eval("axis(side=2,lwd=0.7,lwd.ticks=0.7,tck=.01,cex.axis=0.7,mgp=c(0,-1,0),col='green',col.ticks='green',col.axis='green')");

		re.eval("abline(h=11, lty=3,col='green')");
		
		//同类指标
		re.eval("points(date,y3,type='l',col='red',xaxt='n',axes=F,xlab='',ylab='')");
		
		//不同类指标
		//re.eval("par(new=T)");
		//re.eval("plot(date,y3,type='l',col='red',xaxt='n',axes=F,xlab='',ylab='')");
		//re.eval("abline(h=0.5, lty=3,col='red')");//????
		re.eval("axis(4,pretty(y3), col='red')");
		
		re.eval("dev.off()");
		
		
		/*
		stats2 = null;
		dayFq = null;
		
		c.eval("rm(date)");
		c.eval("rm(y1)");
		c.eval("rm(y2)");
		
		c.eval("gc()");
		*/
	}
	
	
	public synchronized static void chart2(String fileName, String mainTitle, String[] tsDates, String[] matrixHeaders, double[][] tsMatrix)
			throws REngineException {
		
		re.assign("dateStr", tsDates);
		re.eval("date <- as.Date(dateStr)");
		re.assign("y1", tsMatrix[0]);
		re.assign("y2", tsMatrix[1]);

		String ylab = matrixHeaders[0]+"/"+matrixHeaders[1];
		// String fileName =
		// "/Users/zhangjz/projects/DiffWind/"+code+".xma"+tsData.w_ma+"."+tsData.date[wBegin-1]+"-"+tsData.date[wEnd-1]+".png";
		String outputFileName = "/Users/zhangjz/projects/DiffWind/stats.chart/" + fileName + ".png";
		//图片大小根据窗口大小调整
		re.eval("png('" + outputFileName + "',width=as.integer(length(date)/1000*800),height=800,res=160)");
		re.eval("plot(date,y1,type='l',col='gray',xaxt='n',xlab='',ylab='y1/y2',family='STKaiti')");
		//re.eval("abline(h=1, lty=3,col='gray')");
		//re.eval("xat <- seq(1,length(date),by=60)");
		//re.eval("axis(side=1, at=xat,labels=date[xat],las=2,cex.axis=0.7)");
		re.eval("axis.Date(1, at=seq(min(date), max(date), by='3 mon'), format='%Y/%m/%d',las=2,cex.axis=0.7)");
		re.eval("abline(v=seq(min(date), max(date), by='3 mon'), lty=3,col='gray')");
		// c.eval("lines(close[w],type='l',col='green')");
		// c.eval("abline(v=wPos, lty=3,col='grey')");
		// c.eval("text(wPos,close[w][wPos],round(offset[w][wPos],2),
		// srt=60,cex=0.5,col='red')");
		// c.eval("text(wPos,close[w][wPos],round(offset[w][wPos]*wHsl[w][wPos]/100,2),
		// srt=90,cex=0.5,col='red')");
		// c.eval("qqplot(x, y)");// 绘图
		/*
		 * par(new=T) cumsmal <- (-cumsmal) miny2 <- min(cumsmal) maxy2 <-
		 * max(min(cumsmal)+max(2000,1.3*(max(cumsmal)-min(cumsmal))),max(
		 * cumsmal))
		 * plot(cumsmal,ylim=c(miny2,maxy2),type='l',col='red',xaxt='n',axes=F,
		 * xlab='',ylab='') axis(4, pretty(c(miny2,maxy2)), col='red')
		 * 
		 * title(main=paste(code," MA"
		 * ,nd,sep=""),sub=paste(hdata$txn_date[1],"~",hdata$txn_date[nr],nr,
		 * "days"))
		 */
		
		re.assign("mainTitle", mainTitle);
		re.eval("title(main=mainTitle,family='STKaiti')");//MingLiU,STKaiti
		//re.eval("title(main='hello')");
		
		re.eval("par(new=T)");
		re.eval("plot(date,y2,type='l',col='green',xaxt='n',xlab='',ylab='',axes=F)");
		re.eval("abline(h=0.5, lty=3,col='green')");
		// c.eval("axis(side=2,lwd=0.7,lwd.ticks=0.7,tck=.01,
		// line=1.5,cex.axis=0.7,col='green',col.ticks='green')");
		re.eval("axis(side=2,lwd=0.7,lwd.ticks=0.7,tck=.01,cex.axis=0.7,mgp=c(0,-1,0),col='green',col.ticks='green',col.axis='green')");

		/*
		re.eval("par(new=T)");
		re.eval("plot(date,close,type='l',col='red',xaxt='n',axes=F,xlab='',ylab='')");
		re.eval("axis(4,pretty(close), col='red')");
		*/
		
		re.eval("dev.off()");
		
		
		/*
		stats2 = null;
		dayFq = null;
		
		c.eval("rm(date)");
		c.eval("rm(y1)");
		c.eval("rm(y2)");
		
		c.eval("gc()");
		*/
	}

}
