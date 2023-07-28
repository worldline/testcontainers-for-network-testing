echo ">> streaming server started, will generate a random file of $1 MB to stream and store it in /opt/streaming/random"
mkdir /opt/streaming
dd if=/dev/urandom of=/opt/streaming/random bs=1M count=$1

echo ">> done generating, ready to stream data on port 8000 to the first client"
echo "[TestContainers] ready for testing"
cat /opt/streaming/random | nc -l -p 8000

echo ">> did stream one file, stopping"