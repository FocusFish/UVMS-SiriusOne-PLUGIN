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
package fish.focus.uvms.plugins.iridium.consumer;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fish.focus.schema.exchange.common.v1.AcknowledgeType;
import fish.focus.schema.exchange.common.v1.AcknowledgeTypeType;
import fish.focus.schema.exchange.plugin.v1.PingRequest;
import fish.focus.schema.exchange.plugin.v1.PluginBaseRequest;
import fish.focus.schema.exchange.plugin.v1.SetCommandRequest;
import fish.focus.schema.exchange.plugin.v1.SetConfigRequest;
import fish.focus.schema.exchange.plugin.v1.SetReportRequest;
import fish.focus.schema.exchange.plugin.v1.StartRequest;
import fish.focus.schema.exchange.plugin.v1.StopRequest;
import fish.focus.uvms.exchange.model.mapper.ExchangePluginResponseMapper;
import fish.focus.uvms.exchange.model.mapper.JAXBMarshaller;
import fish.focus.uvms.plugins.iridium.StartupBean;
import fish.focus.uvms.plugins.iridium.producer.PluginMessageProducer;
import fish.focus.uvms.plugins.iridium.service.PluginService;

public class PluginNameEventBusListener implements MessageListener {

    private static final Logger LOG = LoggerFactory.getLogger(PluginNameEventBusListener.class);

    @Inject
    PluginService service;

    @Inject
    PluginMessageProducer messageProducer;

    @Inject
    StartupBean startup;

    @Override
    public void onMessage(Message inMessage) {

        LOG.debug("Eventbus listener for siriusone (MessageConstants.PLUGIN_SERVICE_CLASS_NAME): {}", startup.getRegisterClassName());

        TextMessage textMessage = (TextMessage) inMessage;

        try {

            PluginBaseRequest request = JAXBMarshaller.unmarshallTextMessage(textMessage, PluginBaseRequest.class);
            String responseMessage = null;

            switch (request.getMethod()) {
                case SET_CONFIG:
                    SetConfigRequest setConfigRequest = JAXBMarshaller.unmarshallTextMessage(textMessage, SetConfigRequest.class);
                    AcknowledgeTypeType setConfig = service.setConfig(setConfigRequest.getConfigurations());
                    AcknowledgeType setConfigAck = ExchangePluginResponseMapper.mapToAcknowledgeType(textMessage.getJMSMessageID(), setConfig);
                    responseMessage = ExchangePluginResponseMapper.mapToSetConfigResponse(startup.getRegisterClassName() + "." + startup.getApplicationName(), setConfigAck);
                    break;
                case SET_COMMAND:
                    SetCommandRequest setCommandRequest = JAXBMarshaller.unmarshallTextMessage(textMessage, SetCommandRequest.class);
                    AcknowledgeTypeType setCommand = service.setCommand(setCommandRequest.getCommand());
                    AcknowledgeType setCommandAck = ExchangePluginResponseMapper.mapToAcknowledgeType(textMessage.getJMSMessageID(), setCommand);
                    responseMessage = ExchangePluginResponseMapper.mapToSetCommandResponse(startup.getRegisterClassName() + "." + startup.getApplicationName(), setCommandAck);
                    break;
                case SET_REPORT:
                    SetReportRequest setReportRequest = JAXBMarshaller.unmarshallTextMessage(textMessage, SetReportRequest.class);
                    AcknowledgeTypeType setReport = service.setReport(setReportRequest.getReport());
                    AcknowledgeType setReportAck = ExchangePluginResponseMapper.mapToAcknowledgeType(textMessage.getJMSMessageID(), setReport);
                    responseMessage = ExchangePluginResponseMapper.mapToSetReportResponse(startup.getRegisterClassName() + "." + startup.getApplicationName(), setReportAck);
                    break;
                case START:
                    StartRequest startRequest = JAXBMarshaller.unmarshallTextMessage(textMessage, StartRequest.class);
                    AcknowledgeTypeType start = service.start();
                    AcknowledgeType startAck = ExchangePluginResponseMapper.mapToAcknowledgeType(textMessage.getJMSMessageID(), start);
                    responseMessage = ExchangePluginResponseMapper.mapToStartResponse(startup.getRegisterClassName() + "." + startup.getApplicationName(), startAck);
                    break;
                case STOP:
                    StopRequest stopRequest = JAXBMarshaller.unmarshallTextMessage(textMessage, StopRequest.class);
                    AcknowledgeTypeType stop = service.stop();
                    AcknowledgeType stopAck = ExchangePluginResponseMapper.mapToAcknowledgeType(textMessage.getJMSMessageID(), stop);
                    responseMessage = ExchangePluginResponseMapper.mapToStopResponse(startup.getRegisterClassName() + "." + startup.getApplicationName(), stopAck);
                    break;
                case PING:
                    PingRequest pingRequest = JAXBMarshaller.unmarshallTextMessage(textMessage, PingRequest.class);
                    responseMessage = ExchangePluginResponseMapper.mapToPingResponse(startup.isIsEnabled(), startup.isIsEnabled());
                    break;
                default:
                    LOG.error("Not supported method");
                    break;
            }

            messageProducer.sendResponseMessage(responseMessage, textMessage);

        } catch (RuntimeException e) {
            LOG.error("[ Error when receiving message in siriusone " + startup.getRegisterClassName() + " ]", e);
        } catch (JMSException ex) {
            LOG.error("[ Error when handling JMS message in siriusone " + startup.getRegisterClassName() + " ]", ex);
        }
    }
}