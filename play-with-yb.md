### deploy yugabyte cluster on ubuntu

---

Prerequisites:

python --version

if not installed, install python

```bash
sudo apt-get update
sudo apt-get install python
sudo apt-get install python-is-python3
```

Install YugabyteDB on Ubuntu

```bash
wget https://downloads.yugabyte.com/releases/2.23.0.0/yugabyte-2.23.0.0-b710-linux-x86_64.tar.gz
tar xvfz yugabyte-2.23.0.0-b710-linux-x86_64.tar.gz
cd yugabyte-2.23.0.0
```

Deploy cluster

way-1: ( single node )

```bash
./bin/post_install.sh
./bin/yugabyted start
./bin/yugabyted status
```

way-2: ( multi node ) ( 3 nodes ) by yb-ctl

```bash
./bin/yb-ctl destroy
sudo systemctl stop redis
./bin/yb-ctl create --rf 3 --master_flags "default_memory_limit_to_ram_ratio=0.05" --timeout-processes-running-sec 600
```

1. Master UI
   http://<master_ip>:7000

2. Tablet Server (TServer) UI
   http://<tserver_ip>:9000

3. Check All Nodes in the Cluster
   ./bin/yb-ctl status

add new-node (tserver) to cluster

```bash
./bin/yb-ctl add_node
./bin/yb-ctl status
```

remove node from existing cluster

```bash
./bin/yb-ctl remove_node  <node-index
```

download prometheus and grafana

```bash
wget https://github.com/prometheus/prometheus/releases/download/v3.0.0-beta.0/prometheus-3.0.0-beta.0.linux-amd64.tar.gz
tar xvfz prometheus-3.0.0-beta.0.linux-amd64.tar.gz

wget https://dl.grafana.com/enterprise/release/grafana-enterprise-11.2.2.linux-amd64.tar.gz
tar -zxvf grafana-enterprise-11.2.2.linux-amd64.tar.gz
```

update /path/to/prometheus/prometheus.yml

```yml
scrape_configs:
  - job_name: "yugabyte-master"
    metrics_path: "/prometheus-metrics"
    static_configs:
      - targets: ["127.0.0.1:7000", "127.0.0.2:7000", "127.0.0.3:7000"]

  - job_name: "yugabyte-tserver"
    metrics_path: "/prometheus-metrics"
    static_configs:
      - targets: ["127.0.0.1:9000", "127.0.0.2:9000", "127.0.0.3:9000"]
```

start prometheus

```bash
./prometheus --config.file=prometheus.yml
```

start grafana

```bash
cd /path/to/grafana
./bin/grafana server
```

Grafana UI
http://localhost:3000

Add Prometheus as a data source in Grafana:

1. Open Grafana in your browser.
2. Log in with the default credentials (admin/admin).
3. Click on the gear icon on the left sidebar to open the Configuration menu.
4. Click on Data Sources.
5. Click on Add data source.
6. Select Prometheus from the list of data sources.
7. In the URL field, enter http://localhost:9090.
8. Click Save & Test to verify the connection.

Import a dashboard in Grafana:

1. Open Grafana in your browser.
2. Log in with the default credentials (admin/admin).
3. Click on the "+" icon on the left sidebar to open the Create menu.
4. Click on Import.
5. Enter the dashboard ID (e.g., 12620) in the Grafana.com Dashboard field.
6. Click Load to load the dashboard.
7. Select the Prometheus data source you added earlier.
8. Click Import to import the dashboard.

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

### sharding in yugabyte ( data distribution )

Sharding is a method for distributing data across multiple nodes in a cluster.

Demonstrate Table and Sharding

Creating a Table with Default Hash Sharding

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE TABLE users (
  user_id UUID DEFAULT gen_random_uuid(),
  name TEXT,
  email TEXT,
  country TEXT,
  signup_date TIMESTAMP DEFAULT NOW(),
  PRIMARY KEY (user_id)
);
```

Here, user_id is the primary key, which will be used for sharding (hash partitioning by default).

Insert Some Data Insert sample data into the table to observe distribution:

```sql
INSERT INTO users (name, email, country)
SELECT 'User ' || i, 'user' || i || '@example.com', CASE
    WHEN i % 5 = 0 THEN 'USA'
    WHEN i % 5 = 1 THEN 'India'
    WHEN i % 5 = 2 THEN 'UK'
    WHEN i % 5 = 3 THEN 'Canada'
    ELSE 'Australia'
