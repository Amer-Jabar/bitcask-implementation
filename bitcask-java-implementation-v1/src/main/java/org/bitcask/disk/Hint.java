package org.bitcask.disk;

import org.bitcask.application.ObjectAddress;

import java.util.HashMap;
import java.util.Map;

public record Hint(Map<String, ObjectAddress> map) {
    public static Hint of(String text) {
        Map<String, ObjectAddress> map = new HashMap<>();
        text.lines().forEach(line -> {
            String[] entryParts = line.split(":");
            String[] objectAddressParts = entryParts[1].split(",");
            map.put(entryParts[0], new ObjectAddress(objectAddressParts[0], Long.parseLong(objectAddressParts[1]), Long.parseLong(objectAddressParts[2])));
        });
        return new Hint(map);
    }

    public static Hint of(Map<String, ObjectAddress> map) {
        return new Hint(map);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        map.forEach((key, objectAddress) -> stringBuilder.append("%s:%s,%s,%s\n".formatted(key, objectAddress.filename(), objectAddress.offset(), objectAddress.payloadSize())));
        return stringBuilder.toString();
    }
}
