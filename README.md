MELA-Fork
=========
Major changes in this fork include the use of Spring Framework as well as a dedicated namespace for configuring 
various MELA aspects (e.g. datasources)

General Information about Configuration
----------------------------------------
There are two important config files for each MELA-DataService (`mela-data-service.properties`) and 
MELA-AnalysisService (`mela-analysis-service.properties`).

Those config files are using the standard Java properties format and their content is injected into the Spring context. 

They are located in the `src/main/resources` folders in their related module base.

Datasource Configuration
-----------------------------------------
For now, adding MELA datasources is done by adding `<mela:ganglia-datasource>` elements in the application context configuration file of MELA-DataService (`mela-data-service-context.xml`). An example configuration for a local Ganglia installation would look like the following:

```xml
<mela:ganglia-datasource id="ganglia-local" 
                         host="localhost" port="8649" 
                         polling-interval-ms="5000"/>
```

Database Configuration (HSQLDB)
--------------------------------
The underlying HSQLDB is configured via the application context of MELA-DataService (`mela-data-service-context.xml`)

There are two modes of operation available, using an embedded (in-process) HSQL instance or starting a full-blown HSQLServer DB.

### Configuring MELA with Embedded HSQL

Uncomment the following in `mela-data-service-context.xml` (and uncomment the server configuration explained below):

```xml
<jdbc:embedded-database id="dataSource" type="HSQL"/>-->

<jdbc:initialize-database data-source="dataSource" enabled="${dataservice.operationmode.continuous}">
   <jdbc:script location="classpath:schema-continous.sql"/>
</jdbc:initialize-database>
```

### Configuring MELA with HSQL Server

Add/Uncomment the following parts in `mela-data-service-context.xml` (and comment the in-memory configuration part) for _both_ MELA-DataService and MELA-Analysis service (as both modules require direct database access)

```xml
<!-- this configures a standalone HSQLDB server instance that is started during application startup -->
<bean id="database" class="at.ac.tuwien.dsg.mela.dataservice.spring.HsqlServerBean" 
      lazy-init="false">
    <property name="serverProperties">
        <props>
            <prop key="server.port">9001</prop>
            <prop key="server.database.0">./db/mela</prop>
            <prop key="server.dbname.0">mela</prop>
        </props>
    </property>
</bean>

<!-- this datasource is injected and used by JdbcTemplate -->
<bean id="dataSource" class="org.springframework.jdbc.datasource.DriverManagerDataSource" 
      depends-on="database">
    <property name="driverClassName" value="org.hsqldb.jdbcDriver"/>
    <property name="url" value="jdbc:hsqldb:hsql://localhost:9001/mela"/>
    <property name="username" value="SA"/>
    <property name="password" value=""/>
</bean>
```

### Configuring AMQ and JMS related settings
There is only one important configuration parameter which is located in `mela-data-service.properties` as well as 
`mela-analysis-service.properties`:

```properties
dataservice.configuration.uri=tcp://localhost:9125
```

This controls the protocol, hostname and port that the broker and the JMS template will use. Other parameters, such
as the `destination` queue, can be controlled in the `<jms:listener>` configuration (which is setup in the 
application context, but those parameters rarely change.
