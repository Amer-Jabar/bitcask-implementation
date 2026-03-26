package org.bitcask.memory;

import org.bitcask.application.ObjectAddress;

import java.util.HashMap;
import java.util.Map;

public class Keydir {
	private final Map<String, ObjectAddress> keydir;

	public Keydir() {
		this.keydir = new HashMap<>();
	}

	public ObjectAddress read(String key) {
		if (!keydir.containsKey(key)) {
			return null;
		}

		return keydir.get(key);
	}

	public void write(String key, ObjectAddress objectAddress) {
		this.keydir.put(key, objectAddress);
	}

    public Map<String, ObjectAddress> asMap() {
        return this.keydir;
    }

    public void reload(Map<String, ObjectAddress> map) {
        this.keydir.clear();
        this.keydir.putAll(map);
    }
}