END
FROM generate_series(1, 1000) AS i;
```

This adds 1000 rows of sample data, distributed across different countries.

Observe Data Distribution Across Shards You can see how the data is distributed by using yb-admin to list the tablets:

```bash
./bin/yb-admin --master_addresses 127.0.0.1:7100 list_tablets ysql.yugabyte users 0
```

Check the Row Hash Distribution in YSQL Within YSQL, you can use the yb_hash_code() function to see how rows are hashed and distributed:

```sql
SELECT yb_hash_code(user_id) AS hash_value, COUNT(*)
FROM users
GROUP BY hash_value
ORDER BY hash_value;
```

Creating a Table with Explicit Range Partitioning

Create a Range-Partitioned Table

```sql
CREATE TABLE orders (
  order_id UUID DEFAULT gen_random_uuid(),
  user_id UUID,
  order_date TIMESTAMP DEFAULT NOW(),
  total_amount DECIMAL(10, 2),
  PRIMARY KEY (order_date, order_id) -- Include `order_date` in the primary key
) PARTITION BY RANGE (order_date);
```

Add Partitions Based on Ranges You can define how data should be partitioned based on order_date:

```sql
CREATE TABLE orders_q1 PARTITION OF orders
FOR VALUES FROM ('2024-01-01') TO ('2024-04-01');

CREATE TABLE orders_q2 PARTITION OF orders
FOR VALUES FROM ('2024-04-01') TO ('2024-07-01');

CREATE TABLE orders_q3 PARTITION OF orders
FOR VALUES FROM ('2024-07-01') TO ('2024-10-01');

CREATE TABLE orders_q4 PARTITION OF orders
FOR VALUES FROM ('2024-10-01') TO ('2025-01-01');
```

This example creates quarterly partitions for the year 2024.
Insert Sample Data and Observe Range Distribution Insert data into the table and check how it is distributed across partitions:

```sql
INSERT INTO orders (user_id, order_date, total_amount)
VALUES
(gen_random_uuid(), '2024-01-15', 100.00),
(gen_random_uuid(), '2024-05-20', 250.00),
(gen_random_uuid(), '2024-08-10', 320.00);
```

To see the partitions:

```sql
SELECT inhrelid::regclass AS partition_name
FROM pg_inherits
WHERE inhparent = 'orders'::regclass;
```

This query lists the partitions created for the orders table.

Using Composite/Hybrid Partitioning
You can also combine hash and range partitioning for a more sophisticated sharding approach.

Create a Table with Hybrid Partitioning

```sql
CREATE TABLE transactions (
  transaction_id UUID DEFAULT gen_random_uuid(),
  user_id UUID,
  transaction_date TIMESTAMP DEFAULT NOW(),
  amount DECIMAL(10, 2),
  PRIMARY KEY (transaction_date, transaction_id)
) PARTITION BY RANGE (transaction_date);

CREATE TABLE transactions_q1 PARTITION OF transactions
FOR VALUES FROM ('2024-01-01') TO ('2024-04-01');

CREATE TABLE transactions_q2 PARTITION OF transactions
FOR VALUES FROM ('2024-04-01') TO ('2024-07-01');

CREATE TABLE transactions_q3 PARTITION OF transactions
FOR VALUES FROM ('2024-07-01') TO ('2024-10-01');

CREATE TABLE transactions_q4 PARTITION OF transactions
FOR VALUES FROM ('2024-10-01') TO ('2025-01-01');
```

Insert Sample Data and Observe Hybrid Distribution Insert data into the table and check how it is distributed across partitions:

```sql
DO $$
DECLARE
    i INTEGER;
BEGIN
    FOR i IN 1..1000000 LOOP
        INSERT INTO transactions (user_id, transaction_date, amount)
        VALUES (
            gen_random_uuid(),
            ('2024-' || LPAD((1 + (i % 12))::TEXT, 2, '0') || '-' || LPAD((1 + (i % 28))::TEXT, 2, '0') || ' ' ||
             LPAD((i % 24)::TEXT, 2, '0') || ':' || LPAD((i % 60)::TEXT, 2, '0') || ':' || LPAD((i % 60)::TEXT, 2, '0'))::TIMESTAMP, -- Properly formatted timestamp
            ROUND(RANDOM() * 1000) -- Random amount
        );
    END LOOP;
END $$;
```

https://nagcloudlab.notion.site/How-many-shards-per-table-1185bab9bf8780a7b509dfc29e0e2482?pvs=4

### Indexes

Step 1: Create a Large Table (Without Index)

First, let's create a table called products with a million records to simulate a real-world
scenario.

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;
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
            ROUND(RANDOM() * 1000),
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
SELECT * FROM products WHERE category = 'Electronics' AND price > 500;
```

