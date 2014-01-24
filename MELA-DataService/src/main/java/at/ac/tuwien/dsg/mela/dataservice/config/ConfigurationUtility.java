package at.ac.tuwien.dsg.mela.dataservice.config;

import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesConfiguration;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBContext;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by omoser on 1/24/14.
 */

@Component
public class ConfigurationUtility {

    static final Logger log = LoggerFactory.getLogger(ConfigurationUtility.class);

    @Autowired
    ApplicationContext context;

    @Value("${MELA_CONFIG_DIR")
    private String configDir;

    public InputStream getDefaultServiceStructureStream() throws IOException {
        return context.getResource("file://" + configDir + "/default/structure.xml").getInputStream();
    }

    public InputStream getDefaultMetricCompositionRulesStream() throws IOException {
        return context.getResource("file://" + configDir + "/default/compositionRules.xml").getInputStream();
    }

    public InputStream getDefaultRequirementsStream() throws IOException {
        return context.getResource("file://" + configDir + "/default/requirements.xml").getInputStream();
    }

    public ConfigurationXMLRepresentation createDefaultConfiguration() {
        ConfigurationXMLRepresentation configurationXMLRepresentation = new ConfigurationXMLRepresentation();
        CompositionRulesConfiguration compositionRulesConfiguration = new CompositionRulesConfiguration();
        Requirements requirements = new Requirements();
        // create service with 1 topology and 1 service unit having * (all) VMs

        MonitoredElement service = new MonitoredElement()
                .withId("Service")
                .withLevel(MonitoredElement.MonitoredElementLevel.SERVICE);

        MonitoredElement topology = new MonitoredElement()
                .withId("ServiceTopology")
                .withLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);

        service.addElement(topology);

        MonitoredElement serviceUnit = new MonitoredElement()
                .withLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)
                .withId("ServiceUnit");

        topology.addElement(serviceUnit);


        //retrieve the default config from files
        try {
            JAXBContext jAXBContext = JAXBContext.newInstance(MonitoredElement.class);
            InputStream fileStream = getDefaultServiceStructureStream();
            service = (MonitoredElement) jAXBContext.createUnmarshaller().unmarshal(fileStream);
        } catch (Exception ex) {
            log.error("Cannot unmarshall ServiceStructure: {}", ex.getMessage());
            // todo shouldn't we throw an exception in this case?
        }

        //retrieve the default config from files
        try {
            JAXBContext jAXBContext = JAXBContext.newInstance(CompositionRulesConfiguration.class);
            InputStream fileStream = getDefaultMetricCompositionRulesStream();
            compositionRulesConfiguration = (CompositionRulesConfiguration) jAXBContext.createUnmarshaller().unmarshal(fileStream);
        } catch (Exception ex) {
            log.error("Cannot unmarshall CompositionRulesConfiguration: {}", ex.getMessage());
            //todo exception?
        }

        //retrieve the default config from files
        try {
            JAXBContext jAXBContext = JAXBContext.newInstance(Requirements.class);
            InputStream fileStream = getDefaultRequirementsStream();
            requirements = (Requirements) jAXBContext.createUnmarshaller().unmarshal(fileStream);
        } catch (Exception ex) {
            log.error("Cannot unmarshall Requirements: {}", ex.getMessage());
        }

        configurationXMLRepresentation.setServiceConfiguration(service);
        configurationXMLRepresentation.setCompositionRulesConfiguration(compositionRulesConfiguration);
        configurationXMLRepresentation.setRequirements(requirements);

        return configurationXMLRepresentation;
    }
}
