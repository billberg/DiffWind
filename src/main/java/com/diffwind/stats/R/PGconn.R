require(RPostgreSQL)
# 读入driver
drv = dbDriver("PostgreSQL")
# 填写连接信息
con = dbConnect(drv, dbname = "diffview", 
user = "diffview", password = "diffview", host="localhost", port = 5432)
# 查询语句
qry <- "select *
from xueqiu_stock_finance s
where s.symbol = 'SZ002230'
order by reportdate desc;"
rs = dbSendQuery(con, statement = qry)
# 收割结果
df = fetch(rs, n = -1)
# 其实可以直接执行查询返回结果
df = dbGetQuery(con, qry)
plot(df$reportdate,df$netassgrowrate, type = 'l')
# 断开连接
dbDisconnect(con)
# 释放资源
dbUnloadDriver(drv)
