/*
﻿Developed with the contribution of the European Commission - Directorate General for Maritime Affairs and Fisheries
© European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can
redistribute it and/or modify it under the terms of the GNU General Public License as published by the
Free Software Foundation, either version 3 of the License, or any later version. The IFDM Suite is distributed in
the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details. You should have received a
copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */
package fish.focus.uvms.plugins.iridium;

import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timer;
import javax.inject.Inject;
import javax.jms.JMSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fish.focus.schema.exchange.plugin.types.v1.PluginType;
import fish.focus.schema.exchange.registry.v1.ExchangeRegistryMethod;
import fish.focus.schema.exchange.service.v1.CapabilityListType;
import fish.focus.schema.exchange.service.v1.ServiceType;
import fish.focus.schema.exchange.service.v1.SettingListType;
import fish.focus.uvms.exchange.model.constant.ExchangeModelConstants;
import fish.focus.uvms.exchange.model.mapper.ExchangeModuleRequestMapper;
import fish.focus.uvms.plugins.iridium.mapper.ServiceMapper;
import fish.focus.uvms.plugins.iridium.producer.PluginMessageProducer;
import fish.focus.uvms.plugins.iridium.service.FileHandlerBean;

@Singleton
@Startup
public class StartupBean extends PluginDataHolder {

    private static final Logger LOG = LoggerFactory.getLogger(StartupBean.class);

    private static final int MAX_NUMBER_OF_TRIES = 20;
    private boolean isRegistered = false;
    private boolean isEnabled = false;
    private boolean waitingForResponse = false;
    private int numberOfTriesExecuted = 0;

    private String REGISTER_CLASS_NAME = "";
    private String APPLICATION_NAME = "";
    private String RESPONSE_TOPIC_NAME = "";

    @Inject
    PluginMessageProducer messageProducer;

    @Inject
    FileHandlerBean fileHandler;

    private CapabilityListType capabilities;
    private SettingListType settingList;
    private ServiceType serviceType;

    @PostConstruct
    public void startup() {

        //This must be loaded first!!! Not doing that will end in dire problems later on!
        super.setPluginApplicaitonProperties(fileHandler.getPropertiesFromFile(PluginDataHolder.PLUGIN_PROPERTIES));
        REGISTER_CLASS_NAME = getPLuginApplicationProperty("application.groupid");
        APPLICATION_NAME = getPLuginApplicationProperty("application.name");
        RESPONSE_TOPIC_NAME = getPLuginApplicationProperty("application.responseTopicName");

        //These can be loaded in any order
        super.setPluginProperties(fileHandler.getPropertiesFromFile(PluginDataHolder.PROPERTIES));
        super.setPluginCapabilities(fileHandler.getPropertiesFromFile(PluginDataHolder.CAPABILITIES));

        ServiceMapper.mapToMapFromProperties(super.getSettings(), super.getPluginProperties(), getRegisterClassName() + "." + getApplicationName());
        ServiceMapper.mapToMapFromProperties(super.getCapabilities(), super.getPluginCapabilities(), null);

        capabilities = ServiceMapper.getCapabilitiesListTypeFromMap(super.getCapabilities());
        settingList = ServiceMapper.getSettingsListTypeFromMap(super.getSettings());

        serviceType = ServiceMapper.getServiceType(
                getRegisterClassName() + "." + getApplicationName(),
                getApplicationName(),
                "SiriusOne plugin for Polaris data reports",
                PluginType.SATELLITE_RECEIVER,
                getPluginResponseSubscriptionName(),
                "IRIDIUM");

        register();

        LOG.debug("Settings updated in plugin {}", REGISTER_CLASS_NAME);
        for (Map.Entry<String, String> entry : super.getSettings().entrySet()) {
            LOG.debug("Setting: KEY: {} , VALUE: {}", entry.getKey(), entry.getValue());
        }

        LOG.info("PLUGIN STARTED");
    }

    @PreDestroy
    public void shutdown() {
        unregister();
    }

    @Schedule(second = "*/10", minute = "*", hour = "*", persistent = false)
    public void timeout(Timer timer) {
        if (!isRegistered && numberOfTriesExecuted < MAX_NUMBER_OF_TRIES) {
            LOG.info(getRegisterClassName() + " is not registered, trying to register");
            register();
            numberOfTriesExecuted++;
        }
        if (isRegistered) {
            LOG.info(getRegisterClassName() + " is registered. Cancelling timer.");
            timer.cancel();
        } else if(numberOfTriesExecuted >= MAX_NUMBER_OF_TRIES) {
            LOG.info(getRegisterClassName() + " failed to register, maximum number of retries reached.");
        }
    }

    private void register() {
        LOG.info("Registering to Exchange Module");
        setWaitingForResponse(true);
        try {
            String registerServiceRequest = ExchangeModuleRequestMapper.createRegisterServiceRequest(serviceType, capabilities, settingList);
            messageProducer.sendEventBusMessage(registerServiceRequest, ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE, ExchangeRegistryMethod.REGISTER_SERVICE.value());
        } catch (JMSException | RuntimeException e) {
            LOG.error("Failed to send registration message to {}", ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE);
            setWaitingForResponse(false);
        }

    }

    private void unregister() {
        LOG.info("Unregistering from Exchange Module");
        try {
            String unregisterServiceRequest = ExchangeModuleRequestMapper.createUnregisterServiceRequest(serviceType);
            messageProducer.sendEventBusMessage(unregisterServiceRequest, ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE, ExchangeRegistryMethod.UNREGISTER_SERVICE.value());
        } catch (JMSException | RuntimeException e) {
            LOG.error("Failed to send unregistration message to {}", ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE);
        }
    }

    public String getPLuginApplicationProperty(String key) {
        try {
            return (String) super.getPluginApplicaitonProperties().get(key);
        } catch (Exception e) {
            LOG.error("Failed to getSetting for key: " + key, getRegisterClassName());
            return null;
        }
    }

    public String getPluginResponseSubscriptionName() {
        return getRegisterClassName() + "." + getApplicationName() + RESPONSE_TOPIC_NAME;
    }

    public String getResponseTopicMessageName() {
        return getSetting("application.groupid");
    }

    public String getRegisterClassName() {
        return REGISTER_CLASS_NAME;
    }

    public String getApplicationName() {
        return APPLICATION_NAME;
    }

    public String getSetting(String key) {
        try {
            LOG.debug("Trying to get setting {} ", REGISTER_CLASS_NAME + "." + APPLICATION_NAME + "." + key);
            return super.getSettings().get(REGISTER_CLASS_NAME + "." + APPLICATION_NAME + "." + key);
        } catch (Exception e) {
            LOG.error("Failed to getSetting for key: " + key, REGISTER_CLASS_NAME + "." + APPLICATION_NAME);
            return null;
        }
    }

    public boolean isWaitingForResponse() {
        return waitingForResponse;
    }

    public void setWaitingForResponse(boolean waitingForResponse) {
        this.waitingForResponse = waitingForResponse;
    }

    public boolean isIsRegistered() {
        return isRegistered;
    }

    public void setIsRegistered(boolean isRegistered) {
        this.isRegistered = isRegistered;
    }

    public boolean isIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

}