### deploy yugabyte cluster

Prerequisites:

python --version

if not installed, install python

```bash
sudo apt-get update
sudo apt-get install python
supo apt-get install python3-is-python
```

Install YugabyteDB on Ubuntu

```bash
wget https://downloads.yugabyte.com/releases/2.23.0.0/yugabyte-2.23.0.0-b710-linux-x86_64.tar.gz
tar xvfz yugabyte-2.23.0.0-b710-linux-x86_64.tar.gz && cd yugabyte-2.23.0.0/
```

Start the cluster

way-1: ( single node )

```bash
./bin/post_install.sh
./bin/yugabyted start
./bin/yugabyted status
```

way-2: ( multi node ) ( 3 nodes ) by yb-ctl

```bash
./bin/yb-ctl destroy
./bin/yb-ctl create --rf 3 --master_flags "default_memory_limit_to_ram_ratio=0.05" --timeout-processes-running-sec 600
./bin/yb-ctl status
```

add new node to existing cluster

```bash
./bin/yb-ctl add_node
./bin/yb-ctl status
```

remove node from existing cluster

```bash
./bin/yb-ctl remove_node  4
```

### connect to yugabyte cluster

```bash
./bin/ysqlsh -h 127.0.0.1 -p 5433
```

### YSQL

YSQL Shell (ysqlsh)

ysqlsh is the command-line shell for YugabyteDB, derived from PostgreSQL's psql.
Use it to run SQL queries, execute commands from files, and perform admin tasks.
Connect to a node:

```bash
./bin/ysqlsh -h 127.0.0.1
```

Check server version:

```sql
SELECT version();
```

Query Timing

Display query execution time:

```sql
\timing
```

Users

Default admin users: yugabyte and postgres.
Check connection info:

```sql
\conninfo
```

List all users:

```sql
\du
```

Databases

Databases contain all objects (tables, views, etc.) and are isolated from each other.
Default databases: postgres, system_platform, template0, template1, yugabyte.

Create a database:

```sql
CREATE DATABASE testdb;
```

List all databases:

```sql
\l
```

Connect to a database:

```sql
\c testdb
```

Drop a database:

```sql
DROP DATABASE testdb;
```

Tables

Store structured data; created within schemas.

Create a table:

```sql
CREATE TABLE users (
id serial,
username CHAR(25) NOT NULL,
enabled boolean DEFAULT TRUE,
PRIMARY KEY (id)
);
```

List all tables:

```sql
\dt
```

Describe a table:

```sql
\d users
```

Schemas
Logical containers for organizing database objects (tables, views, etc.).

Create a schema:

```sql
CREATE SCHEMA myschema;
```

List schemas:

```sql
\dn
```

Create a table in a specific schema:

```sql
CREATE TABLE myschema.company (
ID INT NOT NULL,
NAME VARCHAR(20) NOT NULL,
AGE INT NOT NULL,
ADDRESS CHAR(25),
SALARY DECIMAL(18, 2),
PRIMARY KEY (ID)
);
```

View current schema:

```sql
SHOW search_path;
```

Set default schema:

```sql
SET search_path=myschema;
```

Drop schema and its objects:

```sql
DROP SCHEMA myschema CASCADE;
```

Exiting ysqlsh

```sql
\q
```

### sharding

Sharding is a method for distributing data across multiple nodes in a cluster.

Create a sharded table:

```sql
CREATE EXTENSION pgcrypto;
CREATE TABLE orders (
  order_id UUID DEFAULT gen_random_uuid(), -- Unique identifier for sharding
  customer_id UUID NOT NULL,
  order_date TIMESTAMP NOT NULL,
  status TEXT NOT NULL,
  shipping_address TEXT,
  billing_address TEXT,
  total_amount DECIMAL(10, 2),
  currency CHAR(3),
  payment_method TEXT,
  shipped_date TIMESTAMP,
  delivery_date TIMESTAMP,
  is_gift BOOLEAN DEFAULT FALSE,
  gift_message TEXT,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  PRIMARY KEY (order_id HASH) -- Sharding key
);
```

### insert data

Insert data into a table:

