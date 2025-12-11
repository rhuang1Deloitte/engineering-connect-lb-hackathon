# Engineering Connect Hackathon Instructions

Thank you for taking part in the Engineering Connect December 2025 Hackathon. This hackathon will run from 4pm Thursday 4th of December 2025 until 5:30pm Thursday 11th of December 2025.

This repository contains instructions for how to get started, what the requirements are, and how to make your submission.

The task is to code your own load balancer implementation with a focus on either performance, or features.

Your team is allowed to use any programming language and tooling to complete this hackathon! See [Constraints](#Constraints) for restrictions. The only restriction is that you must provide Dockerfile so that your implementation can be built.

Recommended libraries (if your language doesn't include standard implementations) are:
 - HTTP Server and Client
 - Structured environment configuration
 - Logging

There are two paths to victory, to be eligible for the performance crown you only need to implement the base requirements. The utility prize is for the team that implements the most stretch requirements with tie being resolved with the highest performance implementation. Performance will be evaluated with a combination of latency, throughput, and concurrency.

## Constraints

 - The work required to meet the requirements must be performed entirely within the load balancer code
 - Libraries can be used, however, load balancer specific libraries are not available for use. Eg. Pingora, pyLoadBalancer
 - Work must be your teams own and cannot be plagarised from another source, even if the source allows unbounded use of that original source
 - Your submission should be distributed as a docker image. This docker image should perform all compilation and packaging steps as part of the docker build. A consumer of your image should not have to install any tooling to create this image except for Podman/Docker. As these containers may run on either ARM64 or AMD64 machines during testing, please provide a bash script each for ARM64 and AMD64 targets. An example submission that satisfies these requirements can be found in this repostory at `/submissions/example-team`.
 - Any request caching must be able to be disabled
 
## Win Conditions

There are two oppertunities to win this hackathon:
 - ***Best Performing Load Balancer***: Your load balancer successfully meets the base requirements and performs better than all other submissions for latency, connections, and transactions per second.
 - ***Most Feature Complete Load Balancer***: Your load balancer successfully meets the base requirements and implements the more features than any other submission. Ties will be settled by choosing the better performing implementation.
 
## Making Your Submission

To start your submission fork this repository and create a folder under the `/submissions` directory with your team name as the name. 
Instructions for how to fork a repository can be found here: [Fork A Repo](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/working-with-forks/fork-a-repo)

Once your team has completed the implementation make a pull request back to the original repository. This pull request will be approved by the submission closing time.
Instructions for how to make a pull request into the original repository can be found here: [Creating A Pull Request From A Fork](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/creating-a-pull-request-from-a-fork)

An example submission can be found in this repostory at `/submissions/example-team`.

***Submissions close EOD Thursday 11th of December 2025. Pull requests and commits received after 5:30pm on this day will not be eligible for submission.***

All submissions will be licenced under the GNU General Public License Version 2, a copy of which can be found in this respository and at this link: [GNU General Public License, Version 2.](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)

***IMPORTANT***: Your submission should be distributed as a docker image. This docker image should perform all compilation and packaging steps as part of the docker build. A consumer of your image should not have to install any tooling to create this image except for Podman/Docker. As these containers may run on either ARM64 or AMD64 machines during testing, please provide a bash script each for ARM64 and AMD64 targets. An example submission that satisfies these requirements can be found in this repostory at `/example-team`.

## Requirements

### ***Common Terms***
 - **Listener Rule**: A mapping from a URI on the load balancer, to a target group. The rule *may* rewrite the URI between receiving the request and make the upstream request to a target.
 - **Target**: A IP & port with an optional base URI path for an HTTP request. For example:
   - `127.0.0.1:8080` has a base URI of `/`. A request, `load-balancer:80/resource`, will be sent to the resolved address and HTTP uri `127.0.0.1/resource`
   - `127.0.0.1:8080/some/uri` has a base URI of `/some/uri`. A request, `load-balancer:80/resource`, will be sent to the resolved address and HTTP uri `127.0.0.1/some/uri/resource`
 - **Target Group**: A set of resolvable hostnames that resolve to a target. A hostname could resolve to multiple targets.
 - **URI Rewrite**: The modification of a request URI. For example, a listener rule could listen on the URI `/my/listener` but this path could need to be modified before forwarding the request to an upstream host. In this case, if our URI rewrite is configured to strip the listener path we could receive a request at `/my/listener/resource` but rewrite the path to `/resource` before making the upstream HTTP request.
 - **may**: If a requirement uses the term ***may*** then the this part of the requirement is optional.
 - **should**: If a requirement uses the term ***should*** then the this part of the requirement is required.
 
 
### ***Base Requirements***

#### **Environment Variable Configuration**
Your load balancer ***should*** be configurable through environment variables.  

The following variables ***should*** be included:
 - `LISTENER_PORT`, (integer): Sets the port that the load balancer listens on for incoming connections.
 - `CONNECTION_TIMEOUT`, (integer): Sets the timeout ,in milliseconds, for upstream requests.
 - `LOAD_BALANCING_ALGORITHM`, (string): Sets the load balancing algorithm that will determine how requests are balanced across the targets in a target group. A value for each implemented algorithm ***should*** be available under this setting, algorithms not implemented do not need to be implemented. The values for each algorithm ***should*** be as below:
   - Round Robin: `ROUND_ROBIN`
   - Weighted: `WEIGHTED`
   - Sticky: `STICKY`
   - LRT: `LRT`

In addition, your submission ***should*** have clear instructions for how to configure listener rules, and target groups. The environment variables that set these configurations are up to the teams to decide.  

Listener rules ***should*** have the following configuration parameters:
 - Path Prefix, (string): The prefix to match against incoming HTTP URIs to trigger this rule
 - Path Rewrite, (string): The prefix to strip from the URI before forwarding a request 
 - Target Group, (string): The target group to route each matched request to
 
Multiple listener rules will be configured, so the load balancer ***should*** be configurable in this way.

Target groups ***should*** have the following configuration parameters:
 - Name, (string): A name for this target group.
 - Targets, (string): A comma delimited list of `<hostname>:<port>/<base-uri>` entries that will be included in this target group. Each entry in the list ***should*** omit the protocol as the load balancer will only handle http requests.

Multiple target groups will be configured, so the load balancer ***should*** be configurable in this way.

*For this feature to be considered completed, all of the above configuration parameters should be present with clear instructions how to use them.* 

#### **DNS Resolution**
DNS facilitates the resolution of a single domain name to multiple host IP addresses. 

For example, the domain `my.cool.domain` could resolve to the ip addresses `192.168.0.1` and `192.168.0.2`.

*For this feature to be considered completed, your load balancer should resolve a DNS hostname and use each IP address as a target for a target group.*

#### **Round Robin Algorithm**
When a request is accepted by the load balancer and matched with a listener rule, the load balancer must select with an upstream host to forward the request to. In the round robin algorithm, the load balancer will select the next host in a circular pattern so that the targets have requests routed to them evenly.

For example, if you have two targets, A and B, the load balancer alternate requests between A and B evenly. If you have three targets, A, B and C, each host will see every third request the load balancer receives.

*For this feature to be considered completed, your load balancer should evenly distribute requests to targets in a target group.*

#### **Robust Error Handling**

Any errors from upstream services ***should*** have their status code and payload returned unchanged, to the caller.  

In addition, the following codes ***should*** be returned:
 - **404**: If a request is not matched to a listener rule, this status ***should*** be returned with an empty payload.
 - **502**: If the load balancer encounters a connection error during request handling, this status ***should*** be returned with an empty payload.
 - **503**: If a request is matched to a listener rule, but no targets are available, this status ***should*** be returned with an empty payload. For this exercise, there is no need to set the `Retry-After` header.
 - **504**: If a request is matched to a listener rule, but the request times out, this status ***should*** be returned with an empty payload.
 
 *For this feature to be considered completed, all of the above requirements should be met.*

#### **Path Base Application Routing**
In path based routing, the load balancer will direct request to different targets based on the HTTP request path. Request for each uri prefixed with the given URI will be routed to the same target group.  

For example, if there is some listener rule `/some/uri` that routes to the target group `myTargets`, and another listener rule `/another/uri` routes to the target group `myOtherTargets`, then requests would be routed accordingly:
 - `/some/uri/endpoint1`: routed to `myTargets`
 - `/some/uri/endpoint2`: routed to `myTargets`
 - `/another/uri/endpoint3`: routed to `myOtherTargets`

*For this feature to be considered completed, your load balancer should be able to route to different targets based on a URI path prefix. In addition, the listener rule configuration should have basic URI rewriting functionality that allows the configurer to strip some leading characters from the URI before sending the request upstream.*

### ***Stretch Requirements***

#### **Proper Use of Header Conventions**

For this feature to be considered completed, your load balancer ***should*** add headers to upstream requests, as specificed [in this article](https://docs.oracle.com/en-us/iaas/Content/Balance/Reference/httpheaders.htm), excluding the `X-Request-Id` header.

The following headers ***should*** be implemented:
 - `X-Forwarded-For`: Provides a list of connection IP addresses
 - `X-Forwarded-Host`: Identifies the original host and port requested by the client in the host HTTP request header
 - `X-Forwarded-Port`: Identifies the listener port number that the client used to connect to the load balancer
 - `X-Forwarded-Proto`: Identifies the protocol that the client used to connect to the load balancer. HTTP/HTTPS
 - `X-Real-IP`: Identifies the client's IP address
 - `X-Request-Id`: The Request ID can help you with tracking and managing a request
 - `Host`: identifies the original host and optionally the port requested by the client

This feature ***may*** be togglable using the following environment variable:
 - `HEADER_CONVENTION_ENABLE`, (boolean): Disables processing of convential headers 

#### **Instance Health Checks**

Target groups can optionally be configued with health checks to confirm whether a target is available. This health check is performed periodically and if the health checks fail over a given threshold, the target is temporarily removed from the group until the health check succeeds over a given threshold.

A health check is considered failed if any response code other than 200 is received from the target.

The following configurations ***should*** be added to the target group configuration parameters:
 - Health Check Enable, (boolean): Enables the health check for this target group.
 - Health Check Path, (string): The path that listens for health check requests.
 - Health Check Interval, (integer): The interval in milliseconds at which a health check is requested.
 - Succeed Threshold, (integer): The number of requests that must succeed before a target is considered healthy.
 - Failure Threshold, (integer): The number of requests that must fail before a target is considered unhealthy.
 
*For this feature to be considered completed, all of the above requirements should be met. Documentation should be provided for how to configure the health checks for a target group.*

#### **Weighted Algorithm**

This algorithm is a variant of the round robin algorithm. Instead of sending requests to all targets evenly, each target is assigned a weight and is assigned requests based on this value. For example, if I have a target group with two targets:
 - `target1.com`: weight = 1
 - `target2.com`: weight = 2
Then out of every 3 requests sent to the load balancer and matched to this target, 1 will be forwarded to `target1.com` and 2 will be forwarded to `target2.com`.

The weight assigned to a target will be considered a fraction of the total value of all weights for that target group. In the above example, `target1.com` has a weight of `1/3` and `target2.com` has a weight of `2/3`. Accordingly if we added a third target to the group, `target3.com` with a weight of 5, then the fractions would become:
 - `target1.com`: fraction = 1/8
 - `target2.com`: fraction = 2/8
 - `target3.com`: fraction = 5/8
 
This feature ***should*** be added to the target group configuration parameters:
 - `TARGET_WEIGHTS`, (string): A comma delimited list of `<hostname>:<weight>` entries that will be set for targets in the group. All targets in the given target group ***should*** be present in this list, otherwise an error ***should*** be thrown.

*For this feature to be considered implemented target weights should be configurable, the value 'WEIGHTED' should be valid for the LOAD_BALANCING_ALGORITHM configuration, and targets should recieve requests according to their weight.*

#### **Sticky Algorithm**

When a client make it's first request to the load balancer, it will be identified and a session will be started. A target for this session is then chosen in a round robin fashion. For the duration of that session any request from that same client will be routed to the same chosen target. Once the session has expired the next request from that client will establish a new session.

This feature ***should*** be added to the target group configuration parameters:
 - `SESSION_TTL`, (integer): The duration, in milliseconds, that a session is active for, once established. After this duration has elapsed the session will expire and a new session must be created.

*For this feature to be considered implemented session duration should be configurable, the value 'STICKY' should be valid for the LOAD_BALANCING_ALGORITHM configuration, requests for one client should always be routed to the same target for the duration of their session, and after expiry a new session should be created and the client assigned to a new target in a round robin fashion.*

#### **LRT Algorithm**

This algorithm routes requests to target within a target group based on a combination of their active connections and the response time for each of those connections. The response time is calculated as the TTFB or time to first byte. This is the time between sending a request to a target and reciving the first byte of the response payload. If it simplifies the implementation this time can be taken as the time taken to recieve the request headers, but before receiving the body. 

When a request is sent to the load balancer, the target is chosen by multiplying the number of connections by the average TTFB for each target, and then chosen the target with the lowest value.

*For this feature to be considered implemented target weights should be configurable, the value 'LRT' should be valid for the LOAD_BALANCING_ALGORITHM configuration, and targets should recieve requests according to their calculated requests and latency.*

#### **Request Retries**

The load balancer ***should*** retry upstream requests after a configued exponential backoff time, up to the configured number of retries. An exponential backoff will have an initial time to wait and then each successive retry increases this duration in an exponential manner (inital_backoff^retry_number).

This behaviour ***should*** occur under the following circumstances:
 - 5xx response codes
 - Connection error

This behaviour ***should*** *not* occur under the following circumstances:
 - 4xx response codes

When the request is considered failed the most recent error response ***should*** be returned, or a .

This feature ***should*** be configurable using the following environment variables:
 - `RETRY_ENABLE`, (boolean): Enables request retries.
 - `RETRY_BACKOFF`, (integer): The initial duration, in milliseconds, to wait before retrying a request.
 - `RETRY_COUNT`, (boolean): The number of retries that should be sent before returning an error.
 
*For this feature to be considered implemented this feature should be configurable, if an upstream request fails is should be retried in expontentially increasing durations and a failure response should be returned. 4xx response codes should not trigger this behaviour*

#### **Request Caching**

Request response time can be improved by caching frequent requests. As multiple requests for RESTful APIs do not share state, a GET request can be cached in the load balancer and looked up to decrease response time and upstream load.

The lookup key ***should*** contain both the HTTP method and the request URI. When writing an entry to the cache, the expiry time ***should*** be set. After the entry expires it ***should*** be cleaned up, how this is cleaned is up to the implementer. When looking up entries in the cache and expired entry ***should*** never be served. The cache storage ***should*** reside within the load balancer application, no external caching solutions ***should*** be used (Valkey, Redis, etc.). You ***should*** only cache GET requests with this feature.

This feature ***should*** be configurable using the following environment variables:
 - `CACHE_ENABLE`, (boolean): Enables request chaching.
 - `CACHE_TTL`, (integer): The time in milliseconds that a cached entry ***should*** live for.

*For this feature to be considered completed, the cache should be configurable by environment variables, should cache request properly, should expire keys according to the abovementioned strategy, and an expired key should never be served.*
