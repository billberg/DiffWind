require(RPostgreSQL)
# 读入driver
drv = dbDriver("PostgreSQL")
# 填写连接信息
con = dbConnect(drv, dbname = "diffview", 
user = "diffview", password = "diffview", host="localhost", port = 5432)
# 查询语句
symbol = 'SH600104'
qry <- paste("select *
from xueqiu_stock_finance s
where s.symbol = '",symbol,"'
order by reportdate desc;", sep="")

df = dbGetQuery(con, qry)

plot(df$reportdate,df$netassgrowrate, type = 'l')
# 主营业务收入
plot(df$reportdate,df$mainbusiincome, type = 'l')
# 主营业务利润
plot(df$reportdate,df$mainbusiprofit, type = 'l')
# 利润总额
plot(df$reportdate,df$totprofit, type = 'l')
# 净利润
plot(df$reportdate,df$netprofit, type = 'l')
# 筹资活动产生的现金流量净额
plot(df$reportdate,df$finnetcflow, type = 'l')
# 股东权益合计
plot(df$reportdate,df$totsharequi, type = 'l')

