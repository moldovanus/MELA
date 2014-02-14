/**
 * Copyright 2013 Technische Universitat Wien (TUW), Distributed Systems Group
 * E184
 *
 * This work was partially supported by the European Commission in terms of the
 * CELAR FP7 project (FP7-ICT-2011-8 \#317790)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package at.ac.tuwien.dsg.mela.analysisservice.control;

import at.ac.tuwien.dsg.mela.analysisservice.connectors.MelaDataServiceConfigurationAPIConnector;
import at.ac.tuwien.dsg.mela.analysisservice.util.converters.JsonConverter;
import at.ac.tuwien.dsg.mela.analysisservice.util.converters.XmlConverter;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesConfiguration;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityPathway.LightweightEncounterRateElasticityPathway;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityPathway.som.Neuron;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElSpaceDefaultFunction;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticitySpace;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticitySpaceFunction;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.engines.InstantMonitoringDataAnalysisEngine;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.report.AnalysisReport;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.elasticity.ElasticityPathwayXML;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.elasticity.ElasticitySpaceXML;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.*;
import at.ac.tuwien.dsg.mela.common.requirements.Requirement;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import at.ac.tuwien.dsg.mela.dataservice.config.ConfigurationXMLRepresentation;
import at.ac.tuwien.dsg.mela.dataservice.persistence.PersistenceSQLAccess;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at *
 * <p/>
 * Delegates the functionality of configuring MELA for instant monitoring and
 * analysis
 */
@Service
public class ElasticityAnalysisManager {

    @Value("${analysisservice.elasticityanalysis:true}")
    private boolean elasticityAnalysisEnabled;

    @Autowired
    private MelaDataServiceConfigurationAPIConnector melaApi;

    private Requirements requirements;

    private CompositionRulesConfiguration compositionRulesConfiguration;

    private MonitoredElement serviceConfiguration;

    @Autowired
    private InstantMonitoringDataAnalysisEngine instantMonitoringDataAnalysisEngine;

    @Autowired
    private PersistenceSQLAccess persistenceSQLAccess;

    @Autowired
    private JsonConverter jsonConverter;

    @Autowired
    private XmlConverter xmlConverter;

    @PostConstruct
    public void init() {
        instantMonitoringDataAnalysisEngine = new InstantMonitoringDataAnalysisEngine();

        // get latest config
        ConfigurationXMLRepresentation configurationXMLRepresentation = persistenceSQLAccess.getLatestConfiguration();
        persistenceSQLAccess.setMonitoringId(configurationXMLRepresentation.getServiceConfiguration().getId());

        // open proper sql access
        //persistenceSQLAccess = new PersistenceSQLAccess(configurationXMLRepresentation.getServiceConfiguration().getId());
        setInitialServiceConfiguration(configurationXMLRepresentation.getServiceConfiguration());
        setInitialCompositionRulesConfiguration(configurationXMLRepresentation.getCompositionRulesConfiguration());
        setInitialRequirements(configurationXMLRepresentation.getRequirements());
    }

    protected ElasticityAnalysisManager() {
    }


    public synchronized void addExecutingAction(String targetEntityID, String actionName) {
        melaApi.addExecutingAction(targetEntityID, actionName);
    }

    public synchronized void removeExecutingAction(String targetEntityID, String actionName) {
        melaApi.removeExecutingAction(targetEntityID, actionName);
    }

    public synchronized void setServiceConfiguration(MonitoredElement serviceConfiguration) {
        this.serviceConfiguration = serviceConfiguration;
        persistenceSQLAccess.refresh(); // = new PersistenceSQLAccess("mela", "mela", Configuration.getDataServiceIP(), Configuration.getDataServicePort(), serviceConfiguration.getId());
        melaApi.sendServiceStructure(serviceConfiguration);
    }

    public synchronized MonitoredElement getServiceConfiguration() {
        return serviceConfiguration;
    }

    private synchronized void setInitialServiceConfiguration(MonitoredElement serviceConfiguration) {
        this.serviceConfiguration = serviceConfiguration;
    }

