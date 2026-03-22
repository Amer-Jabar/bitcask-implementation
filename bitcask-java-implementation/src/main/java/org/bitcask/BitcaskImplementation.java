package org.bitcask;

import java.io.IOException;

public class BitcaskImplementation implements Bitcask {
	private final ActiveFile activeFile;
	private final Keydir keydir;
	public BitcaskImplementation(String baseDirectory) throws IOException {
		this.activeFile = new ActiveFile(baseDirectory);
		this.keydir = new Keydir();
	}

	@Override
	public String read(String key) throws IOException {
		ObjectAddress address;
		if ((address = this.keydir.read(key)) == null) {
			return null;
		}
		return this.activeFile.read(address);
	}

	@Override
	public void write(String key, String value) throws IOException {
		ObjectAddress objectAddress = this.activeFile.write(key, value);
		this.keydir.write(key, objectAddress);
	}
}
