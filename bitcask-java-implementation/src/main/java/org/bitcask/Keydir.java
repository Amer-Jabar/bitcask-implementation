package org.bitcask;

import java.util.HashMap;

public class Keydir {
	private final HashMap<String, ObjectAddress> keydir;
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
}