    // actually removes all VMs and Virtual Clusters from the ServiceUnit and
    // adds new ones.
    public synchronized void updateServiceConfiguration(MonitoredElement serviceConfiguration) {
        // extract all ServiceUnit level monitored elements from both services,
        // and replace their children
        Map<MonitoredElement, MonitoredElement> serviceUnits = new HashMap<MonitoredElement, MonitoredElement>();
        for (MonitoredElement element : this.serviceConfiguration) {
            if (element.getLevel().equals(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)) {
                // remove element's children
                element.getContainedElements().clear();
                serviceUnits.put(element, element);
            }
        }

        // go trough the new service, and for each Service Unit, add its
        // children (containing both Virtual Machines and Virtual Clusters) to
        // the original service
        for (MonitoredElement element : serviceConfiguration) {
            if (serviceUnits.containsKey(element)) {
                // bad practice. breaks incapsulation
                serviceUnits.get(element).getContainedElements().addAll(element.getContainedElements());
            }
        }

        melaApi.sendUpdatedServiceStructure(this.serviceConfiguration);

    }

    public synchronized Requirements getRequirements() {
        return requirements;
    }

    public synchronized void setRequirements(Requirements requirements) {
        this.requirements = requirements;
        melaApi.sendRequirements(requirements);

    }

    private synchronized void setInitialRequirements(Requirements requirements) {
        this.requirements = requirements;
        // elasticitySpaceFunction.setRequirements(requirements);
        //
    }

    public synchronized CompositionRulesConfiguration getCompositionRulesConfiguration() {
        return compositionRulesConfiguration;
    }

    public synchronized void setCompositionRulesConfiguration(CompositionRulesConfiguration compositionRulesConfiguration) {
        this.compositionRulesConfiguration = compositionRulesConfiguration;
        melaApi.sendCompositionRules(compositionRulesConfiguration);
    }

    private synchronized void setInitialCompositionRulesConfiguration(CompositionRulesConfiguration compositionRulesConfiguration) {
        this.compositionRulesConfiguration = compositionRulesConfiguration;
    }

    public synchronized AnalysisReport analyzeLatestMonitoringData() {
        return instantMonitoringDataAnalysisEngine.analyzeRequirements(persistenceSQLAccess.extractLatestMonitoringData(), requirements);
    }

    public synchronized Collection<Metric> getAvailableMetricsForMonitoredElement(MonitoredElement monitoredElement) {
        return persistenceSQLAccess.getAvailableMetrics(monitoredElement);
    }

    public synchronized MonitoredElementMonitoringSnapshot getLatestMonitoringData() {
        ServiceMonitoringSnapshot monitoringSnapshot = persistenceSQLAccess.extractLatestMonitoringData();
        if (monitoringSnapshot != null && !monitoringSnapshot.getMonitoredData().isEmpty()) {
            return monitoringSnapshot.getMonitoredData(MonitoredElement.MonitoredElementLevel.SERVICE).values().iterator().next();
        } else {
            return new MonitoredElementMonitoringSnapshot();
        }
    }

    public synchronized MonitoredElementMonitoringSnapshot getLatestMonitoringData(MonitoredElement element) {
        ServiceMonitoringSnapshot monitoringSnapshot = persistenceSQLAccess.extractLatestMonitoringData();
        if (monitoringSnapshot != null && !monitoringSnapshot.getMonitoredData().isEmpty()) {
            //keep data only for element
            MonitoredElementMonitoringSnapshot data = monitoringSnapshot.getMonitoredData(element);
            data.setChildren(null);
            return data;
        } else {
            return new MonitoredElementMonitoringSnapshot();
        }
    }

    public synchronized List<MonitoredElementMonitoringSnapshot> getAllAggregatedMonitoringData() {
        List<MonitoredElementMonitoringSnapshot> elementMonitoringSnapshots = new ArrayList<MonitoredElementMonitoringSnapshot>();
        for (ServiceMonitoringSnapshot monitoringSnapshot : persistenceSQLAccess.extractMonitoringData()) {
            elementMonitoringSnapshots.add(monitoringSnapshot.getMonitoredData(MonitoredElement.MonitoredElementLevel.SERVICE).values().iterator().next());
        }
        return elementMonitoringSnapshots;
    }


