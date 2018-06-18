-- 根据净利润规模筛选，重点关注行业龙头
select f.*,h.name,h.hyname 
from xueqiu_stock_finance f, sina_zjhhangye_stock h
where f.symbol = h.symbol
and f.reportdate = '2016-12-31'
and f.netprofit > 2*1e8 -- 根据行业调整
and f.weightedroe > 7 -- 根据行业调整
order by h.hyname, f.netprofit desc;


-- 连续3年盈利指标筛选，重点关注行业龙头
-- 再结合风险指标PE/PB
with q1 as (select f.* 
from xueqiu_stock_finance f
where f.reportdate = '2016-12-31'
and f.netprofit > 10*1e8 -- 根据行业调整
and f.weightedroe > 10 -- 根据行业调整
), q2 as (select f.* 
from xueqiu_stock_finance f
where f.reportdate = '2015-12-31'
and f.netprofit > 10*1e8 -- 根据行业调整
and f.weightedroe > 10 -- 根据行业调整
), q3 as (select f.* 
from xueqiu_stock_finance f
where f.reportdate = '2014-12-31'
and f.netprofit > 10*1e8 -- 根据行业调整
and f.weightedroe > 10 -- 根据行业调整
), q4 as (
select q1.* 
from q1, q2, q3
where q1.symbol = q2.symbol
and q1.symbol = q3.symbol
)
select q4.reportdate "报表日期",q4.symbol "代码",h.name "名称",h.hyname "行业/板块",
q4.netprofit "净利润",q4.weightedroe "净资产收益率(加权)",100*q4.netprofit/q4.totalassets "总资产收益率",
q4.mainbusiincome "主营业务收入", q4.mainbusiprofit "主营业务利润",
q4.netassgrowrate "净资产增长率", q4.mainbusincgrowrate "主营业务收入增长率",
q4.netincgrowrate "净利润增长率", q4.totassgrowrate "总资产增长率", 
q4.totalassets "资产总额", q4.totalliab "负债总额", q4.totsharequi "股东权益合计", q4.cashequfinbal "期末现金及现金等价物余额"
from q4, sina_zjhhangye_stock h
where q4.symbol = h.symbol
order by h.hyname, q4.netprofit desc
;


-- 连续5年盈利指标筛选，重点关注行业龙头
-- 再结合风险指标PE/PB
@Deprecated
with q1 as (select f.* 
from xueqiu_stock_finance f
where f.reportdate = '2016-12-31'
and f.netprofit > 10*1e8 -- 根据行业调整
and f.weightedroe > 10 -- 根据行业调整
), q2 as (select f.* 
from xueqiu_stock_finance f
where f.reportdate = '2015-12-31'
and f.netprofit > 10*1e8 -- 根据行业调整
and f.weightedroe > 10 -- 根据行业调整
), q3 as (select f.* 
from xueqiu_stock_finance f
where f.reportdate = '2014-12-31'
and f.netprofit > 10*1e8 -- 根据行业调整
and f.weightedroe > 10 -- 根据行业调整
), q4 as (select f.* 
from xueqiu_stock_finance f
where f.reportdate = '2013-12-31'
and f.netprofit > 10*1e8 -- 根据行业调整
and f.weightedroe > 10 -- 根据行业调整
), q5 as (select f.* 
from xueqiu_stock_finance f
where f.reportdate = '2012-12-31'
and f.netprofit > 10*1e8 -- 根据行业调整
and f.weightedroe > 10 -- 根据行业调整
), qx as (
select q1.* 
from q1, q2, q3, q4, q5
where q1.symbol = q2.symbol
and q1.symbol = q3.symbol
and q1.symbol = q4.symbol
and q1.symbol = q5.symbol
)
select qx.reportdate "报表日期",qx.symbol "代码",h.name "名称",h.hyname "行业/板块",
qx.netprofit "净利润",qx.weightedroe "净资产收益率(加权)",100*qx.netprofit/qx.totalassets "总资产收益率",
qx.mainbusiincome "主营业务收入", qx.mainbusiprofit "主营业务利润",
qx.netassgrowrate "净资产增长率", qx.mainbusincgrowrate "主营业务收入增长率",
qx.netincgrowrate "净利润增长率", qx.totassgrowrate "总资产增长率", 
qx.totalassets "资产总额", qx.totalliab "负债总额", qx.totsharequi "股东权益合计", qx.cashequfinbal "期末现金及现金等价物余额"
from qx, sina_zjhhangye_stock h
where qx.symbol = h.symbol
order by h.hyname, qx.netprofit desc
;