Query Plan Summary

Sequential Scan (Seq Scan): Scans all rows of products table, as no index is used.
Estimated Cost & Rows:
cost=0.00..105.00: Estimated execution cost.
rows=1000: Expected rows to match the filter (estimate).
Actual Execution Time & Rows:
78.624..586.595 ms: Time to start returning rows and complete the scan.
49618 rows: Actual rows returned.
Storage Filter:
Conditions: price > 500 and category = 'Electronics'.
Planning Time: 2.629 ms to plan the query execution.
Execution Time: 596.487 ms to execute and return all rows.
Peak Memory Usage: 24 kB, indicating low memory use.
Performance Issue

Seq Scan is slow due to lack of indexes on category or price.

Improvement Suggestions
Add Indexes on category or a composite index on category and price to optimize performance.

Create Indexes in YugabyteDB

YugabyteDB supports various indexing types that enhance query performance by avoiding full table scans. Let's explore some common types:

A. Single-Column Index
Create an index on the category column to optimize queries filtering by category:

```sql
CREATE INDEX idx_category ON products (category);
```

Re-run the query:

```sql
EXPLAIN ANALYZE
SELECT * FROM products WHERE category = 'Electronics' AND price > 500;
```

Notice that the query now uses the index on category, which should improve performance compared to the full table scan.

Composite Index (Multi-Column)

For queries that filter by multiple columns, a composite index can be helpful. Create an index on both category and price:

```sql
CREATE INDEX idx_category_price ON products (category, price);
```

Re-run the query:

```sql
EXPLAIN ANALYZE
SELECT * FROM products WHERE category = 'Electronics' AND price > 500;
```

The composite index will further optimize the query by indexing both category and price.

Unique Index
Create a unique index to ensure no duplicate values for a column, often used for columns like email, product codes, etc.

```sql
CREATE UNIQUE INDEX idx_unique_name ON products (name);
```

This index enforces uniqueness on the name column.

Partial Index
If queries only filter on a subset of the data, a partial index can be more efficient.

```sql
CREATE INDEX idx_in_stock ON products (price) WHERE in_stock = TRUE;
```

This partial index only indexes rows where in_stock is TRUE, making it more efficient for queries specifically filtering by in_stock.

Re-run a query:

```sql
EXPLAIN ANALYZE
SELECT * FROM products WHERE in_stock = TRUE AND price > 500;
```

Understanding Index Performance
Index Scan vs. Sequential Scan: The EXPLAIN ANALYZE output will show whether an index scan (faster) or sequential scan (slower) was used.
Execution Time: Measure how the query performance improves after adding indexes, especially for large tables.

Types of Indexes in YugabyteDB
Single-Column Index: Index on a single column to speed up queries that filter by that column.
Composite Index: Index on multiple columns to optimize queries filtering on more than one column.
Unique Index: Ensures the values in the index are unique, adding a constraint to the column.
Partial Index: An index that includes a subset of rows that meet a specific condition.
Primary Key Index: The primary key column(s) automatically have an index in YugabyteDB.

covering index

```sql
SELECT name, category, price FROM products WHERE category = 'Electronics' AND price > 500;
CREATE INDEX idx_category_price_covering ON products (category, price) INCLUDE (name);

```

expression index

```sql
SELECT * FROM products WHERE LOWER(category) = 'electronics';
CREATE INDEX idx_category_lower ON products (LOWER(category));
```

### Transactions

A transaction is a sequence of operations that are treated as a single unit of work. In YugabyteDB, transactions ensure data consistency and integrity.

ACID Properties of Transactions

Atomicity: All operations in a transaction are treated as a single unit. If any operation fails, the entire transaction is rolled back.
Consistency: Transactions move the database from one consistent state to another. Constraints are enforced to maintain data integrity.
Isolation: Transactions are isolated from each other to prevent interference. Each transaction sees a consistent snapshot of the data.
Durability: Once a transaction is committed, its changes are permanent and survive system failures.

Transaction Commands in YugabyteDB

BEGIN: Start a new transaction block.
COMMIT: Save the transaction changes to the database.
ROLLBACK: Discard the transaction changes and undo any modifications.
SAVEPOINT: Set a named point within a transaction to allow partial rollback.
RELEASE SAVEPOINT: Remove a savepoint from the transaction.
ROLLBACK TO SAVEPOINT: Roll back to a specific savepoint within the transaction.

how to achieve Atomicity in yugabyte ?

