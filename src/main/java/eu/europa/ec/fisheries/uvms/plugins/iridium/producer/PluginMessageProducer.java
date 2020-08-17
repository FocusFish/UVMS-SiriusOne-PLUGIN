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
package eu.europa.ec.fisheries.uvms.plugins.iridium.producer;

import static eu.europa.ec.fisheries.uvms.plugins.iridium.constants.ModuleQueue.EXCHANGE;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import eu.europa.ec.fisheries.uvms.commons.message.api.MessageConstants;
import eu.europa.ec.fisheries.uvms.exchange.model.constant.ExchangeModelConstants;
import eu.europa.ec.fisheries.uvms.plugins.iridium.constants.ModuleQueue;

@Singleton
public class PluginMessageProducer {

    final static Logger LOG = LoggerFactory.getLogger(PluginMessageProducer.class);

    @Resource(mappedName = "java:/" + ExchangeModelConstants.EXCHANGE_MESSAGE_IN_QUEUE)
    private Queue exchangeQueue;


    @Resource(mappedName = "java:/" + ExchangeModelConstants.PLUGIN_EVENTBUS)
    private Topic eventBus;

    @Resource(mappedName = "java:/ConnectionFactory")
    private ConnectionFactory connectionFactory;

    @PostConstruct
    public void resourceLookup() {
    }

    public void sendResponseMessage(String text, TextMessage requestMessage) throws JMSException {
        try (Connection connection = connectionFactory.createConnection();
             Session session = connection.createSession(false, 1);
             MessageProducer producer = session.createProducer(requestMessage.getJMSReplyTo());
        ) {
            TextMessage message = session.createTextMessage();
            message.setJMSDestination(requestMessage.getJMSReplyTo());
            message.setJMSCorrelationID(requestMessage.getJMSMessageID());
            message.setText(text);

            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.send(message);
        }
    }

    public String sendModuleMessage(String text, ModuleQueue queue, String function) throws JMSException {
        try (Connection connection = connectionFactory.createConnection();
             Session session = connection.createSession(false, 1);
             MessageProducer producer = session.createProducer(exchangeQueue);
        ) {

            TextMessage message = session.createTextMessage();
            message.setText(text);
            message.setStringProperty(MessageConstants.JMS_FUNCTION_PROPERTY, function);

            if (EXCHANGE == queue) {
                producer.setDeliveryMode(DeliveryMode.PERSISTENT);
                producer.send(message);

            } else {
                LOG.error("[ Sending Queue is not implemented ]");
            }

            LOG.debug("SendMessage-queue:{}, message:{}", queue, message);
            return message.getJMSMessageID();

        } catch (JMSException e) {
            LOG.error("[ Error when sending data source message. {}] {}", text, e.getMessage());
            throw new JMSException(e.getMessage());
        }
    }

    public String sendEventBusMessage(String text, String serviceName, String function) throws JMSException {
        try (Connection connection = connectionFactory.createConnection();
             Session session = connection.createSession(false, 1);
             MessageProducer producer = session.createProducer(eventBus);
        ) {
            TextMessage message = session.createTextMessage();
            message.setText(text);
            message.setStringProperty(ExchangeModelConstants.SERVICE_NAME, serviceName);
            message.setStringProperty(MessageConstants.JMS_FUNCTION_PROPERTY, function);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.send(message);
            return message.getJMSMessageID();
        } catch (JMSException e) {
            LOG.error(e.toString(),e);
            throw e;
        }
    }

}