-- 连续5年盈利指标筛选，重点关注行业龙头
-- 再结合风险指标PE/PB
with qx as (select f1.* 
	from xueqiu_stock_finance f1,xueqiu_stock_finance f2,xueqiu_stock_finance f3,xueqiu_stock_finance f4,xueqiu_stock_finance f5
	where f1.reportdate = make_date(extract(year from now())::int-1,12,31)
	and f1.netprofit > 10*1e8 -- 根据行业调整
	and f1.weightedroe > 10 -- 根据行业调整
	and f2.reportdate = make_date(extract(year from now())::int-2,12,31)
	and f2.netprofit > 10*1e8 -- 根据行业调整
	and f2.weightedroe > 10 -- 根据行业调整
	and f3.reportdate = make_date(extract(year from now())::int-3,12,31)
	and f3.netprofit > 10*1e8 -- 根据行业调整
	and f3.weightedroe > 10 -- 根据行业调整
	and f4.reportdate = make_date(extract(year from now())::int-4,12,31)
	and f4.netprofit > 10*1e8 -- 根据行业调整
	and f4.weightedroe > 10 -- 根据行业调整
	and f5.reportdate = make_date(extract(year from now())::int-5,12,31)
	and f5.netprofit > 10*1e8 -- 根据行业调整
	and f5.weightedroe > 10 -- 根据行业调整
	and f1.symbol = f2.symbol
	and f1.symbol = f3.symbol
	and f1.symbol = f4.symbol
	and f1.symbol = f5.symbol
)
select qx.reportdate "报表日期",qx.symbol "代码",h.name "名称",h.hycode "行业代码",h.hyname "行业/板块",
qx.netprofit "净利润",qx.weightedroe "净资产收益率(加权)",100*qx.netprofit/qx.totalassets "总资产收益率",
qx.mainbusiincome "主营业务收入", qx.mainbusiprofit "主营业务利润",
qx.netassgrowrate "净资产增长率", qx.mainbusincgrowrate "主营业务收入增长率",
qx.netincgrowrate "净利润增长率", qx.totassgrowrate "总资产增长率", 
qx.totalassets "资产总额", qx.totalliab "负债总额", qx.totsharequi "股东权益合计", qx.cashequfinbal "期末现金及现金等价物余额"
from qx, sina_zjhhangye_stock h
where qx.symbol = h.symbol
-- and h.hycode like 'hangye%' -- 只输出行业不输出概念板块
order by h.hycode desc, qx.netprofit desc
;

-- 连续5年盈利指标筛选，重点关注行业龙头
-- 再结合风险指标PE/PB
-- 高roe筛选
with qx as (select f1.* 
	from xueqiu_stock_finance f1,xueqiu_stock_finance f2,xueqiu_stock_finance f3,xueqiu_stock_finance f4,xueqiu_stock_finance f5
	where f1.reportdate = make_date(extract(year from now())::int-1,12,31)
	and f1.netprofit > 2*1e8 -- 根据行业调整
	and f1.weightedroe > 20 -- 根据行业调整
	and f2.reportdate = make_date(extract(year from now())::int-2,12,31)
	and f2.netprofit > 2*1e8 -- 根据行业调整
	and f2.weightedroe > 20 -- 根据行业调整
	and f3.reportdate = make_date(extract(year from now())::int-3,12,31)
	and f3.netprofit > 2*1e8 -- 根据行业调整
	and f3.weightedroe > 20 -- 根据行业调整
	and f4.reportdate = make_date(extract(year from now())::int-4,12,31)
	and f4.netprofit > 2*1e8 -- 根据行业调整
	and f4.weightedroe > 20 -- 根据行业调整
	and f5.reportdate = make_date(extract(year from now())::int-5,12,31)
	and f5.netprofit > 2*1e8 -- 根据行业调整
	and f5.weightedroe > 20 -- 根据行业调整
	and f1.symbol = f2.symbol
	and f1.symbol = f3.symbol
	and f1.symbol = f4.symbol
	and f1.symbol = f5.symbol
)
select qx.reportdate "报表日期",qx.symbol "代码",h.name "名称",h.hycode "行业代码",h.hyname "行业/板块",
qx.netprofit "净利润",qx.weightedroe "净资产收益率(加权)",100*qx.netprofit/qx.totalassets "总资产收益率",
qx.mainbusiincome "主营业务收入", qx.mainbusiprofit "主营业务利润",
qx.netassgrowrate "净资产增长率", qx.mainbusincgrowrate "主营业务收入增长率",
qx.netincgrowrate "净利润增长率", qx.totassgrowrate "总资产增长率", 
qx.totalassets "资产总额", qx.totalliab "负债总额", qx.totsharequi "股东权益合计", qx.cashequfinbal "期末现金及现金等价物余额"
from qx, sina_zjhhangye_stock h
where qx.symbol = h.symbol
-- and h.hycode like 'hangye%' -- 只输出行业不输出概念板块
order by h.hycode desc, qx.netprofit desc
;


-- 根据盈利指标做聚类分析
select f1.* 
	from xueqiu_stock_finance f1
	where f1.reportdate = make_date(extract(year from now())::int-1,12,31)
	and f1.netprofit > 0
	and f1.netprofit < 100*1e8
	and f1.weightedroe > 0
	;