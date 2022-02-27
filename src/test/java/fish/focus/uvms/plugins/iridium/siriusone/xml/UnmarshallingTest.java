package fish.focus.uvms.plugins.iridium.siriusone.xml;

import static org.junit.Assert.assertThat;
import java.io.ByteArrayInputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import fish.focus.uvms.plugins.iridium.siriusone.xml.Device;
import fish.focus.uvms.plugins.iridium.siriusone.xml.Devices;
import fish.focus.uvms.plugins.iridium.siriusone.xml.Position;
import fish.focus.uvms.plugins.iridium.siriusone.xml.Positions;

public class UnmarshallingTest {

    @Test
    public void unmarshallXmlTest() throws JAXBException {
        Unmarshaller marshaller = JAXBContext.newInstance(Devices.class).createUnmarshaller();
        Devices devices = (Devices) marshaller.unmarshal(getXml());
        
        Device device = devices.getDevice().get(0);
        assertThat(device.getSerial(), CoreMatchers.is("E12345678"));
        Positions positions = device.getPositions();
        Position position = positions.getPosition().get(0);
        assertThat(position.getLatitude(), CoreMatchers.is(57.7152863));
        assertThat(position.getLongitude(), CoreMatchers.is(11.9736080));
        assertThat(position.getGps().toString(), CoreMatchers.is("2020-09-02T14:59:01"));
        assertThat(position.getCourse(), CoreMatchers.is(123.0));
        assertThat(position.getSpeed().getKnots(), CoreMatchers.is(1.2));
    }
    
    private ByteArrayInputStream getXml() {
        String xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + 
                "<devices>\n" + 
                "    <device>\n" + 
                "        <devicetype>SiriusOne</devicetype>\n" + 
                "        <name>Test</name>\n" + 
                "        <firmware>3.00</firmware>\n" + 
                "        <serial>E12345678</serial>\n" + 
                "        <positions>\n" + 
                "            <position>\n" + 
                "                <serial>350</serial>\n" + 
                "                <csp>2020-09-02 15:00:12</csp>\n" + 
                "                <source>Data Report</source>\n" + 
                "                <type>Interval report</type>\n" + 
                "                <timestamp>2020-09-02 15:00:18</timestamp>\n" + 
                "                <gps>2020-09-02 14:59:01</gps>\n" + 
                "                <speed>\n" + 
                "                    <knots>1.2</knots>\n" + 
                "                    <km>0</km>\n" + 
                "                </speed>\n" + 
                "                <course>123</course>\n" + 
                "                <odometer/>\n" + 
                "                <io/>\n" + 
                "                <zone/>\n" + 
                "                <analog/>\n" + 
                "                <latitude>57.7152863</latitude>\n" + 
                "                <longitude>11.9736080</longitude>\n" + 
                "                <altitude/>\n" + 
                "            </position>\n" + 
                "        </positions>\n" + 
                "    </device>\n" + 
                "</devices>\n" + 
                "";
        return new ByteArrayInputStream(xmlString.getBytes());
    }
}