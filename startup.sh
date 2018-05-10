#!/bin/sh
echo -e "\033[36m############# Building the application... #############\033[0m\n"

mvn clean install

echo -e "\033[36m############# Starting the application... #############\033[0m\n"

if [ ! -f "nohup.out" ]; then
    touch "nohup.out"
fi

nohup java -server -jar target/wall-e.jar >> nohup.out 2>&1 &

tail -f -n 0 nohup.out
