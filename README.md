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

### Configuring HCR
    
HCR can be configured via a few environment variable which can be set on the container.

#### `log_level`

Set the logging level for HCR.  Defaults to `INFO`.  Posible values: `TRACE`, `DEBUG`, `INFO`, `WARNING`, `ERROR`, `FATAL`.

#### `hcr_port`

The port number for HCR to listen on,  Defaults to `6379`.

#### `hcr_socket_timeout`

The length of time a connection maybe idle in Seconds.  Defaults to `600` (10 Minutes).

#### `hcr_key_timeout`

The expiry time for an idle key in Seconds.  Defaults to `3600` (1 Hour).

#### `hcr_stats_port`

The port for the status HTTP server to run on.  Defaults to `6380`.

#### `hcr_host`

The hostname to connect to when running a healthcheck.  Default to `127.0.0.1`.  You probably do not need to change this.

#### `hcr_healthcheck_key`

The key to set in HCR when running a healthcheck.  Defaults to `__hcr_internal_healthcheck`.  You probably do not need to change this.

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

There is also a specific healthcheck and ping status endpoints, these will actually connect to the local instance as if it was a client and execute a quick test, these status endpoints are ideal for being used as the healthcheck for a load balancer.  The `/ping` endpoint will simply connect and run a `PING` command.  If the check fails the HTTP status code will be `500` rather than `200`:

    #> curl -v http://172.17.0.2:6380/ping
    < HTTP/1.1 200 OK
    < content-length: 12
    < host: 127.0.0.1:6380
    
    PING: PONG

Whereas the `/healthcheck` endpoint will connect an run `PING`, `SET` and `GET` commands. If the check fails the HTTP status code will be `500` rather than `200`:
    
    #> curl -v http://172.17.0.2:6380/healthcheck 
    < HTTP/1.1 200 OK
    < content-length: 49
    < host: 127.0.0.1:6380
    
    PING: PONG
    SET: OK
    GET: OK (HCR Health Check)

