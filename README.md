# Cloud Foundry Route Service for ABTesting

This project is for AB Testing written with Spring Boot.  This application does the following to each request:

1. Intercepts an incoming request
2. Check if the request is from existing session
3. If it is an existing session, Then It forwards the request and response to the requested URL.
4. If it is a new session and the routing logic resolves to Domain A, Then It forwards the request and response to the requested URL.
5. If it is a new session and the routing logic resolves to Domain B, Then the request is temporarily redirected to Domain B.

## Requirements
### Java, Maven
The application is written in Java 8 and packaged as a self executable JAR file. This enables it to run anywhere that Java is available.

## Configuration
Edit the below properties in the application.yml file with the desired values.
```bash
route.service.abtest.domainA: sample-web-conjunctional-ophthalmia.cfapps.io
route.service.abtest.domainB: sample-web-v2-preinflectional-compliancy.cfapps.io
route.service.abtest.domainAPercentage: 70
route.service.abtest.domainBPercentage: 30
```

## Deployment
_The following instructions assume that you have [created an account][c] and [installed the `cf` command line tool][i]._

In order to automate the deployment process as much as possible, the project contains a Cloud Foundry [manifest][y].  To deploy run the following commands:
```bash
$ cd ~/abtest-route-service/
$ ./mvnw clean package

$ cf push --no-start 
$ cf create-service p-redis shared-vm redis
$ cf create-service rediscloud 30mb redis 
$ cf bind-service abtest-route-service redis 
$ cf start abtest-route-service
```

Next, create a user provided service that contains the route service configuration information.  To do this, run the following command, substituting the address that the ABTesting route service is listening on:
```bash
$ cf create-user-provided-service abtest-route-service -r https://<ROUTE-SERVICE-ADDRESS>
```

The next step assumes that you have an application already running that you'd like to bind this route service to.  To do this, run the following command, substituting the domain and hostname bound to that application:
```bash
$ cf bind-route-service <APPLICATION-DOMAIN> abtest-route-service --hostname <APPLICATION-HOST-OF-DOMAIN-A>
```

In order to view the interception of the requests, you will need to stream the logs of the route service.  To do this, run the following command:
```bash
$ cf logs abtest-route-service
```

Finally, start making requests against your test application.  The route service's logs should start returning results that look similar to the following:
```text
2017-11-23 12:21:02.256 DEBUG 23256 --- [           main] org.cloudfoundry.service.route.abtest.Controller      : Incoming Request: <PATCH http://localhost/route-service/patch,[B@2e51d054,{X-CF-Forwarded-Url=[http://localhost/original/patch], X-CF-Proxy-Metadata=[test-proxy-metadata], X-CF-Proxy-Signature=[test-proxy-signature], cookie=[JSESSIONID=12345], Content-Type=[text/plain], Content-Length=[9]}>
2017-11-23 12:21:02.383 DEBUG 23256 --- [           main] org.cloudfoundry.service.route.abtest.Controller      : Existing session or Domain A
2017-11-23 12:21:02.416 DEBUG 23256 --- [           main] org.cloudfoundry.service.route.abtest.Controller      : Outgoing Request: <PATCH http://localhost/original/patch,[B@2e51d054,{X-CF-Proxy-Metadata=[test-proxy-metadata], X-CF-Proxy-Signature=[test-proxy-signature], cookie=[JSESSIONID=12345], Content-Type=[text/plain], Content-Length=[9]}>
2017-11-23 12:21:02.534 DEBUG 23256 --- [           main] org.cloudfoundry.service.route.abtest.Controller      : Incoming Request: <PUT http://localhost/route-service/put,[B@49122b8f,{X-CF-Forwarded-Url=[http://localhost/original/put], X-CF-Proxy-Metadata=[test-proxy-metadata], X-CF-Proxy-Signature=[test-proxy-signature], cookie=[JSESSIONID=12345], Content-Type=[text/plain], Content-Length=[9]}>
2017-11-23 12:21:02.537 DEBUG 23256 --- [           main] org.cloudfoundry.service.route.abtest.Controller      : Existing session or Domain A
2017-11-23 12:21:02.537 DEBUG 23256 --- [           main] org.cloudfoundry.service.route.abtest.Controller      : Outgoing Request: <PUT http://localhost/original/put,[B@49122b8f,{X-CF-Proxy-Metadata=[test-proxy-metadata], X-CF-Proxy-Signature=[test-proxy-signature], cookie=[JSESSIONID=12345], Content-Type=[text/plain], Content-Length=[9]}>
```


[a]: http://www.apache.org/licenses/LICENSE-2.0
[b]: http://projects.spring.io/spring-boot/
[c]: https://console.run.pivotal.io/register
[i]: http://docs.run.pivotal.io/devguide/installcf/install-go-cli.html
[j]: http://www.jetbrains.com/idea/
[r]: http://docs.cloudfoundry.org/services/route-services.html
[y]: manifest.yml