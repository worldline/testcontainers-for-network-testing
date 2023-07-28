echo ">> connecting to $1:$2 ... sending $3 -- timeout 5 seconds"

# -N shutdown on EOF, -w idle and establish timeout
printf "$3\n" | nc -N -w 5 "$1" $2

echo ">> done sending"