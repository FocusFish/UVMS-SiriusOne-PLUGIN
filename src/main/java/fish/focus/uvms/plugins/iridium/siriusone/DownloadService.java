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
package fish.focus.uvms.plugins.iridium.siriusone;

import fish.focus.schema.exchange.movement.mobileterminal.v1.IdList;
import fish.focus.schema.exchange.movement.mobileterminal.v1.IdType;
import fish.focus.schema.exchange.movement.mobileterminal.v1.MobileTerminalId;
import fish.focus.schema.exchange.movement.v1.*;
import fish.focus.schema.exchange.plugin.types.v1.PluginType;
import fish.focus.uvms.plugins.iridium.StartupBean;
import fish.focus.uvms.plugins.iridium.service.ExchangeService;
import fish.focus.uvms.plugins.iridium.siriusone.xml.Device;
import fish.focus.uvms.plugins.iridium.siriusone.xml.Devices;
import fish.focus.uvms.plugins.iridium.siriusone.xml.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.mail.*;
import javax.mail.Flags.Flag;
import javax.mail.search.FlagTerm;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Properties;

@RequestScoped
public class DownloadService {

    private static final Logger LOG = LoggerFactory.getLogger(DownloadService.class);

    @Inject
    StartupBean startUp;

    @Inject
    ExchangeService service;

    public void download() {
        LOG.debug("Download invoked");
        try {
            getMessages();
        } catch (Exception e) {
            LOG.error("Could not get messages", e);
        }
    }

