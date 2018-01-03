# qantas-web-crawler

This is my Java Microservice web crawler.

It runs on port 8080 and you can start it by

> java -jar target/qantas-crawler-1.0-SNAPSHOT-swarm.jar

You can generate the docker image, it wont run by default.

> mvn docker:docker 

or 

> docker load -i qantas-web-crawler.tar

> docker run -i --network host qantas-web-crawler


There are postman/newman collection in the root directory.

> newman run Qantas.postman_collection.json