- Use BEGIN and COMMIT/ROLLBACK commands to group operations into a transaction.
- Ensure that all operations in a transaction succeed or fail together.
- Use SAVEPOINT to create intermediate points for partial rollback.
- Use ROLLBACK TO SAVEPOINT to undo changes up to a specific point.
- Use transactions to maintain data consistency and integrity.

Example: Atomicity in Transactions

Suppose you have two tables, orders and order_items, and you want to insert data into both tables as part of a single transaction.

```sql

CREATE TABLE bank_accounts (
    account_id UUID PRIMARY KEY,
    account_name TEXT,
    balance DECIMAL(10, 2)
);
```

Insert Sample Data

```sql
INSERT INTO bank_accounts (account_id, account_name, balance)
VALUES
('a0f7b3b4-0b3b-4b1b-8b1b-0b3b4b1b8b1b', 'Account A', 1000.00),
('b0f7b3b4-0b3b-4b1b-8b1b-0b3b4b1b8b1b', 'Account B', 500.00);
```

```sql
BEGIN;

-- Deduct $100 from Account A
UPDATE bank_accounts
SET balance = balance - 100
WHERE account_name = 'Account A';

-- Add $100 to Account B
UPDATE bank_accounts
SET balance = balance + 100
WHERE account_name = 'Account B';

COMMIT;
-- ROLLBACK;

```

use savepoint

```sql
BEGIN;

-- Deduct $100 from Account A
UPDATE bank_accounts
SET balance = balance - 100
WHERE account_name = 'Account A';

SAVEPOINT deduct_complete;

-- Try adding $100 to Account B
UPDATE bank_accounts
SET balance = balance + 100
WHERE account_name = 'Account B';

-- If the above update fails, rollback to the savepoint
ROLLBACK TO SAVEPOINT deduct_complete;

-- Otherwise, commit the transaction
COMMIT;
```

YugabyteDB Transaction Overview

Consensus & Replication: Uses Raft for data consistency; replication across nodes based on RF.
Transaction Manager & 2PC: Manages transactions with a Two-Phase Commit, ensuring either full commit or rollback.
Transaction Flow: BEGIN ➔ Execute Operations ➔ Prepare Phase ➔ Commit/Rollback Phase ➔ Resolve Write Intents.

how to achieve Consistency in yugabyte ?

- Use constraints (primary key, foreign key, unique) to enforce data integrity.
- Use transactions to maintain data consistency and atomicity.
- Use indexes to optimize queries and ensure data integrity.

how to achieve Isolation in yugabyte ?

common concurrency issues

- Dirty Reads: Reading uncommitted data from another transaction.
- Non-Repeatable Reads: Reading different values for the same row in a transaction.
- Phantom Reads: Reading new rows or missing rows in a transaction.
- Lost Updates: Overwriting changes made by another transaction.
- Write Skew: Concurrent writes that violate constraints.
- Deadlocks: Transactions waiting for each other to release locks.
- Starvation: Transactions waiting indefinitely due to resource contention.

solution

- Use isolation levels to control the visibility of data changes.
- Use locks to prevent concurrent access to the same data.
- Use transactions to ensure data consistency and integrity.

Isolation Levels in YugabyteDB

Read Uncommitted: Allows dirty reads, non-repeatable reads, and phantom reads.
Read Committed: Prevents dirty reads but allows non-repeatable reads and phantom reads.
Repeatable Read: Prevents dirty reads and non-repeatable reads but allows phantom reads.
Serializable: Prevents dirty reads, non-repeatable reads, phantom reads, and write skew.
Snapshot Isolation: Prevents dirty reads, non-repeatable reads, phantom reads, and write skew.

how to configure isolation level in yugabyte ?

how to see current isolation level in yugabyte ?

```sql
SHOW default_transaction_isolation;
```

```sql
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
```

https://nagcloudlab.notion.site/Isolation-Levels-1185bab9bf8780c0acdfc2c45ffc2e54?pvs=4

how to achieve Durability in yugabyte ?

- Use WAL (Write-Ahead Logging) to ensure changes are written to disk before committing.
- Use replication to maintain multiple copies of data across nodes.
- Use backups and snapshots to recover data in case of failures.

---

### YCQL

YugabyteDB supports multiple APIs, including YCQL (Yugabyte CQL), which is compatible with Apache Cassandra's CQL (Cassandra Query Language).

YCQL Shell (ycqlsh)

ycqlsh is the command-line shell for YugabyteDB's YCQL API, which is compatible with Apache Cassandra's CQL.

Connect to a node:

```bash
./bin/ycqlsh
```