    private void getMessages() throws IOException, MessagingException, JAXBException {
        Message[] mails = getMails(startUp.getSetting("MAILHOST"), startUp.getSetting("MAILPORT"), startUp.getSetting("USERNAME"), startUp.getSetting("PSW"), startUp.getSetting("SUBFOLDER"));
        LOG.info("New messages: {}", mails.length);

        for (Message message : mails) {
            Multipart multipart = (Multipart) message.getContent();

            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (!Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) && (bodyPart.getFileName() == null || bodyPart.getFileName().isEmpty())) {
                    continue; // dealing with attachments only
                }
                try {
                    InputStream is = bodyPart.getInputStream();
                    String fileName = bodyPart.getFileName();
                    if (fileName.endsWith(".xml")) {
                        handleXmlReport(is);
                    } else {
                        handle10BytesReport(fileName, is);
                    }
                    message.setFlag(Flag.SEEN, true);
                } catch (Exception e) {
                    LOG.error("Could not handle report", e);
                    message.setFlag(Flag.SEEN, false);
                }
            }
        }
    }

    private Message[] getMails(String server, String port, String user, String password, String inboxSubfolder) throws MessagingException {
        Properties props = System.getProperties();

        props.setProperty("mail.imap.starttls.enable", "true");
        props.setProperty("mail.imap.port", port);

        Session session = Session.getInstance(props, null);

        Store store = session.getStore("imap");
        store.connect(server, user, password);
        Folder inbox = store.getFolder("INBOX");

        if (inboxSubfolder != null) {
            inbox = inbox.getFolder(inboxSubfolder);
        }

        // search for all "unseen" messages
        Flags seen = new Flags(Flags.Flag.SEEN);
        FlagTerm unseenFlagTerm = new FlagTerm(seen, false);

        inbox.open(Folder.READ_WRITE);
        return inbox.search(unseenFlagTerm);
    }

    private void handle10BytesReport(String fileName, InputStream is) throws JMSException, IOException {
        fileName = fileName.substring(0, fileName.indexOf('.'));
        String[] splittedFilename = fileName.split("_");

        if (splittedFilename.length == 2) {
            byte[] buf = new byte[100];
            SiriusOneMessage mes = null;
            while (is.read(buf) != -1) {
                mes = new SiriusOneMessage(buf, Long.parseLong(splittedFilename[0]), Long.parseLong(splittedFilename[1]));
                if (mes.isValid()) {
                    SetReportMovementType reportType = mapToSetReportMovementType(mes);
                    service.sendMovementReportToExchange(reportType);
                    LOG.debug("Sending movement to Exchange");
                }
            }
        }
    }

    private SetReportMovementType mapToSetReportMovementType(SiriusOneMessage msg) {
        MovementBaseType movement = new MovementBaseType();
        movement.setComChannelType(MovementComChannelType.MOBILE_TERMINAL);
        MobileTerminalId mobTermId = new MobileTerminalId();

        IdList deviceId = new IdList();
        deviceId.setType(IdType.SERIAL_NUMBER);
        deviceId.setValue("" + msg.getDeviceId());

        mobTermId.getMobileTerminalIdList().add(deviceId);

        movement.setMobileTerminalId(mobTermId);

        movement.setMovementType(MovementTypeType.POS);

        MovementPoint mp = new MovementPoint();
        mp.setAltitude(0.0);
        mp.setLatitude(msg.getLatitude());
        mp.setLongitude(msg.getLongitude());
        movement.setPosition(mp);

        movement.setPositionTime(msg.getDateTime().toGregorianCalendar().getTime());

        movement.setReportedCourse(msg.getCourse());

        movement.setReportedSpeed(msg.getSpeed());

        movement.setSource(MovementSourceType.IRIDIUM);

        movement.setStatus("11");

        SetReportMovementType reportType = new SetReportMovementType();
        reportType.setMovement(movement);

        reportType.setPluginName(startUp.getRegisterClassName() + "." + startUp.getApplicationName());

        reportType.setTimestamp(new Date());

        reportType.setPluginType(PluginType.SATELLITE_RECEIVER);

        LOG.debug("LONGITUDE GET {}", msg.getLongitude());
        LOG.debug("LATITUDE GET {}", msg.getLatitude());

        return reportType;
    }

    private void handleXmlReport(InputStream is) throws JAXBException, JMSException {
        Unmarshaller marshaller = JAXBContext.newInstance(Devices.class).createUnmarshaller();
        Devices devices = (Devices) marshaller.unmarshal(is);
        SetReportMovementType reportType = mapToSetReportMovementType(devices.getDevice().get(0));
        service.sendMovementReportToExchange(reportType);
        LOG.debug("Sending movement to Exchange");
    }

    private SetReportMovementType mapToSetReportMovementType(Device device) {
        MovementBaseType movement = new MovementBaseType();

        movement.setComChannelType(MovementComChannelType.MOBILE_TERMINAL);

        MobileTerminalId mobTermId = new MobileTerminalId();
        IdList deviceId = new IdList();
        deviceId.setType(IdType.SERIAL_NUMBER);
        deviceId.setValue(device.getSerial());
        mobTermId.getMobileTerminalIdList().add(deviceId);
        movement.setMobileTerminalId(mobTermId);

        movement.setMovementType(MovementTypeType.POS);

        Position position = device.getPositions().getPosition().get(0);
        MovementPoint mp = new MovementPoint();
        mp.setAltitude(position.getAltitude());
        mp.setLatitude(position.getLatitude());
        mp.setLongitude(position.getLongitude());
        movement.setPosition(mp);

        movement.setPositionTime(Date.from(position.getGps().toInstant(ZoneOffset.UTC)));

        movement.setReportedCourse(position.getCourse());

        movement.setReportedSpeed(position.getSpeed().getKnots());

        movement.setSource(MovementSourceType.IRIDIUM);

        movement.setLesReportTime(Date.from(position.getTimestamp().toInstant(ZoneOffset.UTC)));

        SetReportMovementType reportType = new SetReportMovementType();
        reportType.setMovement(movement);

        reportType.setPluginName(startUp.getRegisterClassName() + "." + startUp.getApplicationName());

        reportType.setTimestamp(new Date());

        reportType.setPluginType(PluginType.SATELLITE_RECEIVER);

        return reportType;
    }
}