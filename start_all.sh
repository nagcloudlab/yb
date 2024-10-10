#!/bin/bash
MODE="region"
YB_PATH_BASE=./yugabyte-data

# Check if an argument is provided for fault tolerance mode
if [[ $# -eq 1 ]]; then
    case $1 in
        zone)
            MODE="zone"
            ;;
        region)
            MODE="region"
            ;;
        cloud)
            MODE="cloud"
            ;;
        *)
            echo "Invalid argument: $1"
            echo "Usage: $0 [zone|region|cloud]"
            exit 1
            ;;
    esac
fi

start_node() {
    local node_number=$1
    local advertise_address="localhost"  # Use localhost for each node to match Node 1
    local base_dir="${YB_PATH_BASE}/node${node_number}"
    local cloud_location=""
    local master_webserver_port=$((7000 + node_number - 1))
    local tserver_webserver_port=$((8200 + node_number - 1))
    local join_arg=""

    case $node_number in
        1)
            advertise_address="localhost"  # Node 1 binds to localhost
            ;;
        2)
            advertise_address="localhost"  # Node 2 joins using localhost
            join_arg="--join=localhost:7100"
            ;;
        3)
            advertise_address="localhost"  # Node 3 joins using localhost
            join_arg="--join=localhost:7100"
            ;;
    esac

    case "$MODE" in
    zone)
        cloud_location="cloud1.region1.zone$node_number"
        ;;
    region)
        cloud_location="cloud1.region$node_number.zone$node_number"
        ;;
    cloud)
        cloud_location="cloud$node_number.region$node_number.zone$node_number"
        ;;
    esac

    if [[ ! -d ${base_dir} ]]; then
        mkdir -p $base_dir
        echo "Directory created: $base_dir"
    fi

    echo "Starting node ${node_number} with the following configuration:"
    echo "advertise_address=$advertise_address, base_dir=$base_dir, cloud_location=$cloud_location, fault_tolerance=$MODE"

    # Start the node using yugabyted
    yugabyted start \
        --advertise_address=${advertise_address} \
        --base_dir=${base_dir} \
        --cloud_location=${cloud_location} \
        --fault_tolerance=$MODE \
        --master_flags="yb_num_shards_per_tserver=1,ysql_num_shards_per_tserver=1,ysql_beta_features=true,ysql_enable_packed_row=false" \
        --master_webserver_port=${master_webserver_port} \
        --tserver_flags="yb_num_shards_per_tserver=1,ysql_num_shards_per_tserver=1,ysql_beta_features=true,ysql_enable_packed_row=false" \
        --tserver_webserver_port=${tserver_webserver_port} \
        --callhome=true ${join_arg} > ${base_dir}/startup.log 2>&1

    sleep 5  # Give the node time to start

    # Check if node started successfully
    local status=$(yugabyted status --base_dir=${base_dir} | grep Status | sed 's/.*: //; s/|.*//')
    status=$(echo "$status" | xargs)
    if [[ "$status" == "Running." ]]; then
        echo "Node ${node_number} started successfully!"
    else
        echo "Failed to start node ${node_number}! Check the logs in ${base_dir}/startup.log"
        exit 1
    fi
}

# Start nodes from 1 to 3
MIN_NODE_NUM=1
MAX_NODE_NUM=3
while (( $MIN_NODE_NUM <= $MAX_NODE_NUM )); do
    echo "Starting node $MIN_NODE_NUM of $MAX_NODE_NUM"
    start_node "${MIN_NODE_NUM}"
    sleep 3
    (( MIN_NODE_NUM++ ))
done

# Check node count using YSQL
NODE_COUNT=$(ysqlsh -U yugabyte -h localhost -Atc "select count(*) from yb_servers();")

if [[ $NODE_COUNT -ne $MAX_NODE_NUM ]]; then
    echo "Node count mismatch: expected $MAX_NODE_NUM but got $NODE_COUNT"
    exit 1
else
    echo "All nodes are running. Configuring data placement..."
    yugabyted configure data_placement --base_dir=${YB_PATH_BASE}/node1 --fault_tolerance=$MODE > /dev/null 2>&1

    sleep 1
    echo "Nodes are up, checking status with yugabyted and YSQL."
    yugabyted status --base_dir=${YB_PATH_BASE}/node1
    ysqlsh -U yugabyte -h localhost -c "select * from yb_servers();"
fi
