# HCR

HCR is a simple Redis compatible distributed session cache based on Hazelcast.

HCR uses Hazelcast in memory data grid to create a distributed hash map to store 
key value data, primarily intended for use as session cache.  All keys in HCR 
have a fixed TLL and will (by default) be evicted if they are unused for 1 hour.

HCR implements the Redis Serialisation Protocol so that off the shelf Redis 
clients can connect to it. It deliberately implements a very small number of 
Redis commands.

Any data stored into HCR will be synchronously replicated to 1 node.  It will be 
asynchronously replicated to at most 2 nodes.  HCR does not persist anything to 
durable storage, it is explicitly an in memory cache.

## Supported Commands

### `SET <key> <value>`

Store the given data under the given key in HCR.  Currently the `EX`, `PX` and 
`NX` options supported by Redis will be accepted but ignored.

### `GET <key>`

Retrieve the value for the given key.

### `DEl <key>`

Remove the given key from HCR.

### `KEYS`

List all keys currently stored in HCR.

### `INFO`

Display information about the HCR cluster

### `COMMAND`

List all available commands.

### `QUIT`

Politely close this connection

## Running HCR

HCR can easily be deployed as a Docker container:

    #> docker run bergamotmonitoring/hcr:0.0.1

This will start an instance of HCR, listening on the Docker networking bridge.  
If you start a second container, the two will see each other a form a cluster:

    #> docker run bergamotmonitoring/hcr:0.0.1

You can connect to the cluster as follows:

    #> redis-cli -h 172.17.0.2
    172.17.0.2:6379> INFO
    {
        "version": "0.0.1",
        "clients": 1,
        "keys": 0,
        "members": [
        {
            "id": "9f556d20-86be-42c4-a429-d88bff28b323",
            "address": "[172.17.0.2]:6580",
            "max_memory": 3713531904,
            "total_memory": 186646528,
            "free_memory": 135263944,
            "cpu_count": 4,
            "uptime": 31673,
        },
        {
            "id": "45f9bed0-d0b9-46e5-ae48-72c45ae4f47d",
            "address": "[172.17.0.3]:6580",
            "max_memory": 3713531904,
            "total_memory": 200802304,
            "free_memory": 160393424,
            "cpu_count": 4,
            "uptime": 26805,
        }
    ],
        "max_memory": 7427063808,
        "total_memory": 387448832,
        "free_memory": 295657368
    }

## HTTP Status API

HCR also provides a simple HTTP status API, which overs basic information about the running cluster.  You can connect to the HTTP status API on any instance, by default on port `6380`.  For example:

    #> curl http://172.17.0.2:6380/
    {
        "version": "0.0.3",
        "clients": 0,
        "keys": 0,
        "members": [
            {
                "id": "e21cfc10-b522-4fd1-8550-ce770e1acd58",
                "address": "[172.17.0.2]:6580",
                "max_memory": 3713531904,
                "total_memory": 192413696,
                "free_memory": 142391768,
                "cpu_count": 4,
                "uptime": 22110
            }
        ],
        "max_memory": 3713531904,
        "total_memory": 192413696,
        "free_memory": 142391768
    }

## License

HCR
Copyright (c) 2017, Chris Ellis
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met: 

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer. 
2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution. 

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


## Author

Chris Ellis

Twitter: @intrbiz

Web: intrbiz.com

Copyright (c) Chris Ellis 2017
