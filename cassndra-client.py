import time
import uuid
from cassandra import ConsistencyLevel, WriteTimeout, ReadTimeout, Unavailable
from cassandra.cluster import Cluster

# Set up Cassandra connection (Update with your cluster information if needed)
# cluster = Cluster(['127.0.0.1:9042'])  # Replace with your node IPs
# create cluster with 2 contact points
cluster = Cluster(['127.0.0.1'])  # Replace with your node IPs
session = cluster.connect()

# Use the created keyspace
session.set_keyspace('my_keyspace')

# Prepare the insert and select statements with a tunable consistency level
insert_stmt = session.prepare("""
    INSERT INTO my_table (id, name, value, created_at)
    VALUES (?, ?, ?, ?)
""")
insert_stmt.consistency_level = ConsistencyLevel.ONE  # Example consistency level for insert

select_stmt = session.prepare("""
    SELECT * FROM my_table WHERE id = ?
""")
select_stmt.consistency_level = ConsistencyLevel.ONE  # Example consistency level for read

# Generate a batch of unique keys
def generate_keys(n):
    return [uuid.uuid4() for _ in range(n)]

# Function to perform write operations with error handling and retries
def simulate_writes(keys, retries=3):
    inserted_count = 0
    for key in keys:
        attempts = 0
        while attempts < retries:
            try:
                session.execute(
                    insert_stmt,
                    (key, 'SampleName', 'SampleValue', int(time.time() * 1000))
                )
                inserted_count += 1
                break
            except (WriteTimeout, Unavailable) as e:
                attempts += 1
                print(f"Write failed for key {key} (Attempt {attempts}): {e}")
                time.sleep(1)  # Brief pause before retry
        else:
            print(f"Failed to insert key {key} after {retries} attempts.")
    print(f"Inserted {inserted_count}/{len(keys)} rows successfully.")

# Function to perform read operations with error handling and retries
def simulate_reads(keys, retries=3):
    read_count = 0
    for key in keys:
        attempts = 0
        while attempts < retries:
            try:
                rows = session.execute(select_stmt, (key,))
                for row in rows:
                    # print(row)
                    read_count += 1
                break
            except (ReadTimeout, Unavailable) as e:
                attempts += 1
                print(f"Read failed for key {key} (Attempt {attempts}): {e}")
                time.sleep(1)  # Brief pause before retry
        else:
            print(f"Failed to read key {key} after {retries} attempts.")
    print(f"Read {read_count}/{len(keys)} rows successfully.")

# Configuration for simulation
num_operations = 100000  # Number of insert-read operations to perform

# Generate keys
primary_keys = generate_keys(num_operations)

# Run write simulation
start_time = time.time()
simulate_writes(primary_keys)
print(f"Time for writing {num_operations} rows: {time.time() - start_time} seconds")

# Run read simulation
start_time = time.time()
simulate_reads(primary_keys)
print(f"Time for reading {num_operations} rows: {time.time() - start_time} seconds")

# Close session and cluster connection
session.shutdown()
cluster.shutdown()