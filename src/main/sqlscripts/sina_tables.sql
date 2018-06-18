create table sina_stock
(
"symbol" char(8) primary key,
"code" char(6),
"name" varchar(20)
);

--
create table sina_zjhhangye
(
"code" varchar(20),
"name" varchar(120)
);

create table sina_zjhhangye_stock
(
"hycode" varchar(20),
"hyname" varchar(120),
"symbol" char(8),
"name" varchar(20)
);

-- 公司简介信息
create table sina_stock_corp_info
(
"symbol" char(8) primary key,
"corp_name" varchar(60),
"shangshi_date" date,
"chengli_date" date,
"zuzhixingshi" varchar(30),
"history_names" varchar(200),
"zhuce_addr" varchar(200),
"bangong_addr" varchar(200),
"jianjie" text,
"jingyingfanwei" text
);

