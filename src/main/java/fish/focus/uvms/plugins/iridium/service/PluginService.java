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

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fish.focus.schema.exchange.common.v1.AcknowledgeTypeType;
import fish.focus.schema.exchange.common.v1.CommandType;
import fish.focus.schema.exchange.common.v1.CommandTypeType;
import fish.focus.schema.exchange.common.v1.KeyValueType;
import fish.focus.schema.exchange.common.v1.ReportType;
import fish.focus.schema.exchange.common.v1.ReportTypeType;
import fish.focus.schema.exchange.movement.v1.MovementPoint;
import fish.focus.schema.exchange.movement.v1.MovementType;
import fish.focus.schema.exchange.plugin.types.v1.EmailType;
import fish.focus.schema.exchange.plugin.types.v1.PollType;
import fish.focus.schema.exchange.service.v1.SettingListType;
import fish.focus.uvms.plugins.iridium.StartupBean;

@RequestScoped
public class PluginService {

    @Inject
    StartupBean startupBean;

    private static final Logger LOG = LoggerFactory.getLogger(PluginService.class);

    /**
     * TODO implement
     *
     * @param report
     * @return
     */
    public AcknowledgeTypeType setReport(ReportType report) {
        LOG.info(startupBean.getRegisterClassName() + ".report(" + report.getType().name() + ")");
        LOG.debug("timestamp: " + report.getTimestamp());
        MovementType movement = report.getMovement();
        if (movement != null && ReportTypeType.MOVEMENT.equals(report.getType())) {
            MovementPoint pos = movement.getPosition();
            if (pos != null) {
                LOG.info("lon: " + pos.getLongitude());
                LOG.info("lat: " + pos.getLatitude());
            }
        }
        return AcknowledgeTypeType.OK;
    }

    /**
     * TODO implement
     *
     * @param command
     * @return
     */
    public AcknowledgeTypeType setCommand(CommandType command) {
        LOG.info(startupBean.getRegisterClassName() + ".setCommand(" + command.getCommand().name() + ")");
        LOG.debug("timestamp: " + command.getTimestamp());
        PollType poll = command.getPoll();
        EmailType email = command.getEmail();
        if (poll != null && CommandTypeType.POLL.equals(command.getCommand())) {
            LOG.info("POLL: " + poll.getPollId());
        }
        if (email != null && CommandTypeType.EMAIL.equals(command.getCommand())) {
            LOG.info("EMAIL: subject=" + email.getSubject());
        }
        return AcknowledgeTypeType.OK;
    }

    /**
     * Set the config values for the siriusone
     *
     * @param settings
     * @return
     */
    public AcknowledgeTypeType setConfig(SettingListType settings) {
        LOG.info(startupBean.getRegisterClassName() + ".setConfig()");
        try {
            for (KeyValueType values : settings.getSetting()) {
                LOG.debug("Setting [ " + values.getKey() + " : " + values.getValue() + " ]");
                startupBean.getSettings().put(values.getKey(), values.getValue());
            }
            return AcknowledgeTypeType.OK;
        } catch (Exception e) {
            LOG.error("Failed to set config in {}", startupBean.getRegisterClassName());
            return AcknowledgeTypeType.NOK;
        }

    }

    /**
     * Start the siriusone. Use this to enable functionality in the siriusone
     *
     * @return
     */
    public AcknowledgeTypeType start() {
        LOG.info(startupBean.getRegisterClassName() + ".start()");
        try {
            startupBean.setIsEnabled(Boolean.TRUE);
            return AcknowledgeTypeType.OK;
        } catch (Exception e) {
            startupBean.setIsEnabled(Boolean.FALSE);
            LOG.error("Failed to start {}", startupBean.getRegisterClassName());
            return AcknowledgeTypeType.NOK;
        }

    }

    /**
     * Start the siriusone. Use this to disable functionality in the siriusone
     *
     * @return
     */
    public AcknowledgeTypeType stop() {
        LOG.info(startupBean.getRegisterClassName() + ".stop()");
        try {
            startupBean.setIsEnabled(Boolean.FALSE);
            return AcknowledgeTypeType.OK;
        } catch (Exception e) {
            startupBean.setIsEnabled(Boolean.TRUE);
            LOG.error("Failed to stop {}", startupBean.getRegisterClassName());
            return AcknowledgeTypeType.NOK;
        }
    }

}