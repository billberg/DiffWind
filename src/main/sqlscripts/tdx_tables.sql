-- 
create table tdx_stock_day(
"symbol" char(8),
"date" date,
"open" double precision,
"high" double precision,
"low" double precision,
"close" double precision,
"amt" double precision,
"vol" double precision
);

-- tdx_stock_day使用分区表，每5年一个分区
-- 使用PG继承表，不是严格意义上的分区表 --

-- 创建表分区
CREATE TABLE IF NOT EXISTS "tdx_stock_day_2016-2020" (
check ("date" between '2016-01-01' and '2020-12-31')
) inherits (tdx_stock_day);
CREATE TABLE IF NOT EXISTS "tdx_stock_day_2011-2015" (
check ("date" between '2011-01-01' and '2015-12-31')
) inherits (tdx_stock_day);
CREATE TABLE IF NOT EXISTS "tdx_stock_day_2006-2010" (
check ("date" between '2006-01-01' and '2010-12-31')
) inherits (tdx_stock_day);
CREATE TABLE IF NOT EXISTS "tdx_stock_day_2001-2005" (
check ("date" between '2001-01-01' and '2005-12-31')
) inherits (tdx_stock_day);


-- 创建索引
create index idx1_tdx_stock_day_2016-2020 on tdx_stock_day_2016-2020("symbol");
create index idx2_tdx_stock_day_2016-2020 on tdx_stock_day_2016-2020("date");

create index "idx1_tdx_stock_day_2011-2015" on "tdx_stock_day_2011-2015"("symbol");
create index idx2_tdx_stock_day_2011-2015 on tdx_stock_day_2011-2015("date");
