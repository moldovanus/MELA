MELA
====
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
For now, adding MELA datasources is done by adding `<mela:ganglia-datasource>` elements in the context configuration file
of MELA-DataService (`mela-data-service-context.xml`). An example configuration for a local Ganglia installation would look
like the following:

```xml
<mela:ganglia-datasource id="ganglia-local" host="localhost" port="8649" polling-interval-ms="5000"/>
```
