package org.bitcask.application;

import java.io.IOException;

public interface Bitcask {
    void start();
	String read(String key) throws IOException;
	void write(String key, String value) throws IOException;
    void shutdown();
}
