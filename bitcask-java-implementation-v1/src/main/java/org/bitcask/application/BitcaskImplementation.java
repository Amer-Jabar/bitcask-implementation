package org.bitcask.application;

import org.bitcask.disk.ActiveFile;
import org.bitcask.disk.ActiveFileImplementation;
import org.bitcask.disk.Hint;
import org.bitcask.disk.Payload;
import org.bitcask.memory.Keydir;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BitcaskImplementation implements Bitcask {
    private String baseDirectory;
    private ActiveFile activeFile;
    private Keydir keydir;

    public BitcaskImplementation(String baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    @Override
    public void start() {
        this.activeFile = new ActiveFileImplementation(this.baseDirectory);
        this.keydir = new Keydir();
    }

    @Override
    public String read(String key) throws IOException {
        ObjectAddress address;
        if ((address = this.keydir.read(key)) == null) {
            reloadKeydir();
            if ((address = this.keydir.read(key)) == null) {
                return null;
            }
        }
        return this.activeFile.read(address).value();
    }

    @Override
    public void write(String key, String value) throws IOException {
        ObjectAddress objectAddress = this.activeFile.write(new Payload(key, value));
        this.keydir.write(key, objectAddress);
        this.activeFile.flush(Hint.of(this.keydir.asMap()));
    }

    private void reloadKeydir() {
        this.keydir.reload(this.activeFile.fetchAllHints()
                .stream()
                .collect(
                        () -> new HashMap<String, ObjectAddress>(),
                        (hashMap, hint) -> hashMap.putAll(hint.map()),
                        (firstMap, secondMap) -> firstMap.putAll(secondMap)
                ));
    }

    @Override
    public void shutdown() {
        this.activeFile.close();
    }
}
