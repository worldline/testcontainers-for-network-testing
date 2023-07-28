echo ">> connecting to $1:$2 ... receiving streamed file and storing to /opt/receiving - 5 seconds idle timeout"

mkdir /opt/receiving
# -w will break after 5 seconds inactivity or if it can't establish a connection
nc -w 5 "$1" "$2" | dd of=/opt/receiving/random

echo "[TestContainers] ready for verification"
echo ">> done receiving, stopping"