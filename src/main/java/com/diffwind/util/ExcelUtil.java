package com.diffwind.util;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ColorScaleFormatting;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.usermodel.ConditionalFormattingThreshold.RangeType;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetConditionalFormatting;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheetConditionalFormatting;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelUtil {
	private static Logger logger = Logger.getLogger(ExcelUtil.class);
	
	
	/**
	 * 输出多表单
	 * @20180318: 设置单元格样式
	 * 
	 * @param tableNames
	 * @param tableDatas
	 * @param includingHeader
	 * @param isRound
	 * @param outputFileName
	 * @throws IOException
	 */
	public static void exportTables2Excel(String[] tableNames, List<Object[]>[] tableDatas, boolean includingHeader, boolean isRound, String outputFileName) throws IOException {

		Workbook wb = new XSSFWorkbook();
		
		for (int t = 0; t < tableNames.length; t++) {
			Sheet sheet = wb.createSheet();
			wb.setSheetName(t, tableNames[t]);
			
			
			Iterator<Object[]> iter = tableDatas[t].iterator();
			//header
			//Object[] header = tableDatas[t].get(0);
			Object[] header = iter.next();
			//样式集合
			Object[] styles = (Object[])header[header.length-1];
			//冻结窗口
			int[] freezePane = (int[])styles[0];
			//列分组
			int[][] columnGroups = (int[][])styles[1];
			//突出显示列
			int[] highlightCols = (int[])styles[2];
			//三色阶格式化区域集合
			//Object[] regionGroups = new Object[]{new int[][]{{11,12},{14,15},{17,18},{20,21},{23,24}}, new int[][]{{37,41},{62,101}}};
			Object[] regionGroups =  (Object[])styles[3];
			//三色阶阈值集合
			//Object[] thresholdGroups = new Object[]{new double[]{0d,0.4d,1d}, new double[]{-30d,10d,50d}};
			Object[] thresholdGroups =  (Object[])styles[4];
			
			
			//sheet.createFreezePane(5,1,5,1);
			sheet.createFreezePane(freezePane[0],freezePane[1]);
			//列分组
			for (int i = 0; i < columnGroups.length; i++) {
				sheet.groupColumn(columnGroups[i][0], columnGroups[i][1]);
				//sheet.setColumnGroupCollapsed(columnNumber, collapsed);
			}
			
			//筛选
			sheet.setAutoFilter(new CellRangeAddress(0, tableDatas[t].size()-1, 0, header.length-2));
			
			int rownum = 0;
			
			//输出头部列名
			//突出显示的列
			//int[] highlightCols = new int[0];
			if (includingHeader) {
				//如果你需要使用换行符,你需要设置  
				//单元格的样式wrap=true,代码如下:  
				XSSFCellStyle cs = (XSSFCellStyle)wb.createCellStyle();  
				cs.setWrapText(true); 
				cs.setFillForegroundColor(IndexedColors.BLUE_GREY.index);// 设置背景色
				cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
				//设置字体
				Font font = wb.createFont();
				font.setColor(IndexedColors.WHITE.getIndex());
				//font.setFontHeightInPoints((short) 8);
				cs.setFont(font);
				
				Row r0 = sheet.createRow(rownum++);
				//增加行的高度以适应2行文本的高度,设置高度单位(像素)  
				r0.setHeightInPoints((2*sheet.getDefaultRowHeightInPoints()));  
				
				//Object[] header = iter.next();
				//注: header的最后一列为样式列表
				//highlightCols = (int[])header[header.length-1];
				for (int i = 0; i < header.length-1; i++) {
					Cell c = r0.createCell(i);
					
					c.setCellValue((String)header[i]);
					
					c.setCellStyle(cs); 
				}
			}
			

			//输出数据
			//突出单元格样式
			CellStyle cs = wb.createCellStyle();
			cs.setBorderLeft(BorderStyle.MEDIUM);
			cs.setBorderRight(BorderStyle.THIN);
			cs.setLeftBorderColor(IndexedColors.LIGHT_BLUE.index);
			cs.setRightBorderColor(IndexedColors.LIGHT_BLUE.index);
			//设置字体
			XSSFFont font = (XSSFFont)wb.createFont();
			//font.setBoldweight(Font.BOLDWEIGHT_BOLD);
			font.setBold(true);
			font.setFontHeightInPoints((short) 12);  
			cs.setFont(font);
			
			
			while (iter.hasNext()) {
				Object[] dr = iter.next();
				
				Row r = sheet.createRow(rownum++);

				for (int i = 0; i < dr.length; i++) {
					Cell c = r.createCell(i);
					
					//设置样式
					if (Functions.in(i, highlightCols)) {
						c.setCellStyle(cs); 
					}
					
					if (dr[i] instanceof String)  {
						c.setCellValue((String) dr[i]);
					} else if (dr[i] instanceof Date)  {
						CellStyle csDate = wb.createCellStyle();
						DataFormat format = wb.createDataFormat();  
						//csDate.cloneStyleFrom(cs);
						//csDate.setDataFormat(HSSFDataFormat.getBuiltinFormat("yyyy/mm/dd"));
						csDate.setDataFormat(format.getFormat("yyyy/MM/dd"));  
						c.setCellStyle(csDate);
						
						c.setCellValue((Date) dr[i]);
					} else if (dr[i] instanceof Double)  {
						//test
						//logger.info("******** "+Arrays.toString(dr));
						//logger.info("******** "+dr[i]);
						c.setCellValue((isRound)? round((double)dr[i]) : (double)dr[i]);
					} else if (dr[i] instanceof Integer) {
						c.setCellValue(Double.valueOf(dr[i].toString()));
					}  else if(dr[i] != null && dr[i].getClass().isArray() ) {  
						   //如果是数组的话 然后进行操作  
						c.setCellValue(Arrays.toString((double[])dr[i]));
					} 
					
				}
				
			}
			
			//TODO: 外部传入条件规则区域集合与三色阶阈值集合
			//三色阶阈值颜色  3-color Scale
			XSSFColor[] triColors = new XSSFColor[]{new XSSFColor(new Color(98,162,56)),new XSSFColor(new Color(252,232,112)),new XSSFColor(new Color(230,104,38))};
			
			
			
			//条件规则
			SheetConditionalFormatting sheetCF = sheet.getSheetConditionalFormatting();
			for (int i = 0; i < regionGroups.length; i++) {
				
				int[][] ranges = (int[][])regionGroups[i];
				double[] triThresholds = (double[])thresholdGroups[i];
				
				CellRangeAddress [] regions1 = new CellRangeAddress[ranges.length];
				for (int j = 0; j < ranges.length; j++) {
					regions1[j] = new CellRangeAddress(1, tableDatas[t].size()-1, ranges[j][0], ranges[j][1]);
				}
				 
				ConditionalFormattingRule cfRule = sheetCF.createConditionalFormattingColorScaleRule();
				ColorScaleFormatting clrFmt = cfRule.getColorScaleFormatting();
					
					//clrFmt.getColors();
					clrFmt.setColors(triColors);
					//clrFmt.getThresholds()[0].setRangeType(RangeType.MIN);
					clrFmt.getThresholds()[0].setRangeType(RangeType.NUMBER);
					clrFmt.getThresholds()[0].setValue(triThresholds[0]);
					 //clrFmt.getThresholds()[1].setRangeType(RangeType.PERCENT);
			        clrFmt.getThresholds()[1].setRangeType(RangeType.NUMBER);
			        clrFmt.getThresholds()[1].setValue(triThresholds[1]);
			        //clrFmt.getThresholds()[2].setRangeType(RangeType.MAX);
			        clrFmt.getThresholds()[2].setRangeType(RangeType.NUMBER);
					clrFmt.getThresholds()[2].setValue(triThresholds[2]);
					
			        sheetCF.addConditionalFormatting(regions1, cfRule);
			        
			}
			
			/*
			ConditionalFormattingRule cfRule = sheetCF.createConditionalFormattingColorScaleRule();
			ColorScaleFormatting clrFmt = cfRule.getColorScaleFormatting();
			
			//clrFmt.getColors();
			clrFmt.setColors(triColors);
			//clrFmt.getThresholds()[0].setRangeType(RangeType.MIN);
			clrFmt.getThresholds()[0].setRangeType(RangeType.NUMBER);
			clrFmt.getThresholds()[0].setValue(0d);
			 //clrFmt.getThresholds()[1].setRangeType(RangeType.PERCENT);
	        clrFmt.getThresholds()[1].setRangeType(RangeType.NUMBER);
	        clrFmt.getThresholds()[1].setValue(0.4d);
	        //clrFmt.getThresholds()[2].setRangeType(RangeType.MAX);
	        clrFmt.getThresholds()[2].setRangeType(RangeType.NUMBER);
			clrFmt.getThresholds()[2].setValue(1d);
			
			//条件规则
			ConditionalFormattingRule cfRule2 = sheetCF.createConditionalFormattingColorScaleRule();
			ColorScaleFormatting clrFmt2 = cfRule2.getColorScaleFormatting();
			
			clrFmt2.setColors(triColors);
			clrFmt2.getThresholds()[0].setRangeType(RangeType.NUMBER);
			clrFmt2.getThresholds()[0].setValue(-30d);
			clrFmt2.getThresholds()[1].setRangeType(RangeType.NUMBER);
	        clrFmt2.getThresholds()[1].setValue(10d);
	        clrFmt2.getThresholds()[2].setRangeType(RangeType.NUMBER);
	        clrFmt2.getThresholds()[2].setValue(50d);
	        
	        //CellRangeAddress [] regions = { CellRangeAddress.valueOf("L:M") };
	        //CellRangeAddress [] regions = { CellRangeAddress.valueOf("L2:M27") };
	        //行列索引都是从0开始
	        CellRangeAddress [] regions1 = {new CellRangeAddress(1, tableDatas[t].size()-1, 11, 12),
	        		new CellRangeAddress(1, tableDatas[t].size()-1, 14, 15),
	        		new CellRangeAddress(1, tableDatas[t].size()-1, 17, 18),
	        		new CellRangeAddress(1, tableDatas[t].size()-1, 20, 21),
	        		new CellRangeAddress(1, tableDatas[t].size()-1, 23, 24)};
	        sheetCF.addConditionalFormatting(regions1, cfRule);
	        
	        CellRangeAddress [] regions2 = {new CellRangeAddress(1, tableDatas[t].size()-1, 37, 41),
	        		new CellRangeAddress(1, tableDatas[t].size()-1, 62, 101)};
	        sheetCF.addConditionalFormatting(regions2, cfRule2);
	        */
	        
		}
		

		// end deleted sheet
		FileOutputStream out = new FileOutputStream(outputFileName);
		wb.write(out);
		wb.close();
		out.close();
	}
	
	
	public static double round(double d) {
		if (Double.isNaN(d) || Double.isInfinite(d))
			return d;
		
		return new  BigDecimal(Double.toString(d)).setScale(2,java.math.BigDecimal.ROUND_HALF_UP).doubleValue();
	}
	
}
