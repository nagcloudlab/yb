```bash
cd yugabyte-2.23.0.0
./bin/ysqlsh
```

### Create a database

```sql
CREATE DATABASE yb_demo;
\c yb_demo;
```

### Create a table

```sql
CREATE TABLE users (
    id serial PRIMARY KEY,
    username VARCHAR (50) UNIQUE NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);
```

### to list all tables

```sql
\dt
\d
```

### describe table

```sql
\d users
```

create schema

```sql
CREATE SCHEMA yb_demo_schema;
```

list the schema

```sql
\dn
```

### create table in schema

```sql
CREATE TABLE yb_demo_schema.users (
    id serial PRIMARY KEY,
    username VARCHAR (50) UNIQUE NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);
```

set search path

```sql
SET search_path TO yb_demo_schema;
```

### to list all tables

```sql
\dt
```
