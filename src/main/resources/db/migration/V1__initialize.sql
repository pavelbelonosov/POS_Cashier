
create table account
 (
   id int8 not null,
   authority varchar(255),
   password varchar(255) not null,
   username varchar(40) not null,
   work_terminal_tid varchar(255),
   primary key (id)
 );

create table account_info
 (
   email varchar(255),
   first_name varchar(50),
   last_name varchar(50),
   telephone_number varchar(255),
   account_id int8 not null,
   primary key (account_id)
 );

create table product
 (
   id int8 not null,
   balance float8 not null,
   bar_code int8 not null,
   measurement_unit varchar(255),
   name varchar(100),
   purchase_price numeric(8, 2),
   selling_price numeric(8, 2),
   type varchar(255),
   vendor_code varchar(255),
   shop_id int8,
   primary key (id)
 );

create table sales_counter
 (
   id int8 not null,
   balance_per_day float8 not null,
   refunds_counter_per_day int4 not null,
   refunds_per_day float8 not null,
   sales_all float8 not null,
   sales_counter_per_day int4 not null,
   sales_per_day float8 not null,
   shift int4 not null,
   terminal_tid varchar(255),
   primary key (id)
 );

create table shop
 (
   id int8 not null,
   address varchar(60),
   city varchar(40),
   name varchar(40),
   primary key (id)
 );

create table shop_accounts
 (
   shops_id int8 not null,
   accounts_id int8 not null
 );

create table terminal
 (
   id int8 not null,
   cheque_header varchar(120),
   ip varchar(255),
   mid varchar(255),
   tid varchar(255),
   account_id int8,
   shop_id int8 not null,
   primary key (id)
 );

create table transaction
 (
   id int8 not null,
   amount float8 not null,
   cashier varchar(255),
   cheque text,
   date_time timestamp,
   status boolean not null,
   type varchar(255),
   terminal_id int8,
   primary key (id)
 );
alter table sales_counter
    add constraint UK_mpmwdimuyjd8u18627wgvhvek unique (terminal_tid);

alter table account_info
    add constraint FKf2vtn8ov4btro0wh94nbfq6ou foreign key (account_id) references account;

alter table product
    add constraint FK94hgg8hlqfqfnt3dag950vm7n foreign key (shop_id) references shop;

alter table shop_accounts
    add constraint FK5821jn6j9ufiboka5wq4ga6d0 foreign key (accounts_id) references account;

alter table shop_accounts
    add constraint FKdfq65pu7k4lqtjp7hgxylwv7j foreign key (shops_id) references shop;

alter table terminal
    add constraint FKekag5epu7b73qir9lgjhqexh4 foreign key (account_id) references account;

alter table terminal
    add constraint FKh1btffsr81had9b6kyygrcegw foreign key (shop_id) references shop;

alter table transaction
    add constraint FKhsb9kiwnb5cluw6dijgxm51mm foreign key (terminal_id) references terminal;

