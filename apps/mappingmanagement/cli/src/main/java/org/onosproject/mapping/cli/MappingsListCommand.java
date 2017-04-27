/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.mapping.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.mapping.Mapping;
import org.onosproject.mapping.MappingKey;
import org.onosproject.mapping.MappingTreatment;
import org.onosproject.mapping.MappingValue;
import org.onosproject.mapping.MappingService;
import org.onosproject.mapping.MappingStore;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * A command for querying mapping information.
 */
@Command(scope = "onos", name = "mappings",
        description = "Lists mappings")
public class MappingsListCommand extends AbstractShellCommand {

    private static final String DB = "map_database";
    private static final String CACHE = "map_cache";

    private static final String SUMMARY_FORMAT = "deviceId=%s, mappingCount=%d";
    private static final String MAPPING_ID_FORMAT = "  id=%s";
    private static final String MAPPING_KEY_FORMAT = "  key=%s";
    private static final String MAPPING_VALUE_FORMAT = "  value=";
    private static final String MAPPING_ACTION_FORMAT = "    action=%s";
    private static final String MAPPING_TREATMENTS_FORMAT = "    treatments=";
    private static final String MAPPING_TREATMENT_LONG_FORMAT =
            "      address=%s, instructions=%s";
    private static final String MAPPING_TREATMENT_SHORT_FORMAT = "      %s";

    @Argument(index = 0, name = "type",
            description = "Shows mappings with specified type",
            required = true, multiValued = false)
    private String type = null;

    @Argument(index = 1, name = "deviceId", description = "Device identity",
            required = false, multiValued = false)
    private String deviceId = null;

    @Option(name = "-s", aliases = "--short",
            description = "Print more succinct output for each mapping",
            required = false, multiValued = false)
    private boolean shortOutput = false;

    private MappingService mappingService =
            AbstractShellCommand.get(MappingService.class);
    private List<Mapping> mappings;

    @Override
    protected void execute() {

        MappingStore.Type typeEnum = null;

        if (type.equals(DB)) {
            typeEnum = MappingStore.Type.MAP_DATABASE;
        } else if (type.equals(CACHE)) {
            typeEnum = MappingStore.Type.MAP_CACHE;
        }

        DeviceService deviceService = get(DeviceService.class);
        Iterable<Device> devices = deviceService.getDevices();

        if (deviceId != null) {
            mappings = newArrayList(mappingService.getMappingEntries(typeEnum,
                    DeviceId.deviceId(deviceId)));
            printMappings(DeviceId.deviceId(deviceId), mappings);

        } else {

            for (Device d : devices) {
                mappings = newArrayList(mappingService.getMappingEntries(typeEnum, d.id()));
                printMappings(d.id(), mappings);
            }
        }
    }

    /**
     * Prints out mapping information.
     *
     * @param deviceId device identifier
     * @param mappings a collection of mapping
     */
    private void printMappings(DeviceId deviceId, List<Mapping> mappings) {

        print(SUMMARY_FORMAT, deviceId, mappings.size());

        for (Mapping m : mappings) {
            print(MAPPING_ID_FORMAT, Long.toHexString(m.id().value()));
            print(MAPPING_KEY_FORMAT, printMappingKey(m.key()));
            printMappingValue(m.value());
        }
    }

    /**
     * Prints out mapping key.
     *
     * @param key mapping key
     * @return string format of mapping key
     */
    private String printMappingKey(MappingKey key) {
        StringBuilder builder = new StringBuilder();

        if (key.address() != null) {
            builder.append(key.address().toString());
        }

        return builder.toString();
    }

    /**
     * Prints out mapping value.
     *
     * @param value mapping value
     * @return string format of mapping value
     */
    private void printMappingValue(MappingValue value) {

        print(MAPPING_VALUE_FORMAT);

        if (value.action() != null) {
            print(MAPPING_ACTION_FORMAT, value.action().toString());
        }

        if (!value.treatments().isEmpty()) {
            print(MAPPING_TREATMENTS_FORMAT);
            for (MappingTreatment treatment : value.treatments()) {
                printMappingTreatment(treatment);
            }
        }

    }

    /**
     * Prints out mapping treatment.
     *
     * @param treatment mapping treatment
     * @return string format of mapping treatment
     */
    private void printMappingTreatment(MappingTreatment treatment) {
        if (treatment != null) {
            if (shortOutput) {
                print(MAPPING_TREATMENT_SHORT_FORMAT, treatment.address());
            } else {
                print(MAPPING_TREATMENT_LONG_FORMAT, treatment.address(),
                        treatment.instructions());
            }
        }
    }
}