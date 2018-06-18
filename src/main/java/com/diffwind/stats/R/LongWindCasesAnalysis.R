require(RPostgreSQL)
# 读入driver
drv = dbDriver("PostgreSQL")
# 填写连接信息
con = dbConnect(drv, dbname = "diffview", 
user = "diffview", password = "diffview", host="localhost", port = 5432)

# 查询语句
qry <- "select f1.*
	from xueqiu_stock_finance f1
	where f1.reportdate = make_date(extract(year from now())::int-1,12,31)
	and f1.netprofit > 0
	--and f1.netprofit < 100*1e8
	and f1.weightedroe > 0"
	
qry <- "select f1.*
	from xueqiu_stock_day_fq f1
	where f1.symbol = 'sh600887'
	order by date desc"

df = dbGetQuery(con, qry)


# pe, roe
plot(df$date,df$pe, type = 'l')

# 融资融券
scode = '000625'
qry <- paste("select (e.rzrq_data->>'tdate')::date \"date\", e.rzrq_data->'rzrqyecz' \"rzrqyecz\", x.close
from eastmoney_stock_rzrq_json e left join xueqiu_stock_day_fq x
on (e.rzrq_data->>'scode' = substr(x.symbol,3) and (e.rzrq_data->>'tdate')::date = x.date)
where rzrq_data->>'scode' = '", scode, "'
order by rzrq_data->>'tdate' desc;", sep="")

df = dbGetQuery(con, qry)

markedDate <- as.Date(c('2017-12-31','2018-01-10'))
# 
plot(df$date,df$close,type='l',col='gray',xaxt='n',xlab='',ylab='Close/Rzrq',family='STKaiti')
axis.Date(1, at=seq(min(df$date), max(df$date), by='3 mon'), format='%Y/%m/%d',las=2,cex.axis=0.7)
abline(v=seq(min(df$date), max(df$date), by='3 mon'), lty=3,col='gray')
abline(v=markedDate, lty=3,col='orange')

title(main=scode,family='STKaiti')
par(new=T)
#plot(df$date,df$rzrqyecz, type = 'l',xaxt='n',xlab='')
plot(df$date,df$rzrqyecz,type='l',col='green',xaxt='n',xlab='',ylab='',axes=F)
axis(side=2,lwd=0.7,lwd.ticks=0.7,tck=.01,cex.axis=0.7,mgp=c(0,-1,0),col='green',col.ticks='green',col.axis='green')

# 输出绘图
outputFileName <- "D:/Users/zhangjz/projects/DiffWind.output/stats.chart/汽车行业-sz000625长安汽车-w[500].png"
#outputFileName <- "D:/Users/zhangjz/projects/DiffWind.output/stats.chart/000625.png"
png(outputFileName,width=as.integer(length(df$date)/1000*800),height=800,res=160)
plot(df$date,df$close,type='l',col='gray',xaxt='n',xlab='',ylab='Close/Rzrq',family='STKaiti')
axis.Date(1, at=seq(min(df$date), max(df$date), by='3 mon'), format='%Y/%m/%d',las=2,cex.axis=0.7)
abline(v=seq(min(df$date), max(df$date), by='3 mon'), lty=3,col='gray')
abline(v=markedDate, lty=3,col='orange')

title(main=scode,family='STKaiti')
		
dev.off() #关闭当前绘图设备


# 输出矢量图
plot(df$date,df$close,type='l',col='gray',xaxt='n',xlab='',ylab='Close/Rzrq',family='STKaiti')
axis.Date(1, at=seq(min(df$date), max(df$date), by='3 mon'), format='%Y/%m/%d',las=2,cex.axis=0.7)
abline(v=seq(min(df$date), max(df$date), by='3 mon'), lty=3,col='gray')
abline(v=markedDate, lty=3,col='orange')

title(main=scode,family='STKaiti')
# Mac R 3.2.1已不支持savePlot保存矢量图
#savePlot("shiliang", type=c("eps"),device=dev.cur(),restoreConsole=TRUE)
#savePlot("shiliang", type=c("eps"),device=dev.cur())

# 用pdf或postscript保存矢量图
pdf(file="saving_plot4.pdf")
postscript(file="/Users/zhangjz/projects/DiffWind.output/stats.chart/saving_plot4.ps")
plot(df$date,df$close,type='l',col='gray',xaxt='n',xlab='',ylab='Close/Rzrq',family='STKaiti')
axis.Date(1, at=seq(min(df$date), max(df$date), by='3 mon'), format='%Y/%m/%d',las=2,cex.axis=0.7)
abline(v=seq(min(df$date), max(df$date), by='3 mon'), lty=3,col='gray')
abline(v=markedDate, lty=3,col='orange')

title(main=scode,family='STKaiti')
dev.off()
