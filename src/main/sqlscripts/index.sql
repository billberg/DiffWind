drop index idx1_tdx_stock_day;
drop index idx2_tdx_stock_day;
-- 创建索引的性能很差 （collate default）
create index idx1_tdx_stock_day on tdx_stock_day (symbol);
-- 创建索引的性能提高很多倍，因此英文字段表定义时使用collate "C"
create index idx1_tdx_stock_day on tdx_stock_day (symbol collate "C");


create index idx2_tdx_stock_day on tdx_stock_day (date);


CREATE INDEX  "idx2_diffvalue_stock_stats_d" ON "public"."diffvalue_stock_stats_d" (lower(symbol));
CREATE INDEX  "idx3_diffvalue_stock_stats_d" ON "public"."diffvalue_stock_stats_d" (date);

-- TODO:symbol统一成小写
CREATE INDEX  "idx3_xueqiu_stock_day_fq" ON "public"."xueqiu_stock_day_fq" (upper(symbol));