```sql
INSERT INTO orders (customer_id, order_date, status, total_amount, currency, payment_method)
VALUES
  (gen_random_uuid(), NOW(), 'Processing', 100.00, 'USD', 'Credit Card'),
  (gen_random_uuid(), NOW(), 'Shipped', 250.00, 'USD', 'PayPal'),
  (gen_random_uuid(), NOW(), 'Delivered', 320.00, 'EUR', 'Bank Transfer');
```

### query data

see data distribution:

List Tablets of a Table

```bash
./bin/yb-admin --master_addresses <master_ip:port> list_tablets ysql.<keyspace_name> <table_name> 0
```

List Tablet Servers to See Node Distribution

```bash
./bin/yb-admin --master_addresses <master_ip:port> list_tablet_servers
```

Use ysqlsh to See Row Hash Distribution

```sql
SELECT yb_hash_code(order_id), COUNT(*)
FROM orders
GROUP BY yb_hash_code(order_id)
ORDER BY yb_hash_code(order_id);
```

YugabyteDB's Web UI (Yugaware or Yugabyte Platform)

```bash
http://<yb-master-ip>:7000
```

Using yb_stats Extension for Deeper Insights

```bash
CREATE EXTENSION yb_stats;
SELECT * FROM yb_table_size('<table_name>');
```

### Yugabyte data types

1. Numeric Types
   SMALLINT / INT2: 2-byte integer, range: -32,768 to 32,767.
   INTEGER / INT / INT4: 4-byte integer, range: -2,147,483,648 to 2,147,483,647.
   BIGINT / INT8: 8-byte integer, range: -9,223,372,036,854,775,808 to 9,223,372,036,854,775,807.
   SERIAL, BIGSERIAL: Auto-incrementing integer.
   NUMERIC / DECIMAL: Exact numeric with user-defined precision and scale (e.g., NUMERIC(10, 2)).
   REAL: 4-byte floating-point number.
   DOUBLE PRECISION: 8-byte floating-point number.
2. Character Types
   CHAR(N) / CHARACTER(N): Fixed-length string of N characters.
   VARCHAR(N) / CHARACTER VARYING(N): Variable-length string up to N characters.
   TEXT: Variable-length string with no size limit.
3. Boolean Type
   BOOLEAN: Stores TRUE, FALSE, or NULL.
4. Date/Time Types
   DATE: Calendar date (year, month, day).
   TIME [WITHOUT TIME ZONE]: Time of day (hour, minute, second).
   TIMESTAMP [WITHOUT TIME ZONE]: Date and time (no timezone info).
   TIMESTAMPTZ / TIMESTAMP WITH TIME ZONE: Date and time with timezone info.
5. UUID Type
   UUID: Universally unique identifier. Commonly used for generating unique keys across distributed systems.
6. Binary Types
   BYTEA: Stores binary strings, suitable for raw binary data.
7. Array Type
   ARRAY: Allows storing an array of a specified data type (e.g., INTEGER[] for an array of integers).
8. JSON Types
   JSON: Stores JSON data in text format.
   JSONB: Stores JSON data in a binary format for more efficient processing.
9. Geometric Types (PostGIS)
   While YugabyteDB has limited support for these types, you may find:
   POINT: A geometric point on a plane (x, y coordinates).
   LINE, LSEG, BOX, CIRCLE: Other geometric shapes.

10. Network Types
    INET: Stores IPv4 and IPv6 host addresses.
    CIDR: Stores network blocks of IPv4 and IPv6 addresses.

11. Enumerated Types
    ENUM: A user-defined type with a predefined set of values (e.g., CREATE TYPE mood AS ENUM ('sad', 'ok', 'happy');).

12. Composite Types
    Allows defining a new data type as a composite of multiple existing types (similar to creating a structured object).

13. Range Types
    INT4RANGE, INT8RANGE, NUMRANGE, TSRANGE, TSTZRANGE, DATERANGE: Allow storing a range of values, like date ranges or numeric ranges.

14. Serial Types (Auto-increment)
    SERIAL, BIGSERIAL: Auto-increment integer columns.

15. HSTORE Type
    HSTORE: Key-value pairs in a single column.

16. Time Interval Type
    INTERVAL: Represents a span of time (e.g., INTERVAL '3 days').

17. TSVector and TSQuery
    TSVECTOR: A document vector for full-text search.
    TSQUERY: Represents a text search query.

