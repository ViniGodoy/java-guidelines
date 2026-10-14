create sequence customer_seq increment by 50;

create table customer (
    id         bigint                   not null primary key,
    version    bigint                   not null,
    name       varchar(100)             not null,
    email      varchar(254)             not null,
    status     varchar(16)              not null,
    created_at timestamp with time zone not null,
    constraint uk_customer_email unique (email),
    constraint ck_customer_status check (status in ('ACTIVE', 'INACTIVE'))
);

create index idx_customer_status on customer (status);
