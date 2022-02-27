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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 **/
public class SiriusOneMessage {

    private static final Logger LOG = LoggerFactory.getLogger(SiriusOneMessage.class);

    private long deviceId;
    private long msgSequenseNo;
    private BoolType normalReport;
    private BoolType gpsValid;
    int time;
    private LonHemispherType lonHemis;
    private LatHemispherType latHemis;
    private double longitude;
    private double latitude;
    private double speed;
    private double course;
    private byte[] msg;
    private String strMsg;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss");

    public enum BoolType {

        FALSE(0), TRUE(1);
        private int value;

        private BoolType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public enum LonHemispherType {

        WEST(0), EAST(1);
        private int value;

        private LonHemispherType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public enum LatHemispherType {

        SOUTH(1), NORTH(0);
        private int value;

        private LatHemispherType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public SiriusOneMessage(byte[] msg, long deviceId, long msgSequenseNo) {
        this.msg = msg;
        this.deviceId = deviceId;
        this.msgSequenseNo = msgSequenseNo;
        parse(msg);
    }

    public boolean isValid() {
        return true;
    }

    void parse(byte[] msg) {
        StringBuffer buff = new StringBuffer();
        for (int i = 0; i < msg.length; i++) {
            int num = Integer.parseInt(String.format("%02X ", msg[i]).trim(), 16);
            String tmp = Integer.toBinaryString(num);
            String s = String.format("%8s", tmp).replace(' ', '0');
            buff.append(s);
        }
        strMsg = buff.substring(0, 10 * 8);
        LOG.debug("Stringmsg: " + strMsg);
        setNormalReport(strMsg);
        setGpsValid(strMsg);
        setTime(strMsg);
        setLatHemis(strMsg);
        setLatitude(strMsg);
        setLonHemis(strMsg);
        setLongitude(strMsg);
        setSpeed(strMsg);
        setCourse(strMsg);

        print();
    }

    private void setNormalReport(String strMsg) {
        normalReport = BoolType.TRUE;
        if (strMsg.startsWith("1")) {
            normalReport = BoolType.FALSE;
        }
    }

    private void setGpsValid(String strMsg) {
        gpsValid = BoolType.FALSE;
        if (strMsg.charAt(1) == '1') {
            gpsValid = BoolType.TRUE;
        }
    }

    void setTime(String strMsg) {
        time = Integer.parseInt(strMsg.substring(2, 25), 2);
    }

    public XMLGregorianCalendar getDateTime() {
        XMLGregorianCalendar xmlGregorianCalendar = null;
        DatatypeFactory df;
        GregorianCalendar cal = new GregorianCalendar(2015, Calendar.JANUARY, 1, 0, 1);
        cal.add(Calendar.MINUTE, time);
        try {
            df = DatatypeFactory.newInstance();
            xmlGregorianCalendar = df.newXMLGregorianCalendar(cal);
        } catch (DatatypeConfigurationException ex) {

        }
        return xmlGregorianCalendar;
    }

    private void setLonHemis(String strMsg) {
        lonHemis = LonHemispherType.EAST;
        if (strMsg.charAt(25) == '1') {
            lonHemis = LonHemispherType.WEST;
        }
    }

    private void setLongitude(String strMsg) {
        int i = Integer.parseInt(strMsg.substring(26, 45), 2);
        longitude = (double) i * 0.000344;
    }

    private void setLatHemis(String strMsg) {
        latHemis = LatHemispherType.SOUTH;
        if (strMsg.charAt(45)== '0') {
            latHemis = LatHemispherType.NORTH;
        }
    }

    private void setLatitude(String strMsg) {
        int i = Integer.parseInt(strMsg.substring(46, 64), 2);
        latitude = (double) i * 0.000344;
    }

    private void setSpeed(String strMsg) {
        int i = Integer.parseInt(strMsg.substring(64, 72), 2);
        speed = (double) i / 5;
    }

    private void setCourse(String strMsg) {
        int i = Integer.parseInt(strMsg.substring(72), 2);
        course = (double) i * 1.5;
    }

    private void print() {
        LOG.debug(strMsg);
        LOG.debug("normalreport: " + normalReport);
        LOG.debug("gpsValid: " + gpsValid);
        LOG.debug("time: " + time);
        LOG.debug("lonHemis: " + lonHemis);
        LOG.debug("longitude: " + longitude);
        LOG.debug("latHemis: " + latHemis);
        LOG.debug("latitude: " + latitude);
        LOG.debug("speed: " + speed);
        LOG.debug("course: " + course);
    }

    public long getDeviceId() {
        return deviceId;
    }

    public double getLongitude() {
        if (lonHemis == LonHemispherType.WEST) {
            return longitude * -1.0;
        }
        return longitude;
    }

    public double getLatitude() {
        if (latHemis == LatHemispherType.SOUTH) {
            return latitude * -1.0;
        }

        return latitude;
    }

    public double getSpeed() {
        return speed;
    }

    public double getCourse() {
        return course;
    }

}