
# Pre requisites

* Java 8
* Postgresql with a database `jdbc:postgresql://localhost:5432/test` and a user `test/test`
    * Hibernate will create a bunch of tables in this `test` database. 
    * Configuration can be changed in the `application.properties` config files
* Elastic APM + Elasticsearch
   * login/password `elastic/elastic` enabled for filebeat
   * Elastic APM secret key `my_secret_token`
* `filebeat`
* Folder `/usr/local/var/log/my-shopping-cart/`

# Architecture

## Simplified Architecture

![](https://github.com/cyrille-leclerc/my-shopping-cart/raw/elastic/docs/images/demo-architecture-simplified.png)

## Detailed Architecture

![](https://github.com/cyrille-leclerc/my-shopping-cart/raw/elastic/docs/images/demo-architecture.png)

# Run the sample

* Install java: on Mac, see https://installvirtual.com/install-openjdk-10-mac-using-brew/

* Install Postgresql

```
brew install postgresql
brew services start postgresql
psql postgres
create database test;
CREATE USER test WITH PASSWORD 'test';

// TODO create role test
GRANT ALL PRIVILEGES ON DATABASE test TO test;

```

* shell 1: Anti Fraud service
 
```
 cd anti-fraud-java/
 ./run-anti-fraud.sh  
 ```

* shell 2: Frontend
 
```
 cd frontend-java/
 ./run-frontend.sh  
 ```

* shell 3: Monitor to inject load on the application
 ```
cd monitor-java
./run-monitor.sh  
```

* Shell 4: filebeat
 
```
 cd elastic-filebeat
 ./run-filebeat.sh  
 ```

For troubleshooting, edit `logging.level` in `filebeat/filebeat.yml`.
See sample below.

# Sample execution

![](https://github.com/cyrille-leclerc/my-shopping-cart/raw/elastic/docs/images/elastic-apm-distributed-trace-elastic.png)

![](https://github.com/cyrille-leclerc/my-shopping-cart/raw/elastic/docs/images/elastic-apm-distributed-trace-elastic-links.png)

```
e
2020-06-16T17:53:27.742+0200    INFO    instance/beat.go:621    Home path: [/usr/local/Cellar/filebeat-full/7.7.1/libexec] Config path: [.] Data path: [/usr/local/var/lib/filebeat] Logs path: [/usr/local/var/log/filebeat]
2020-06-16T17:53:27.742+0200    INFO    instance/beat.go:629    Beat ID: e1d83b8a-38df-4a55-be4c-9dc4dea879cd
2020-06-16T17:53:27.744+0200    INFO    [beat]  instance/beat.go:957    Beat info       {"system_info": {"beat": {"path": {"config": ".", "data": "/usr/local/var/lib/filebeat", "home": "/usr/local/Cellar/filebeat-full/7.7.1/libexec", "logs": "/usr/local/var/log/filebeat"}, "type": "filebeat", "uuid": "e1d83b8a-38df-4a55-be4c-9dc4dea879cd"}}}
2020-06-16T17:53:27.744+0200    INFO    [beat]  instance/beat.go:966    Build info      {"system_info": {"build": {"commit": "932b273e8940575e15f10390882be205bad29e1f", "libbeat": "7.7.1", "time": "2020-05-28T15:24:10.000Z", "version": "7.7.1"}}}
2020-06-16T17:53:27.744+0200    INFO    [beat]  instance/beat.go:969    Go runtime info {"system_info": {"go": {"os":"darwin","arch":"amd64","max_procs":8,"version":"go1.13.9"}}}
2020-06-16T17:53:27.744+0200    INFO    [beat]  instance/beat.go:973    Host info       {"system_info": {"host": {"architecture":"x86_64","boot_time":"2020-06-16T15:32:58.300025+02:00","name":"my-laptop","ip":["127.0.0.1/8","::1/128","fe80::1/64","fe80::aede:48ff:fe00:1122/64","fe80::30:f0c:edef:aba6/64","192.168.3.46/24","fe80::64ca:3ff:fe68:5d7/64","fe80::64ca:3ff:fe68:5d7/64","fe80::bab3:7987:a623:85bc/64","fe80::e3f7:a61a:96c9:8ae8/64"],"kernel_version":"19.5.0","mac":["ac:de:48:00:11:22","fa:ff:c2:4e:d1:b1","f8:ff:c2:4e:d1:b1","82:bf:e9:40:48:01","82:bf:e9:40:48:00","82:bf:e9:40:48:05","82:bf:e9:40:48:04","82:bf:e9:40:48:01","0a:ff:c2:4e:d1:b1","66:ca:03:68:05:d7","66:ca:03:68:05:d7"],"os":{"family":"darwin","platform":"darwin","name":"Mac OS X","version":"10.15.5","major":10,"minor":15,"patch":5,"build":"19F101"},"timezone":"CEST","timezone_offset_sec":7200,"id":"04A12D9F-C409-5352-B238-99EA58CAC285"}}}
2020-06-16T17:53:27.744+0200    INFO    [beat]  instance/beat.go:1002   Process info    {"system_info": {"process": {"cwd": "/path/to/my-shopping-cart/filebeat", "exe": "/usr/local/Cellar/filebeat-full/7.7.1/libexec/bin/filebeat", "name": "filebeat", "pid": 12109, "ppid": 12107, "start_time": "2020-06-16T17:53:27.689+0200"}}}
2020-06-16T17:53:27.745+0200    INFO    instance/beat.go:297    Setup Beat: filebeat; Version: 7.7.1
2020-06-16T17:53:27.745+0200    INFO    [index-management]      idxmgmt/std.go:182      Set output.elasticsearch.index to 'filebeat-7.7.1' as ILM is enabled.
2020-06-16T17:53:27.745+0200    INFO    eslegclient/connection.go:84    elasticsearch url: http://localhost:9200
2020-06-16T17:53:27.745+0200    INFO    [publisher]     pipeline/module.go:110  Beat name: my-laptop
2020-06-16T17:53:27.748+0200    INFO    [monitoring]    log/log.go:118  Starting metrics logging every 30s
2020-06-16T17:53:27.748+0200    INFO    instance/beat.go:438    filebeat start running.
2020-06-16T17:53:27.748+0200    INFO    registrar/registrar.go:145      Loading registrar data from /usr/local/var/lib/filebeat/registry/filebeat/data.json
2020-06-16T17:53:27.749+0200    INFO    registrar/registrar.go:152      States Loaded from registrar: 7
2020-06-16T17:53:27.749+0200    INFO    beater/crawler.go:73    Loading Inputs: 2
2020-06-16T17:53:27.749+0200    INFO    log/input.go:152        Configured paths: [/usr/local/var/log/my-shopping-cart/anti-fraud.log]
2020-06-16T17:53:27.749+0200    INFO    input/input.go:114      Starting input of type: log; ID: 1264852361483349187 
2020-06-16T17:53:27.750+0200    INFO    log/input.go:152        Configured paths: [/usr/local/var/log/my-shopping-cart/frontend.log]
2020-06-16T17:53:27.750+0200    INFO    input/input.go:114      Starting input of type: log; ID: 655581495753941733 
2020-06-16T17:53:27.750+0200    INFO    beater/crawler.go:105   Loading and starting Inputs completed. Enabled inputs: 2
2020-06-16T17:53:30.746+0200    INFO    [add_cloud_metadata]    add_cloud_metadata/add_cloud_metadata.go:89     add_cloud_metadata: hosting provider type not detected.
2020-06-16T17:53:57.754+0200    INFO    [monitoring]    log/log.go:145  Non-zero metrics in the last 30s        {"monitoring": {"metrics": {"beat":{"cpu":{"system":{"ticks":32,"time":{"ms":32}},"total":{"ticks":74,"time":{"ms":75},"value":74},"user":{"ticks":42,"time":{"ms":43}}},"info":{"ephemeral_id":"503f30b4-0579-474d-9ce3-618429365f3e","uptime":{"ms":30035}},"memstats":{"gc_next":8506928,"memory_alloc":6915264,"memory_total":14682528,"rss":32731136},"runtime":{"goroutines":27}},"filebeat":{"events":{"added":2,"done":2},"harvester":{"open_files":0,"running":0}},"libbeat":{"config":{"module":{"running":0}},"output":{"type":"elasticsearch"},"pipeline":{"clients":2,"events":{"active":0,"filtered":2,"total":2}}},"registrar":{"states":{"current":7,"update":2},"writes":{"success":2,"total":2}},"system":{"cpu":{"cores":8},"load":{"1":4.1157,"15":4.3301,"5":4.7612,"norm":{"1":0.5145,"15":0.5413,"5":0.5952}}}}}}
```
