/*
 * Copyright 2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.driver.ovsdb;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.behaviour.BridgeDescription;
import org.onosproject.net.behaviour.BridgeName;
import org.onosproject.net.behaviour.DefaultBridgeDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.ovsdb.controller.OvsdbBridge;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.onosproject.ovsdb.controller.OvsdbPort;

import com.google.common.collect.Sets;

/**
 * The implementation of BridageConfig.
 */
public class OvsdbBridgeConfig extends AbstractHandlerBehaviour
        implements BridgeConfig {

    @Override
    public void addBridge(BridgeName bridgeName) {
        DriverHandler handler = handler();
        OvsdbClientService clientService = getOvsdbClientService(handler);
        clientService.createBridge(bridgeName.name());
    }

    @Override
    public void deleteBridge(BridgeName bridgeName) {
        DriverHandler handler = handler();
        OvsdbClientService clientService = getOvsdbClientService(handler);
        clientService.dropBridge(bridgeName.name());
    }

    @Override
    public Collection<BridgeDescription> getBridges() {
        DriverHandler handler = handler();
        DeviceId deviceId = handler.data().deviceId();
        OvsdbClientService clientService = getOvsdbClientService(handler);
        Set<OvsdbBridge> ovsdbSet = clientService.getBridges();
        Collection<BridgeDescription> bridges = Sets.newHashSet();
        ovsdbSet.forEach(o -> {
            BridgeName bridgeName = BridgeName
                    .bridgeName(o.bridgeName().value());
            DeviceId ownDeviceId = DeviceId.deviceId("of:" + o.datapathId().value());
            BridgeDescription description = new DefaultBridgeDescription(bridgeName,
                                                                         deviceId,
                                                                         ownDeviceId);
            bridges.add(description);
        });
        return bridges == null ? Collections.emptySet() : bridges;
    }

    @Override
    public void addPort(PortDescription port) {
        DriverHandler handler = handler();
        OvsdbClientService clientService = getOvsdbClientService(handler);
        Set<OvsdbBridge> ovsdbSet = clientService.getBridges();
        if (ovsdbSet != null && ovsdbSet.size() > 0) {
            OvsdbBridge bridge = ovsdbSet.iterator().next();
            clientService.createPort(bridge.bridgeName().toString(), port
                    .portNumber().toString());
        }
    }

    @Override
    public void deletePort(PortDescription port) {
        DriverHandler handler = handler();
        OvsdbClientService clientService = getOvsdbClientService(handler);
        Set<OvsdbBridge> ovsdbSet = clientService.getBridges();
        if (ovsdbSet != null && ovsdbSet.size() > 0) {
            OvsdbBridge bridge = ovsdbSet.iterator().next();
            clientService.dropPort(bridge.bridgeName().toString(), port
                    .portNumber().toString());
        }
    }

    @Override
    public Collection<PortDescription> getPorts() {
        DriverHandler handler = handler();
        OvsdbClientService clientService = getOvsdbClientService(handler);
        Set<OvsdbPort> ovsdbSet = clientService.getPorts();
        Collection<PortDescription> ports = Sets.newHashSet();
        ovsdbSet.forEach(o -> {
            PortNumber port = PortNumber.portNumber(o.portNumber().value());
            PortDescription description = new DefaultPortDescription(port, true);
            ports.add(description);
        });
        return ports;
    }

    // OvsdbNodeId(IP:port) is used in the adaptor while DeviceId(ovsdb:IP:port)
    // is used in the core. So DeviceId need be changed to OvsdbNodeId.
    private OvsdbNodeId changeDeviceIdToNodeId(DeviceId deviceId) {
        int lastColon = deviceId.toString().lastIndexOf(":");
        int fistColon = deviceId.toString().indexOf(":");
        String ip = deviceId.toString().substring(fistColon + 1, lastColon);
        String port = deviceId.toString().substring(lastColon + 1);
        IpAddress ipAddress = IpAddress.valueOf(ip);
        long portL = Long.valueOf(port).longValue();
        return new OvsdbNodeId(ipAddress, portL);
    }

    // Used for getting OvsdbClientService.
    private OvsdbClientService getOvsdbClientService(DriverHandler handler) {
        OvsdbController ovsController = handler.get(OvsdbController.class);
        DeviceId deviceId = handler.data().deviceId();
        OvsdbNodeId nodeId = changeDeviceIdToNodeId(deviceId);
        return ovsController.getOvsdbClient(nodeId);
    }

    @Override
    public Set<PortNumber> getPortNumbers() {
        Set<PortNumber> ports = new HashSet<>();
        DriverHandler handler = handler();
        OvsdbClientService clientService = getOvsdbClientService(handler);
        Set<OvsdbPort> ovsdbSet = clientService.getPorts();
        ovsdbSet.forEach(o -> {
            PortNumber port = PortNumber.portNumber(o.portNumber().value(),
                                                    o.portName().value());
            ports.add(port);
        });
        return ports;
    }
}
