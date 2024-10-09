create keyspace my_keyspace with replication = {'class': 'SimpleStrategy', 'replication_factor': 3};

create table my_keyspace.my_table (
id uuid primary key,
name text,
value text,
created_at timestamp
);
