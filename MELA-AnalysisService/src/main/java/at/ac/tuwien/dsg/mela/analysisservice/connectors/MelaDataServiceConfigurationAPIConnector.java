package at.ac.tuwien.dsg.mela.analysisservice.connectors;

import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesConfiguration;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.elasticity.ActionXML;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.requirements.Requirement;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import at.ac.tuwien.dsg.mela.dataservice.api.CommandConsumer;
import at.ac.tuwien.dsg.mela.dataservice.config.ConfigurationXMLRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.Session;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.StringWriter;

@Component
public class MelaDataServiceConfigurationAPIConnector {

    static final Logger log = LoggerFactory.getLogger(MelaDataServiceConfigurationAPIConnector.class);

    @Autowired
    private JmsTemplate jmsTemplate;

    public void sendConfiguration(ConfigurationXMLRepresentation configurationXMLRepresentation) {
        try {
            sendMessage(CommandConsumer.SUBMIT_CONFIGURATION_COMMAND, marshal(configurationXMLRepresentation, ConfigurationXMLRepresentation.class));
        } catch (JAXBException ex) {
            log.error("Unable to marshall object of class " + configurationXMLRepresentation.getClass() + " into String", ex);
        }
    }


    public void sendCompositionRules(CompositionRulesConfiguration compositionRulesConfiguration) {
        try {
            sendMessage(CommandConsumer.SUBMIT_COMPOSITION_RULES, marshal(compositionRulesConfiguration, CompositionRulesConfiguration.class));
        } catch (JAXBException ex) {
            log.error("Unable to marshall object of class " + compositionRulesConfiguration.getClass() + " into String", ex);
        }
    }

    public void sendRequirements(Requirements requirements) {
        try {
            sendMessage(CommandConsumer.SUBMIT_REQUIREMENTS, marshal(requirements, Requirement.class));
        } catch (JAXBException ex) {
            log.error("Unable to marshall object of class " + requirements.getClass() + " into String", ex);
        }
    }

    public void sendUpdatedServiceStructure(MonitoredElement serviceConfiguration) {
        try {
            sendMessage(CommandConsumer.UPDATE_SERVICE_STRUCTURE, marshal(serviceConfiguration, MonitoredElement.class));
        } catch (JAXBException ex) {
            log.error("Unable to marshall object of class " + serviceConfiguration.getClass() + " into String", ex);
        }

    }

    public void addExecutingAction(String targetEntityID, String actionName) {
        try {
            sendMessage(CommandConsumer.ADD_EXECUTING_ACTION, marshalActionElement(targetEntityID, actionName));
        } catch (JAXBException ex) {
            log.error("Unable to marshall object of class " + ActionXML.class + " into String", ex);
        }

    }

    public void removeExecutingAction(String targetEntityID, String actionName) {
        try {
            sendMessage(CommandConsumer.REMOVE_EXECUTING_ACTION, marshalActionElement(targetEntityID, actionName));
        } catch (JAXBException ex) {
            log.error("Unable to marshall object of class " + ActionXML.class + " into String", ex);
        }
    }

    public void sendServiceStructure(MonitoredElement serviceConfiguration) {
        try {
            sendMessage(CommandConsumer.SET_SERVICE_STRUCTURE, marshal(serviceConfiguration, MonitoredElement.class));
        } catch (JAXBException ex) {
            log.error("Unable to marshall object of class " + serviceConfiguration.getClass() + " into String", ex);
        }
    }

    private <T> String marshal(Object source, Class<T> configurationClass) throws JAXBException {
        JAXBContext jAXBContext = JAXBContext.newInstance(configurationClass);
        StringWriter writer = new StringWriter();
        jAXBContext.createMarshaller().marshal(source, writer);
        return writer.toString();
    }


    private String marshalActionElement(String targetEntityID, String actionName) throws JAXBException {
        ActionXML action = new ActionXML();
        MonitoredElement element = new MonitoredElement(targetEntityID);
        action.setElement(element);
        action.addAction(actionName);
        return marshal(action, ActionXML.class);
    }

    private void sendMessage(final String key, final String value) {
        jmsTemplate.send(new MessageCreator() {
            public Message createMessage(Session session) throws JMSException {
                MapMessage message = session.createMapMessage();
                message.setObject(key, value);
                log.info("Sending message (key/value): {}/{}", key, value);
                return message;
            }
        });
    }

}