    // uses a lot of memory (all directly in memory)
    public synchronized String getElasticityPathway(MonitoredElement element) {

        // if no service configuration, we can't have elasticity space function
        // if no compositionRulesConfiguration we have no data
        if (!elasticityAnalysisEnabled || serviceConfiguration == null && compositionRulesConfiguration != null) {
            Logger.getLogger(this.getClass()).log(Level.WARN, "Elasticity analysis disabled, or no service configuration or composition rules configuration");
            JSONObject elSpaceJSON = new JSONObject();
            elSpaceJSON.put("name", "ElPathway");
            return elSpaceJSON.toJSONString();
        }

        Date before = new Date();

        // int recordsCount = persistenceSQLAccess.getRecordsCount();

        // first, read from the sql of monitoring data, in increments of 10, and
        // train the elasticity space function
        LightweightEncounterRateElasticityPathway elasticityPathway = null;

        List<Metric> metrics = null;

        ElasticitySpace space = persistenceSQLAccess.extractLatestElasticitySpace();

        if (space == null) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, "Elasticity Space returned is null");
            JSONObject elSpaceJSON = new JSONObject();
            elSpaceJSON.put("name", "ElPathway");
            return elSpaceJSON.toJSONString();
        }

        Map<Metric, List<MetricValue>> map = space.getMonitoredDataForService(element);
        if (map != null) {
            metrics = new ArrayList<Metric>(map.keySet());
            // we need to know the number of weights to add in instantiation
            elasticityPathway = new LightweightEncounterRateElasticityPathway(metrics.size());
        } else {
            Logger.getLogger(this.getClass()).log(Level.ERROR, "Elasticity Space not found for " + element.getId());
            JSONObject elSpaceJSON = new JSONObject();
            elSpaceJSON.put("name", "ElPathway");
            return elSpaceJSON.toJSONString();
        }

        elasticityPathway.trainElasticityPathway(map);

        List<Neuron> neurons = elasticityPathway.getSituationGroups();
        if (metrics == null) {
            Logger.getLogger(this.getClass()).log(Level.ERROR,
                    "Service Element " + element.getId() + " at level " + element.getLevel() + " was not found in service structure");
            JSONObject elSpaceJSON = new JSONObject();
            elSpaceJSON.put("name", "Service not found");
            return elSpaceJSON.toJSONString();
        } else {
            String converted = jsonConverter.convertElasticityPathway(metrics, neurons);
            Date after = new Date();
            Logger.getLogger(this.getClass()).log(Level.DEBUG, "El Pathway cpt time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
            return converted;
        }

    }

    public synchronized ElasticityPathwayXML getElasticityPathwayInXML(MonitoredElement element) {

        ElasticityPathwayXML elasticityPathwayXML = new ElasticityPathwayXML();

        // if no service configuration, we can't have elasticity space function
        // if no compositionRulesConfiguration we have no data
        if (!elasticityAnalysisEnabled || serviceConfiguration == null && compositionRulesConfiguration != null) {
            Logger.getLogger(this.getClass()).log(Level.WARN, "Elasticity analysis disabled, or no service configuration or composition rules configuration");
            return elasticityPathwayXML;
        }

        Date before = new Date();

        // int recordsCount = persistenceSQLAccess.getRecordsCount();

        // first, read from the sql of monitoring data, in increments of 10, and
        // train the elasticity space function
        LightweightEncounterRateElasticityPathway elasticityPathway = null;

        List<Metric> metrics = null;

        ElasticitySpace space = persistenceSQLAccess.extractLatestElasticitySpace();

        Map<Metric, List<MetricValue>> map = space.getMonitoredDataForService(element);
        if (map != null) {
            metrics = new ArrayList<Metric>(map.keySet());
            // we need to know the number of weights to add in instantiation
            elasticityPathway = new LightweightEncounterRateElasticityPathway(metrics.size());
        }

        elasticityPathway.trainElasticityPathway(map);

        List<Neuron> neurons = elasticityPathway.getSituationGroups();
        if (metrics == null) {
            Logger.getLogger(this.getClass()).log(Level.ERROR,
                    "Service Element " + element.getId() + " at level " + element.getLevel() + " was not found in service structure");

            return elasticityPathwayXML;
        } else {
            elasticityPathwayXML = xmlConverter.convertElasticityPathwayToXML(metrics, neurons, element);
            Date after = new Date();
            Logger.getLogger(this.getClass()).log(Level.DEBUG, "El Pathway cpt time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
            return elasticityPathwayXML;
        }

    }

    public synchronized String getElasticitySpaceJSON(MonitoredElement element) {

        // if no service configuration, we can't have elasticity space function
        // if no compositionRulesConfiguration we have no data
        if (!elasticityAnalysisEnabled || serviceConfiguration == null && compositionRulesConfiguration != null) {
            Logger.getLogger(this.getClass()).log(Level.WARN, "Elasticity analysis disabled, or no service configuration or composition rules configuration");
            JSONObject elSpaceJSON = new JSONObject();
            elSpaceJSON.put("name", "ElSpace");
            return elSpaceJSON.toJSONString();
        }

        Date before = new Date();

        ElasticitySpace space = extractAndUpdateElasticitySpace();


        String jsonRepr = jsonConverter.convertElasticitySpace(space, element);

        Date after = new Date();
        Logger.getLogger(this.getClass()).log(Level.DEBUG, "El Space cpt time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
        return jsonRepr;
    }

    /**
     * @param element
     * @return also contains the monitored values
     */
    public synchronized ElasticitySpaceXML getCompleteElasticitySpaceXML(MonitoredElement element) {

        Date before = new Date();

        ElasticitySpace space = persistenceSQLAccess.extractLatestElasticitySpace();

        ElasticitySpaceXML elasticitySpaceXML = xmlConverter.convertElasticitySpaceToXMLCompletely(space, element);

        Date after = new Date();
        Logger.getLogger(this.getClass()).log(Level.DEBUG, "El Space cpt time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
        return elasticitySpaceXML;
    }

    /**
     * @param element
     * @return contains only the Metric and their ElasticityBoundaries
     */
    public synchronized ElasticitySpaceXML getElasticitySpaceXML(MonitoredElement element) {

        Date before = new Date();

        ElasticitySpace space = persistenceSQLAccess.extractLatestElasticitySpace();
        ElasticitySpaceXML elasticitySpaceXML = xmlConverter.convertElasticitySpaceToXML(space, element);

        Date after = new Date();
        Logger.getLogger(this.getClass()).log(Level.DEBUG, "El Space cpt time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
        return elasticitySpaceXML;
    }

    public synchronized String getLatestMonitoringDataINJSON() {
        Date before = new Date();
        ServiceMonitoringSnapshot serviceMonitoringSnapshot = persistenceSQLAccess.extractLatestMonitoringData();
        Map<Requirement, Map<MonitoredElement, Boolean>> reqAnalysisResult = instantMonitoringDataAnalysisEngine.analyzeRequirements(serviceMonitoringSnapshot,
                requirements).getRequirementsAnalysisResult();
        String converted = jsonConverter.convertMonitoringSnapshot(serviceMonitoringSnapshot, requirements);
        Date after = new Date();
        Logger.getLogger(this.getClass()).log(Level.DEBUG, "Get Mon Data time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
        return converted;
    }

    public synchronized MonitoredElement getLatestServiceStructure() {
        Date before = new Date();
        ServiceMonitoringSnapshot serviceMonitoringSnapshot = persistenceSQLAccess.extractLatestMonitoringData();

        Date after = new Date();
        Logger.getLogger(this.getClass()).log(Level.DEBUG, "Get Mon Data time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
        return serviceMonitoringSnapshot.getMonitoredService();
    }

    public synchronized String getMetricCompositionRules() {
        if (compositionRulesConfiguration != null) {
            return jsonConverter.convertToJSON(compositionRulesConfiguration.getMetricCompositionRules());
        } else {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", "No composition rules yet");
            return jsonObject.toJSONString();
        }
    }

    private ElasticitySpace extractAndUpdateElasticitySpace() {
        ElasticitySpace space = persistenceSQLAccess.extractLatestElasticitySpace();

        //elasticity space is cached on a per-need basis

        if (space == null) {
            //if space is null, compute it from all aggregated monitored data recorded so far
            List<ServiceMonitoringSnapshot> dataFromTimestamp = persistenceSQLAccess.extractMonitoringData();

            ElasticitySpaceFunction fct = new ElSpaceDefaultFunction(serviceConfiguration);
            fct.setRequirements(requirements);
            fct.trainElasticitySpace(dataFromTimestamp);
            space = fct.getElasticitySpace();

            //set to the new space the timespaceID of the last snapshot monitored data used to compute it
            space.setTimestampID(dataFromTimestamp.get(dataFromTimestamp.size() - 1).getTimestampID());
        } else {

            //if space is not null, update it with new data
            List<ServiceMonitoringSnapshot> dataFromTimestamp = persistenceSQLAccess.extractMonitoringData(space.getTimestampID());
            //check if new data has been collected between elasticity space querries
            if (!dataFromTimestamp.isEmpty()) {
                ElasticitySpaceFunction fct = new ElSpaceDefaultFunction();
                fct.trainElasticitySpace(space, dataFromTimestamp, requirements);
                //set to the new space the timespaceID of the last snapshot monitored data used to compute it
                space.setTimestampID(dataFromTimestamp.get(dataFromTimestamp.size() - 1).getTimestampID());
            }
        }

        persistenceSQLAccess.writeElasticitySpace(space);
        return space;
    }
}
