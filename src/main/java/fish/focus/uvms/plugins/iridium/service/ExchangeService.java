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
package fish.focus.uvms.plugins.iridium.service;

import fish.focus.schema.exchange.module.v1.ExchangeModuleMethod;
import fish.focus.schema.exchange.movement.v1.SetReportMovementType;
import fish.focus.schema.exchange.plugin.types.v1.PluginType;
import fish.focus.uvms.exchange.model.mapper.ExchangeModuleRequestMapper;
import fish.focus.uvms.plugins.iridium.StartupBean;
import fish.focus.uvms.plugins.iridium.producer.PluginMessageProducer;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.jms.JMSException;
import java.time.Instant;

@RequestScoped
public class ExchangeService {

    @Inject
    StartupBean startupBean;

    @Inject
    PluginMessageProducer producer;

    public void sendMovementReportToExchange(SetReportMovementType reportType) throws JMSException {
        String text = ExchangeModuleRequestMapper.createSetMovementReportRequest(reportType, "SIRIUSONE", null, Instant.now(), PluginType.SATELLITE_RECEIVER, "SIRIUSONE", null);
        producer.sendMessageToExchange(text, ExchangeModuleMethod.SET_MOVEMENT_REPORT.value());
    }
}