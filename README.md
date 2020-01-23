# qantas-web-crawler

This is my Java Microservice web crawler.

I wrote it using Jdk 13 and maven 3.6.3 and Java EE 7 for raw simplicity.

Of course in a commercialised version I would be using spring boot or dropwizard, or even AWS Lambda.

But I wanted to show that I have the essential knowledge to build with or without those toolkits. 

A lot of a projects work is in the tooling i.e getting the build chain functioning efficiently, versions all matching,

Integration services correctly etc: This all takes time to get working, but once it does it's worth it.

For example I'd like to build service mocks using Karate 

I'd also like to look at clustering this application to run over a larger set of websites. Perhaps using a messaging bus like Kafka or Jini.


Build and test

> mvn 

Build no tests 

> mvn -DskipTests

It runs on port 8080 and you can start it by

> java -jar target/qantas-crawler-1.0-SNAPSHOT-thorntail.jar

You can generate the docker image too for isolation within a container, it wont run by default.

> mvn docker:docker 

or 

> docker load -i qantas-web-crawler.tar
(This is not in the zip file because of email size restrictions)

And then

> docker run -i --network host qantas-web-crawler

There are postman/newman collection in the root directory.

> newman run Qantas.postman_collection.json


