
-- 查询总股本
with x as (select row_number() over (partition by symbol order by begindate desc) rn, symbol, totalshare 
from xueqiu_stock_shareschg)
select symbol,totalshare from x
where rn = 1;

-- 查询最新市值
with x as (select row_number() over (partition by symbol order by begindate desc) rn, symbol, totalshare 
from xueqiu_stock_shareschg),
y as (select symbol, max(date) lastdate
from xueqiu_stock_day
group by symbol)
select s.symbol,x.totalshare,s.close, x.totalshare*s.close as marketcap from xueqiu_stock_day s, x, y
where s.symbol = x.symbol
and s.symbol = y.symbol
and s.date = y.lastdate
and x.rn = 1;

-- 查询净利润
with x as (select row_number() over (partition by symbol order by begindate desc) rn, symbol, totalshare 
from xueqiu_stock_finance)
select symbol,totalshare from x
where rn = 1;


-- 公司基本信息
with q as (select symbol, name, string_agg(hyname,',' order by hycode desc) "hyname"
  	from sina_zjhhangye_stock group by symbol,name)
select q.symbol,q.name, q.hyname, c.shangshi_date, c.chengli_date, c.zuzhixingshi, c.zhuce_addr, c.jingyingfanwei
from q, sina_stock_corp_info c
where q.symbol = c.symbol
order by q.symbol;

--
select date_trunc('year', t.date), count(*) 
from tdx_stock_day t
where t.symbol = 'sh601318'
group by date_trunc('year', t.date);

-- 现金流好的公司
with q as (select symbol, name, string_agg(hyname,',' order by hycode desc) "hyname"
  	from sina_zjhhangye_stock group by symbol,name)
select q.symbol,q.name, q.hyname, f.*
from q, xueqiu_stock_finance f
where q.symbol = f.symbol
and f.reportdate = '2016-12-31'
and f.weightedroe > 10
and f.netprofit > 2e8
and f.operrevenue > f.netprofit
and f.cashequfinbal > 10e8
and f.cashnetr > 1e8
order by q.hyname, f.cashnetr desc;


-- 利润表
-- 查询个股历史所得税费用
select data->>'symbol' "symbol",data->>'报表期截止日' "报表期截止日", (data->>'利润总额')::float "利润总额",(data->>'所得税费用')::float "所得税费用",(data->>'净利润')::float "净利润",
(data->>'所得税费用')::float/(data->>'利润总额')::float "所得税费用/利润总额"
from xueqiu_stock_income_statement_json
where 
-- data->>'报表期截止日' = '20161231'
data->>'symbol' = 'sh600039'
order by data->>'报表期截止日' desc;


select data->>'symbol' "symbol",(data->>'利润总额')::float "利润总额",(data->>'所得税费用')::float "所得税费用",(data->>'净利润')::float "净利润",
(data->>'所得税费用')::float/(data->>'利润总额')::float "所得税费用/利润总额"
from xueqiu_stock_income_statement_json
where data->>'报表期截止日' = '20161231'
and (data->>'所得税费用')::float > 1e8
and (data->>'净利润')::float > 1e8
and (data->>'所得税费用')::float/(data->>'利润总额')::float > 0.2
order by "所得税费用/利润总额" desc;


with q1 as (select data->>'symbol' "symbol",(data->>'利润总额')::float "利润总额",(data->>'所得税费用')::float "所得税费用",(data->>'净利润')::float "净利润",
(data->>'所得税费用')::float/(data->>'利润总额')::float "所得税费用/利润总额"
from xueqiu_stock_income_statement_json
where data->>'报表期截止日' = '20161231'
and (data->>'所得税费用')::float > 1e8
and (data->>'净利润')::float > 1e8
and (data->>'所得税费用')::float/(data->>'利润总额')::float > 0.2
order by "所得税费用/利润总额" desc),
q2 as (select data->>'symbol' "symbol",(data->>'利润总额')::float "利润总额",(data->>'所得税费用')::float "所得税费用",(data->>'净利润')::float "净利润",
(data->>'所得税费用')::float/(data->>'利润总额')::float "所得税费用/利润总额"
from xueqiu_stock_income_statement_json
where data->>'报表期截止日' = '20151231'
and (data->>'所得税费用')::float > 1e8
and (data->>'净利润')::float > 1e8
and (data->>'所得税费用')::float/(data->>'利润总额')::float > 0.2
order by "所得税费用/利润总额" desc)

select q1.*,q2.*
from q1, q2
where q1.symbol = q2.symbol
order by q1.symbol;


