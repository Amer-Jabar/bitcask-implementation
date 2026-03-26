package org.bitcask.disk;

import org.bitcask.application.ObjectAddress;

import java.io.IOException;
import java.util.List;

public interface ActiveFile {
    Payload read(ObjectAddress objectAddress) throws IOException;
    ObjectAddress write(Payload payload) throws IOException;

    List<Hint> fetchAllHints();
    void flush(Hint hint);

    void close();
}