Compatibility with PostgreSQL
YugabyteDB maintains compatibility with most PostgreSQL data types and functions. However, some advanced or less common types may have limited support or slightly different behavior.

### Indexes

Step 1: Create a Large Table (Without Index)

First, let's create a table called products with a million records to simulate a real-world
scenario.

```sql
CREATE TABLE products (
product_id UUID DEFAULT gen_random_uuid(),
name TEXT,
category TEXT,
price DECIMAL(10, 2),
in_stock BOOLEAN,
created_at TIMESTAMP DEFAULT NOW(),
PRIMARY KEY (product_id)
);
```

Insert Sample Data

We can use a loop to insert a large number of records into the products table. Here's an example:

```sql
DO $$
DECLARE
    i INTEGER;
BEGIN
    FOR i IN 1..1000000 LOOP
        INSERT INTO products (name, category, price, in_stock)
        VALUES (
            'Product ' || i,
            CASE WHEN i % 10 = 0 THEN 'Electronics'
                 WHEN i % 10 = 1 THEN 'Clothing'
                 WHEN i % 10 = 2 THEN 'Furniture'
                 WHEN i % 10 = 3 THEN 'Books'
                 ELSE 'Others' END,
            ROUND(RANDOM() * 1000, 2),
            CASE WHEN i % 2 = 0 THEN TRUE ELSE FALSE END
        );
    END LOOP;
END $$;
```

This block inserts 1 million records into products, randomly distributing values for category, price, and in_stock.

Step 2: Query Without Index
Try running a query that will scan the table without any indexing:

```sql
EXPLAIN ANALYZE
SELECT \* FROM products WHERE category = 'Electronics' AND price > 500;
```

This query will likely perform a full table scan since no index exists on category or price.
Observe the query performance and execution time.

Step 3: Create Indexes in YugabyteDB

YugabyteDB supports various indexing types that enhance query performance by avoiding full table scans. Let's explore some common types:

A. Single-Column Index
Create an index on the category column to optimize queries filtering by category:

```sql
CREATE INDEX idx_category ON products (category);
```

Re-run the query:

```sql
EXPLAIN ANALYZE
SELECT \* FROM products WHERE category = 'Electronics' AND price > 500;
```

Notice that the query now uses the index on category, which should improve performance compared to the full table scan.

B. Composite Index (Multi-Column)

For queries that filter by multiple columns, a composite index can be helpful. Create an index on both category and price:

```sql
CREATE INDEX idx_category_price ON products (category, price);
```

Re-run the query:

```sql
EXPLAIN ANALYZE
SELECT \* FROM products WHERE category = 'Electronics' AND price > 500;
```

The composite index will further optimize the query by indexing both category and price.

C. Unique Index
Create a unique index to ensure no duplicate values for a column, often used for columns like email, product codes, etc.

```sql
CREATE UNIQUE INDEX idx_unique_name ON products (name);
```

This index enforces uniqueness on the name column.

D. Partial Index
If queries only filter on a subset of the data, a partial index can be more efficient.

```sql
CREATE INDEX idx_in_stock ON products (price) WHERE in_stock = TRUE;
```

This partial index only indexes rows where in_stock is TRUE, making it more efficient for queries specifically filtering by in_stock.

Re-run a query:

```sql
EXPLAIN ANALYZE
SELECT \* FROM products WHERE in_stock = TRUE AND price > 500;
```

Step 4: Understanding Index Performance
Index Scan vs. Sequential Scan: The EXPLAIN ANALYZE output will show whether an index scan (faster) or sequential scan (slower) was used.
Execution Time: Measure how the query performance improves after adding indexes, especially for large tables.
Query Plan Details: Look for terms like Index Scan and Bitmap Index Scan in the EXPLAIN ANALYZE output to verify the index usage.
Step 5: Types of Indexes in YugabyteDB
Single-Column Index: Index on a single column to speed up queries that filter by that column.
Composite Index: Index on multiple columns to optimize queries filtering on more than one column.
Unique Index: Ensures the values in the index are unique, adding a constraint to the column.
Partial Index: An index that includes a subset of rows that meet a specific condition.
Primary Key Index: The primary key column(s) automatically have an index in YugabyteDB.
