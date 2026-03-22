package org.bitcask;

import java.io.IOException;

public interface Bitcask {
	String read(String key) throws IOException;
	void write(String key, String value) throws IOException;
}
