sudo apt-get update
sudo apt-get install python3

python --version

wget https://downloads.yugabyte.com/releases/2.23.0.0/yugabyte-2.23.0.0-b710-linux-x86_64.tar.gz

tar xvfz yugabyte-2.23.0.0-b710-linux-x86_64.tar.gz && cd yugabyte-2.23.0.0/

./bin/yb-ctl create --rf 3 \
--master_flags "default_memory_limit_to_ram_ratio=0.05" \
--timeout-processes-running-sec 600

./bin/yb-ctl status
./bin/ycqlsh -h 127.0.0.1
