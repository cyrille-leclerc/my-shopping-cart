
# Pre requisites

* Java 8
* Postgresql with a database `jdbc:postgresql://localhost:5432/test` and a user `test/test`
    * Hibernate will create a bunch of tables in this `test` database. 
    * Configuration can be changed in the `application.properties` config files
* Elastic APM

# Architecture

![](https://github.com/cyrille-leclerc/my-shopping-cart/raw/elastic/docs/images/demo-architecture.png)

# Run the sample

* Install java: on Mac, see https://installvirtual.com/install-openjdk-10-mac-using-brew/

* Install Postgresql

```
brew install postgresql
brew services start postgresql
psql postgre
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
 cd fronten-java/
 ./run-frontend.sh  
 ```

* shell 3: Monitor to inject load on the application
 ```
cd monitor-java
./run-monitor.sh  
```


# Sample execution

![](https://github.com/cyrille-leclerc/my-shopping-cart/raw/elastic/docs/images/elastic-apm-distributed-trace-elastic.png)
