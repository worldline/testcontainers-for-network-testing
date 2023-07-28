echo ">> server started, will write data received on 8000 to /opt/receiving"
nc -l 8000 >> /opt/receiving
nc -l 8000 >> /opt/receiving
echo ">> did receive data, stopping"