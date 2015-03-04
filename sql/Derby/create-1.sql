create table tbl_config (config_id bigint not null, created_by varchar(50) not null, created_date timestamp not null, modified_by varchar(50) not null, modified_date timestamp not null, property_name varchar(100) not null unique, property_value varchar(1000), primary key (config_id))
create table tbl_user (user_id bigint not null, admin smallint not null, email varchar(255), user_name varchar(50) not null unique, primary key (user_id))
create index index1 on tbl_config (modified_by, modified_date)
create table tbl_sequences ( SEQUENCE_NAME varchar(255),  SEQUENCE_NEXT_VALUE integer ) 