You can also extract the internal metrics using the `/metrics/` endpoint, this provides a wealth of information about the internal state of the server, returned in JSON:

    #> curl http://172.17.0.2:6380/metrics/
    [ {
    "type" : "parcel",
    "source" : "hcr",
    "captured" : 1512233150518,
    "readings" : [ {
        "type" : "timer",
        "name" : "command.[get].runtime",
        "count" : 7,
        "snapshot" : {
        "type" : "snapshot",
        "size" : 7,
        "median" : 2825713.0,
        "mean" : 3958370.0,
        "min" : 1842969.0,
        "max" : 1.1741609E7,
        "std_dev" : 3506427.529494134,
        "the_75th_percentile" : 4031026.0,
        "the_95th_percentile" : 1.1741609E7,
        "the_98th_percentile" : 1.1741609E7,
        "the_99th_percentile" : 1.1741609E7,
        "the_999th_percentile" : 1.1741609E7
        },
        "mean-rate" : 0.010801058878498327,
        "one-minute-rate" : 1.0589019148123417E-4,
        "five-minute-rate" : 0.026888322060947937,
        "fifteen-minute-rate" : 0.10175119879582095
    }, {
        "type" : "meter",
        "name" : "command.rate",
        "count" : 32,
        "mean-rate" : 0.049646304078514586,
        "one-minute-rate" : 6.964515578820941E-4,
        "five-minute-rate" : 0.3020022722393822,
        "fifteen-minute-rate" : 1.1976784181837432
    }, {
        "type" : "timer",
        "name" : "command.[command].runtime",
        "count" : 0,
        "snapshot" : {
        "type" : "snapshot",
        "size" : 0,
        "median" : 0.0,
        "mean" : 0.0,
        "min" : 0.0,
        "max" : 0.0,
        "std_dev" : 0.0,
        "the_75th_percentile" : 0.0,
        "the_95th_percentile" : 0.0,
        "the_98th_percentile" : 0.0,
        "the_99th_percentile" : 0.0,
        "the_999th_percentile" : 0.0
        },
        "mean-rate" : 0.0,
        "one-minute-rate" : 0.0,
        "five-minute-rate" : 0.0,
        "fifteen-minute-rate" : 0.0
    }, {
        "type" : "meter",
        "name" : "connection.open.rate",
        "count" : 9,
        "mean-rate" : 0.013963014706591805,
        "one-minute-rate" : 2.2391834282327555E-4,
        "five-minute-rate" : 0.07630437340922179,
        "fifteen-minute-rate" : 0.30009102299379475
    }, {
        "type" : "meter",
        "name" : "command.bad.rate",
        "count" : 0,
        "mean-rate" : 0.0,
        "one-minute-rate" : 0.0,
        "five-minute-rate" : 0.0,
        "fifteen-minute-rate" : 0.0
    }, {
        "type" : "meter",
        "name" : "command.[del].error.rate",
        "count" : 0,
        "mean-rate" : 0.0,
        "one-minute-rate" : 0.0,
        "five-minute-rate" : 0.0,
        "fifteen-minute-rate" : 0.0
    }, {
        "type" : "meter",
        "name" : "command.unknown.rate",
        "count" : 0,
        "mean-rate" : 0.0,
        "one-minute-rate" : 0.0,
        "five-minute-rate" : 0.0,
        "fifteen-minute-rate" : 0.0
    }, {
        "type" : "meter",
        "name" : "connection.error.rate",
        "count" : 0,
        "mean-rate" : 0.0,
        "one-minute-rate" : 0.0,
        "five-minute-rate" : 0.0,
        "fifteen-minute-rate" : 0.0
    }, {
        "type" : "meter",
        "name" : "command.[info].error.rate",
        "count" : 0,
        "mean-rate" : 0.0,
        "one-minute-rate" : 0.0,
        "five-minute-rate" : 0.0,
        "fifteen-minute-rate" : 0.0
    }, {
        "type" : "meter",
        "name" : "command.[set].error.rate",
        "count" : 0,
        "mean-rate" : 0.0,
        "one-minute-rate" : 0.0,
        "five-minute-rate" : 0.0,
        "fifteen-minute-rate" : 0.0
    }, {
        "type" : "meter",
        "name" : "connection.close.rate",
        "count" : 9,
        "mean-rate" : 0.013963013072627123,
        "one-minute-rate" : 2.2391834282327555E-4,
        "five-minute-rate" : 0.07630437340922179,
        "fifteen-minute-rate" : 0.30009102299379475
    }, {
        "type" : "timer",
        "name" : "command.[ping].runtime",
        "count" : 9,
        "snapshot" : {
        "type" : "snapshot",
        "size" : 9,
        "median" : 1104565.0,
        "mean" : 1270239.3333333333,
        "min" : 636003.0,
        "max" : 3371061.0,
        "std_dev" : 835072.4715357046,
        "the_75th_percentile" : 1369428.0,
        "the_95th_percentile" : 3371061.0,
        "the_98th_percentile" : 3371061.0,
        "the_99th_percentile" : 3371061.0,
        "the_999th_percentile" : 3371061.0
        },
        "mean-rate" : 0.013887058108727932,
        "one-minute-rate" : 1.97574917038024E-4,
        "five-minute-rate" : 0.028476164584413512,
        "fifteen-minute-rate" : 0.1030896615202982
    }, {
        "type" : "timer",
        "name" : "command.[del].runtime",
        "count" : 0,
        "snapshot" : {
        "type" : "snapshot",
        "size" : 0,
        "median" : 0.0,
        "mean" : 0.0,
        "min" : 0.0,
        "max" : 0.0,
        "std_dev" : 0.0,
        "the_75th_percentile" : 0.0,
        "the_95th_percentile" : 0.0,
        "the_98th_percentile" : 0.0,
        "the_99th_percentile" : 0.0,
        "the_999th_percentile" : 0.0
        },
        "mean-rate" : 0.0,
        "one-minute-rate" : 0.0,
        "five-minute-rate" : 0.0,
        "fifteen-minute-rate" : 0.0
    }, {
        "type" : "meter",
        "name" : "command.[keys].error.rate",
        "count" : 0,
        "mean-rate" : 0.0,
        "one-minute-rate" : 0.0,
        "five-minute-rate" : 0.0,
        "fifteen-minute-rate" : 0.0
    }, {
        "type" : "counter",
        "name" : "active.connections",
        "count" : 0
    }, {
        "type" : "timer",
        "name" : "command.[set].runtime",
        "count" : 7,
        "snapshot" : {
        "type" : "snapshot",
        "size" : 7,
        "median" : 2420975.0,
        "mean" : 2.561941442857143E7,
        "min" : 1787810.0,
        "max" : 1.63955229E8,
        "std_dev" : 6.1003561747686915E7,
        "the_75th_percentile" : 3598749.0,
        "the_95th_percentile" : 1.63955229E8,
        "the_98th_percentile" : 1.63955229E8,
        "the_99th_percentile" : 1.63955229E8,
        "the_999th_percentile" : 1.63955229E8
        },
        "mean-rate" : 0.010801055588689644,
        "one-minute-rate" : 1.0589019148123417E-4,
        "five-minute-rate" : 0.026888322060947937,
        "fifteen-minute-rate" : 0.10175119879582095
    }, {
        "type" : "meter",
        "name" : "command.[command].error.rate",
        "count" : 0,
        "mean-rate" : 0.0,
        "one-minute-rate" : 0.0,
        "five-minute-rate" : 0.0,
        "fifteen-minute-rate" : 0.0
    }, {
        "type" : "meter",
        "name" : "connection.idle.timeout.rate",
        "count" : 0,
        "mean-rate" : 0.0,
        "one-minute-rate" : 0.0,
        "five-minute-rate" : 0.0,
        "fifteen-minute-rate" : 0.0
    }, {
        "type" : "timer",
        "name" : "command.[keys].runtime",
        "count" : 0,
        "snapshot" : {
        "type" : "snapshot",
        "size" : 0,
        "median" : 0.0,
        "mean" : 0.0,
        "min" : 0.0,
        "max" : 0.0,
        "std_dev" : 0.0,
        "the_75th_percentile" : 0.0,
        "the_95th_percentile" : 0.0,
        "the_98th_percentile" : 0.0,
        "the_99th_percentile" : 0.0,
        "the_999th_percentile" : 0.0
        },
        "mean-rate" : 0.0,
        "one-minute-rate" : 0.0,
        "five-minute-rate" : 0.0,
        "fifteen-minute-rate" : 0.0
    }, {
        "type" : "timer",
        "name" : "command.[info].runtime",
        "count" : 0,
        "snapshot" : {
        "type" : "snapshot",
        "size" : 0,
        "median" : 0.0,
        "mean" : 0.0,
        "min" : 0.0,
        "max" : 0.0,
        "std_dev" : 0.0,
        "the_75th_percentile" : 0.0,
        "the_95th_percentile" : 0.0,
        "the_98th_percentile" : 0.0,
        "the_99th_percentile" : 0.0,
        "the_999th_percentile" : 0.0
        },
        "mean-rate" : 0.0,
        "one-minute-rate" : 0.0,
        "five-minute-rate" : 0.0,
        "fifteen-minute-rate" : 0.0
    }, {
        "type" : "meter",
        "name" : "command.[ping].error.rate",
        "count" : 0,
        "mean-rate" : 0.0,
        "one-minute-rate" : 0.0,
        "five-minute-rate" : 0.0,
        "fifteen-minute-rate" : 0.0
    }, {
        "type" : "meter",
        "name" : "command.[get].error.rate",
        "count" : 0,
        "mean-rate" : 0.0,
        "one-minute-rate" : 0.0,
        "five-minute-rate" : 0.0,
        "fifteen-minute-rate" : 0.0
    }, {
        "type" : "meter",
        "name" : "command.[quit].error.rate",
        "count" : 0,
        "mean-rate" : 0.0,
        "one-minute-rate" : 0.0,
        "five-minute-rate" : 0.0,
        "fifteen-minute-rate" : 0.0
    }, {
        "type" : "timer",
        "name" : "command.[quit].runtime",
        "count" : 9,
        "snapshot" : {
        "type" : "snapshot",
        "size" : 9,
        "median" : 1098465.0,
        "mean" : 1279630.0,
        "min" : 713099.0,
        "max" : 2677584.0,
        "std_dev" : 676376.707529909,
        "the_75th_percentile" : 1667621.5,
        "the_95th_percentile" : 2677584.0,
        "the_98th_percentile" : 2677584.0,
        "the_99th_percentile" : 2677584.0,
        "the_999th_percentile" : 2677584.0
        },
        "mean-rate" : 0.013887053527354318,
        "one-minute-rate" : 1.97574917038024E-4,
        "five-minute-rate" : 0.028476164584413512,
        "fifteen-minute-rate" : 0.1030896615202982
    }, {
        "type" : "timer",
        "name" : "connection.lifetime",
        "count" : 21,
        "snapshot" : {
        "type" : "snapshot",
        "size" : 21,
        "median" : 2.0134062E7,
        "mean" : 5.4342367571428575E7,
        "min" : 1269999.0,
        "max" : 3.36305111E8,
        "std_dev" : 9.857235789368394E7,
        "the_75th_percentile" : 2.39842425E7,
        "the_95th_percentile" : 3.321349156999999E8,
        "the_98th_percentile" : 3.36305111E8,
        "the_99th_percentile" : 3.36305111E8,
        "the_999th_percentile" : 3.36305111E8
        },
        "mean-rate" : 0.032206481116071696,
        "one-minute-rate" : 0.03883041383359731,
        "five-minute-rate" : 0.019818434847760098,
        "fifteen-minute-rate" : 0.01409139471316158
    } ]
    }, {
    "type" : "parcel",
    "source" : "com.intrbiz.jvm",
    "captured" : 1512233150540,
    "readings" : [ {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[PS Old Gen].collection.usage_threshold",
        "value" : 0
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.classes.unloaded",
        "value" : 0
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.jit.time",
        "value" : 9446
    }, {
        "type" : "integer-gauge",
        "name" : "com.intrbiz.jvm.memory.objects_pending_finalization",
        "value" : 0
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[PS Survivor Space].collection.usage_threshold",
        "value" : 0
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.uptime",
        "value" : 652371
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.non_heap.initial",
        "value" : 2555904
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[Compressed Class Space].initial",
        "value" : 0
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[Compressed Class Space].committed",
        "value" : 3932160
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.start_time",
        "value" : 1512232498240
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[Compressed Class Space].used",
        "value" : 49108992
    }, {
        "type" : "string-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[Compressed Class Space].type",
        "value" : "Non-heap memory"
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[PS Eden Space].used.peak",
        "value" : 66060288
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.non_heap.free",
        "value" : -35772993
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.heap.committed",
        "value" : 189792256
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.thread.total_started",
        "value" : 58
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[Code Cache].committed",
        "value" : 5373952
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[PS Survivor Space].used.peak",
        "value" : 10476208
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.heap.max",
        "value" : 3713531904
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[PS Old Gen].collection.usage_threshold_crossed",
        "value" : 0
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.non_heap.committed",
        "value" : 36306944
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[PS Eden Space].collection.usage_threshold_crossed",
        "value" : 0
    }, {
        "type" : "string-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[PS Old Gen].type",
        "value" : "Heap memory"
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[Metaspace].usage_threshold_crossed",
        "value" : 0
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[PS Eden Space].max",
        "value" : 1371537408
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[PS Old Gen].committed",
        "value" : 113246208
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[Code Cache].used.peak",
        "value" : 5236352
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[Metaspace].initial",
        "value" : 0
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[PS Old Gen].used",
        "value" : 49108992
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[PS Survivor Space].max",
        "value" : 10485760
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.non_heap.max",
        "value" : -1
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[PS Eden Space].committed",
        "value" : 66060288
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[PS Survivor Space].collection.usage_threshold_crossed",
        "value" : 0
    }, {
        "type" : "integer-gauge",
        "name" : "com.intrbiz.jvm.thread.peak.count",
        "value" : 57
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[PS Eden Space].collection.usage_threshold",
        "value" : 0
    }, {
        "type" : "string-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[Code Cache].type",
        "value" : "Non-heap memory"
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[Code Cache].used",
        "value" : 49108992
    }, {
        "type" : "string-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[PS Survivor Space].name",
        "value" : "PS Survivor Space"
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[PS Old Gen].used.peak",
        "value" : 13101296
    }, {
        "type" : "string-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[Metaspace].name",
        "value" : "Metaspace"
    }, {
        "type" : "string-gauge",
        "name" : "com.intrbiz.jvm.vm.name",
        "value" : "Java HotSpot(TM) 64-Bit Server VM"
    }, {
        "type" : "integer-gauge",
        "name" : "com.intrbiz.jvm.available_processors",
        "value" : 4
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[PS Old Gen].initial",
        "value" : 175112192
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.heap.free",
        "value" : 3664422912
    }, {
        "type" : "double-gauge",
        "name" : "com.intrbiz.jvm.memory.heap.free.percentage",
        "value" : 98.67756644430327
    }, {
        "type" : "integer-gauge",
        "name" : "com.intrbiz.jvm.thread.count",
        "value" : 57
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.heap.used",
        "value" : 49108992
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[Code Cache].initial",
        "value" : 2555904
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[Metaspace].usage_threshold",
        "value" : 0
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[PS Old Gen].usage_threshold_crossed",
        "value" : 0
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.heap.initial",
        "value" : 262144000
    }, {
        "type" : "string-gauge",
        "name" : "com.intrbiz.jvm.jit.name",
        "value" : "HotSpot 64-Bit Tiered Compilers"
    }, {
        "type" : "double-gauge",
        "name" : "com.intrbiz.jvm.memory.heap.used.percentage",
        "value" : 1.3224335556967388
    }, {
        "type" : "string-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[Compressed Class Space].name",
        "value" : "Compressed Class Space"
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[Code Cache].usage_threshold_crossed",
        "value" : 0
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[PS Survivor Space].committed",
        "value" : 10485760
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[PS Eden Space].initial",
        "value" : 66060288
    }, {
        "type" : "string-gauge",
        "name" : "com.intrbiz.jvm.vm.vendor",
        "value" : "Oracle Corporation"
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[Code Cache].max",
        "value" : 251658240
    }, {
        "type" : "string-gauge",
        "name" : "com.intrbiz.jvm.vm.version",
        "value" : "25.25-b02"
    }, {
        "type" : "string-gauge",
        "name" : "com.intrbiz.jvm.os.arch",
        "value" : "amd64"
    }, {
        "type" : "string-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[PS Eden Space].name",
        "value" : "PS Eden Space"
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[Compressed Class Space].max",
        "value" : 1073741824
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[PS Old Gen].max",
        "value" : 2785017856
    }, {
        "type" : "string-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[PS Eden Space].type",
        "value" : "Heap memory"
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[PS Old Gen].usage_threshold",
        "value" : 0
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[PS Eden Space].used",
        "value" : 49108992
    }, {
        "type" : "string-gauge",
        "name" : "com.intrbiz.jvm.os.name",
        "value" : "Linux"
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[Code Cache].usage_threshold",
        "value" : 0
    }, {
        "type" : "string-gauge",
        "name" : "com.intrbiz.jvm.os.version",
        "value" : "4.13.12-1-default"
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[Compressed Class Space].used.peak",
        "value" : 3810312
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[PS Survivor Space].initial",
        "value" : 10485760
    }, {
        "type" : "integer-gauge",
        "name" : "com.intrbiz.jvm.classes.loaded",
        "value" : 5813
    }, {
        "type" : "string-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[Metaspace].type",
        "value" : "Non-heap memory"
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.total.used",
        "value" : 84881984
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[Metaspace].max",
        "value" : -1
    }, {
        "type" : "string-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[PS Old Gen].name",
        "value" : "PS Old Gen"
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.non_heap.used",
        "value" : 35772992
    }, {
        "type" : "double-gauge",
        "name" : "com.intrbiz.jvm.system_load_average",
        "value" : 1.18
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[Compressed Class Space].usage_threshold_crossed",
        "value" : 0
    }, {
        "type" : "integer-gauge",
        "name" : "com.intrbiz.jvm.thread.daemon.count",
        "value" : 5
    }, {
        "type" : "string-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[Code Cache].name",
        "value" : "Code Cache"
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[Metaspace].committed",
        "value" : 27000832
    }, {
        "type" : "string-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[PS Survivor Space].type",
        "value" : "Heap memory"
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[Metaspace].used",
        "value" : 49108992
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[Compressed Class Space].usage_threshold",
        "value" : 0
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[PS Survivor Space].used",
        "value" : 49108992
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.memory.pool.[Metaspace].used.peak",
        "value" : 26726328
    }, {
        "type" : "long-gauge",
        "name" : "com.intrbiz.jvm.classes.total_loaded",
        "value" : 5813
    } ]
    }, {
    "type" : "parcel",
    "source" : "status",
    "captured" : 1512233150546,
    "readings" : [ {
        "type" : "meter",
        "name" : "request.unknown.rate",
        "count" : 2,
        "mean-rate" : 0.0030671607292745554,
        "one-minute-rate" : 0.02490778899904623,
        "five-minute-rate" : 0.006288975787638508,
        "fifteen-minute-rate" : 0.002179432534806779
    }, {
        "type" : "timer",
        "name" : "handler.[root].runtime",
        "count" : 0,
        "snapshot" : {
        "type" : "snapshot",
        "size" : 0,
        "median" : 0.0,
        "mean" : 0.0,
        "min" : 0.0,
        "max" : 0.0,
        "std_dev" : 0.0,
        "the_75th_percentile" : 0.0,
        "the_95th_percentile" : 0.0,
        "the_98th_percentile" : 0.0,
        "the_99th_percentile" : 0.0,
        "the_999th_percentile" : 0.0
        },
        "mean-rate" : 0.0,
        "one-minute-rate" : 0.0,
        "five-minute-rate" : 0.0,
        "fifteen-minute-rate" : 0.0
    }, {
        "type" : "timer",
        "name" : "handler.[ping].runtime",
        "count" : 2,
        "snapshot" : {
        "type" : "snapshot",
        "size" : 2,
        "median" : 8778281.5,
        "mean" : 8778281.5,
        "min" : 8209904.0,
        "max" : 9346659.0,
        "std_dev" : 803807.1690477139,
        "the_75th_percentile" : 9346659.0,
        "the_95th_percentile" : 9346659.0,
        "the_98th_percentile" : 9346659.0,
        "the_99th_percentile" : 9346659.0,
        "the_999th_percentile" : 9346659.0
        },
        "mean-rate" : 0.0030859175575968733,
        "one-minute-rate" : 9.168472555678982E-5,
        "five-minute-rate" : 0.0015878425234655323,
        "fifteen-minute-rate" : 0.0013384627244772977
    }, {
        "type" : "meter",
        "name" : "request.rate",
        "count" : 11,
        "mean-rate" : 0.016869377922040563,
        "one-minute-rate" : 0.013729406599196452,
        "five-minute-rate" : 0.008363397128189981,
        "fifteen-minute-rate" : 0.006503874636407889
    }, {
        "type" : "meter",
        "name" : "connection.close.rate",
        "count" : 12,
        "mean-rate" : 0.018402776782226956,
        "one-minute-rate" : 0.0386371955982427,
        "five-minute-rate" : 0.014652372915828491,
        "fifteen-minute-rate" : 0.008683307171214667
    }, {
        "type" : "meter",
        "name" : "connection.open.rate",
        "count" : 13,
        "mean-rate" : 0.01993628396267424,
        "one-minute-rate" : 0.0386371955982427,
        "five-minute-rate" : 0.014652372915828491,
        "fifteen-minute-rate" : 0.008683307171214667
    }, {
        "type" : "timer",
        "name" : "handler.[metrics].runtime",
        "count" : 1,
        "snapshot" : {
        "type" : "snapshot",
        "size" : 1,
        "median" : 3.3333274E8,
        "mean" : 3.3333274E8,
        "min" : 3.3333274E8,
        "max" : 3.3333274E8,
        "std_dev" : 0.0,
        "the_75th_percentile" : 3.3333274E8,
        "the_95th_percentile" : 3.3333274E8,
        "the_98th_percentile" : 3.3333274E8,
        "the_99th_percentile" : 3.3333274E8,
        "the_999th_percentile" : 3.3333274E8
        },
        "mean-rate" : 0.0015429631692142625,
        "one-minute-rate" : 0.013536188363841833,
        "five-minute-rate" : 0.0031973351962583784,
        "fifteen-minute-rate" : 0.001095787094460976
    }, {
        "type" : "timer",
        "name" : "handler.[healthcheck].runtime",
        "count" : 7,
        "snapshot" : {
        "type" : "snapshot",
        "size" : 7,
        "median" : 1.9266178E7,
        "mean" : 5.2644725571428575E7,
        "min" : 1.5531576E7,
        "max" : 2.55156317E8,
        "std_dev" : 8.933936129660785E7,
        "the_75th_percentile" : 2.360461E7,
        "the_95th_percentile" : 2.55156317E8,
        "the_98th_percentile" : 2.55156317E8,
        "the_99th_percentile" : 2.55156317E8,
        "the_999th_percentile" : 2.55156317E8
        },
        "mean-rate" : 0.010800718589402432,
        "one-minute-rate" : 1.0589019148123417E-4,
        "five-minute-rate" : 0.026888322060947937,
        "fifteen-minute-rate" : 0.10175119879582095
    }, {
        "type" : "meter",
        "name" : "request.bad.rate",
        "count" : 0,
        "mean-rate" : 0.0,
        "one-minute-rate" : 0.0,
        "five-minute-rate" : 0.0,
        "fifteen-minute-rate" : 0.0
    }, {
        "type" : "counter",
        "name" : "active.connections",
        "count" : 1
    }, {
        "type" : "meter",
        "name" : "connection.error.rate",
        "count" : 0,
        "mean-rate" : 0.0,
        "one-minute-rate" : 0.0,
        "five-minute-rate" : 0.0,
        "fifteen-minute-rate" : 0.0
    } ]
    } ]

The `/metrics` endpoint also allows you to filter the returned metrics, in the form: `/metrics/[<source>][/<name>]`.  Metrics are namespaced into `source`s and each have a `name`, you can fetch all metrics in a source or an individual metric, for example:

    #> curl http://172.17.0.2:6380/metrics/hcr/active.connections
    [ {
    "type" : "parcel",
    "source" : "hcr",
    "captured" : 1512233819267,
    "readings" : [ {
        "type" : "counter",
        "name" : "active.connections",
        "count" : 0
    } ]
    } ]
    
    #> curl http://172.17.0.2:6380/metrics/com.intrbiz.jvm/com.intrbiz.jvm.memory.heap.free.percentage
    [ {
    "type" : "parcel",
    "source" : "com.intrbiz.jvm",
    "captured" : 1512233845588,
    "readings" : [ {
        "type" : "double-gauge",
        "name" : "com.intrbiz.jvm.memory.heap.free.percentage",
        "value" : 98.5281959758814
    } ]
    } ]
    
    #> curl http://172.17.0.2:6380/metrics/com.intrbiz.jvm/
    ...
    
    #> curl http://172.17.0.2:6380/metrics/hcr/
    ...

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
