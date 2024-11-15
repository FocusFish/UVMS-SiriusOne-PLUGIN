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

import fish.focus.schema.exchange.movement.v1.SetReportMovementType;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public abstract class PluginDataHolder {

    public static final String PLUGIN_PROPERTIES = "plugin.properties";
    public static final String PROPERTIES = "settings.properties";
    public static final String CAPABILITIES = "capabilities.properties";

    private final ConcurrentHashMap<String, String> settings = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> capabilities = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SetReportMovementType> cachedMovement = new ConcurrentHashMap<>();

    private Properties siriusoneApplicaitonProperties;
    private Properties siriusoneProperties;
    private Properties siriusoneCapabilities;

    public ConcurrentHashMap<String, String> getSettings() {
        return settings;
    }

    public ConcurrentHashMap<String, String> getCapabilities() {
        return capabilities;
    }

    public ConcurrentHashMap<String, SetReportMovementType> getCachedMovement() {
        return cachedMovement;
    }

    public Properties getPluginApplicaitonProperties() {
        return siriusoneApplicaitonProperties;
    }

    public void setPluginApplicaitonProperties(Properties siriusoneApplicaitonProperties) {
        this.siriusoneApplicaitonProperties = siriusoneApplicaitonProperties;
    }

    public Properties getPluginProperties() {
        return siriusoneProperties;
    }

    public void setPluginProperties(Properties siriusoneProperties) {
        this.siriusoneProperties = siriusoneProperties;
    }

    public Properties getPluginCapabilities() {
        return siriusoneCapabilities;
    }

    public void setPluginCapabilities(Properties siriusoneCapabilities) {
        this.siriusoneCapabilities = siriusoneCapabilities;
    }
}