-- 根据近3年所得税筛选（再结合roe等核心指标）
-- 国家需要重点扶持的高新技术企业，减按15%（如汽车制造行业）
with q1 as (select data->>'symbol' "symbol",(data->>'利润总额')::float "利润总额",(data->>'所得税费用')::float "所得税费用",(data->>'净利润')::float "净利润",
(data->>'所得税费用')::float/(data->>'利润总额')::float "所得税费用/利润总额"
from xueqiu_stock_income_statement_json
where data->>'报表期截止日' = '20161231'
and (data->>'所得税费用')::float > 0.5e8
and (data->>'净利润')::float > 1e8
and (data->>'所得税费用')::float/(data->>'利润总额')::float > 0.1
order by "所得税费用/利润总额" desc),
q2 as (select data->>'symbol' "symbol",(data->>'利润总额')::float "利润总额",(data->>'所得税费用')::float "所得税费用",(data->>'净利润')::float "净利润",
(data->>'所得税费用')::float/(data->>'利润总额')::float "所得税费用/利润总额"
from xueqiu_stock_income_statement_json
where data->>'报表期截止日' = '20151231'
and (data->>'所得税费用')::float > 0.5e8
and (data->>'净利润')::float > 1e8
and (data->>'所得税费用')::float/(data->>'利润总额')::float > 0.1
order by "所得税费用/利润总额" desc),
q3 as (select data->>'symbol' "symbol",(data->>'利润总额')::float "利润总额",(data->>'所得税费用')::float "所得税费用",(data->>'净利润')::float "净利润",
(data->>'所得税费用')::float/(data->>'利润总额')::float "所得税费用/利润总额"
from xueqiu_stock_income_statement_json
where data->>'报表期截止日' = '20141231'
and (data->>'所得税费用')::float > 0.5e8
and (data->>'净利润')::float > 1e8
and (data->>'所得税费用')::float/(data->>'利润总额')::float > 0.1
order by "所得税费用/利润总额" desc),
hy as (select symbol, name, string_agg(hyname,',' order by hycode desc) "hyname"
  	from sina_zjhhangye_stock group by symbol,name)

select hy.name,hy.hyname,q1.*,q2.*,q3.*
from q1, q2, q3, hy
where q1.symbol = q2.symbol
and q1.symbol = q3.symbol
and q1.symbol = hy.symbol
order by hy.hyname, q1."所得税费用/利润总额" desc;

-- 
select (rzrq_data->>'tdate')::date "交易日期",rzrq_data->'rzrqyecz' "融资融券余额差值" 
from eastmoney_stock_rzrq_json
where rzrq_data->>'scode' = '600104'
order by rzrq_data->>'tdate' desc;
--



-- 近3年利润表年报数据
with s1 as (select data->>'symbol' "symbol", data->>'报表期截止日' "报表期截止日",row_number() over (partition by data->>'symbol' order by data->>'报表期截止日' desc) "rn" 
from xueqiu_stock_income_statement_json
where data->>'报表期截止日' like '%1231'
)
select a.* 
from xueqiu_stock_income_statement_json a, s1
where a.data->>'symbol' = s1.symbol
and a.data->>'报表期截止日' = s1."报表期截止日"
and s1.rn <= 3
order by a.data->>'symbol',a.data->>'报表期截止日' desc
;


-- 近3年利润表年报数据均满足一定条件的案例
with s1 as (select data->>'symbol' "symbol", data->>'报表期截止日' "报表期截止日",row_number() over w "rn"
from xueqiu_stock_income_statement_json
where data->>'报表期截止日' like '%1231'
WINDOW w AS (partition by data->>'symbol' order by data->>'报表期截止日' desc)
),
s2 as (select a.*, s1.rn
from xueqiu_stock_income_statement_json a, s1
where a.data->>'symbol' = s1.symbol
and a.data->>'报表期截止日' = s1."报表期截止日"
and s1.rn <= 3
and (a.data->>'所得税费用')::float > 0.5e8
and (a.data->>'净利润')::float > 1e8
and (a.data->>'利润总额')::float > 0
and (a.data->>'所得税费用')::float/(a.data->>'利润总额')::float > 0.1
and ((a.data->>'营业总收入')::float-(a.data->>'营业总成本')::float IS NULL 
	or (a.data->>'营业总收入')::float-(a.data->>'营业总成本')::float > (a.data->>'净利润')::float)
order by a.data->>'symbol',a.data->>'报表期截止日' desc)

-- select * from s2 a
-- where (select max(b.rn) from s2 b where b.data->>'symbol' = a.data->>'symbol') >= 3
select data->>'symbol' "symbol",data->>'报表期截止日' "报表期截止日",
(data->>'利润总额')::float "利润总额",(data->>'所得税费用')::float "所得税费用",(data->>'净利润')::float "净利润",
		(data->>'所得税费用')::float/(data->>'利润总额')::float "所得税费用/利润总额",
		(data->>'营业税金及附加')::float/(data->>'利润总额')::float "营业税金及附加/利润总额",
		(data->>'营业总收入')::float-(data->>'营业总成本')::float "EBITDA"
from s2 a
where (select count(1) from s2 b where b.data->>'symbol' = a.data->>'symbol') = 3
;

-- 所得税排行
select data->>'symbol' "symbol",(data->>'利润总额')::float "利润总额",(data->>'所得税费用')::float "所得税费用",
			CASE WHEN (data->>'税金及附加')::float IS NULL
				 THEN (data->>'营业税金及附加')::float
            	 ELSE (data->>'税金及附加')::float
       		END "税金及附加"
		from xueqiu_stock_income_statement_json
		where data->>'报表期截止日' = '20161231'
		and (data->>'所得税费用')::float > 1e8
		order by (data->>'所得税费用')::float desc;
