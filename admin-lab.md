deploy 3 master and 3 tservers

mkdir -p ./yugabyte-data/node1
mkdir -p ./yugabyte-data/node2
mkdir -p ./yugabyte-data/node3

./bin/yb-master \
 --master_addresses 127.0.0.1:7100,127.0.0.2:7100,127.0.0.3:7100 \
 --rpc_bind_addresses 127.0.0.1 \
 --fs_data_dirs /Users/nag/yb/yugabyte-data/node1 \
 --webserver_port 7000

./bin/yb-master \
 --master_addresses 127.0.0.1:7100,127.0.0.2:7100,127.0.0.3:7100 \
 --rpc_bind_addresses 127.0.0.2 \
 --fs_data_dirs /Users/nag/yb/yugabyte-data/node2 \
 --webserver_port 7001

./bin/yb-master \
 --master_addresses 127.0.0.1:7100,127.0.0.2:7100,127.0.0.3:7100 \
 --rpc_bind_addresses 127.0.0.3 \
 --fs_data_dirs /Users/nag/yb/yugabyte-data/node3 \
 --webserver_port 7002

./bin/yb-tserver \
 --tserver_master_addrs 127.0.0.1:7100,127.0.0.2:7100,127.0.0.3:7100 \
 --rpc_bind_addresses 127.0.0.1 \
 --fs_data_dirs /Users/nag/yb/yugabyte-data/node1 \
 --start_pgsql_proxy \
 --pgsql_proxy_bind_address 127.0.0.1:5433 \
 --webserver_port 9000

./bin/yb-tserver \
 --tserver_master_addrs 127.0.0.1:7100,127.0.0.2:7100,127.0.0.3:7100 \
 --rpc_bind_addresses 127.0.0.2 \
 --fs_data_dirs /Users/nag/yb/yugabyte-data/node2 \
 --start_pgsql_proxy \
 --pgsql_proxy_bind_address 127.0.0.2:5433 \
 --webserver_port 9001

./bin/yb-tserver \
 --tserver_master_addrs 127.0.0.1:7100,127.0.0.2:7100,127.0.0.3:7100 \
 --rpc_bind_addresses 127.0.0.3 \
 --fs_data_dirs /Users/nag/yb/yugabyte-data/node3 \
 --start_pgsql_proxy \
 --pgsql_proxy_bind_address 127.0.0.3:5433 \
 --webserver_port 9002

./bin/ysqlsh
