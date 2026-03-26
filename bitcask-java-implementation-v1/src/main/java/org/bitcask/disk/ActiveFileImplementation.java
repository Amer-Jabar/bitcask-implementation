package org.bitcask.disk;

import org.bitcask.application.ObjectAddress;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

import static org.bitcask.infra.Utils.tryFunction;

public class ActiveFileImplementation implements ActiveFile {
    private static final String LOG_FILE_NAME = "log-file";
    private static final String HINT_FILE_NAME = "hint-file";
    private static final Function<Integer, String> LOG_FILE_NAME_FUNCTION = counter -> LOG_FILE_NAME + "-" + counter + ".txt";
    private static final Function<Integer, String> HINT_FILE_NAME_FUNCTION = counter -> HINT_FILE_NAME + "-" + counter + ".txt";
    private static final long FILE_SIZE_THRESHOLD = 1_000_000;
    private static final String CREATION_FAILURE_ERROR_MESSAGE = "Failed to create %s.";

    private final File baseDirectory;

    private File currentlyActiveLogFile;
    private RandomAccessFile currentlyActiveLogFileReader;
    private FileWriter currentlyActiveLogFileWriter;
    private File currentlyActiveHintFile;
    private Scanner currentlyActiveHintFileReader;
    private FileWriter currentlyActiveHintFileWriter;

    public ActiveFileImplementation(String baseDirectoryFilename) {
        this.baseDirectory = createBaseDirectoryIfNotExists(baseDirectoryFilename);
        Optional<Integer> possiblyLastFileCounter = Arrays.stream(baseDirectory.listFiles())
                .max(Comparator.naturalOrder())
                .map(File::getName)
                .map(name -> getFileCounter(name, LOG_FILE_NAME));
        possiblyLastFileCounter.ifPresentOrElse(lastFileCounter -> createActiveLogFile(lastFileCounter), () -> createActiveLogFile(1));
        possiblyLastFileCounter.ifPresentOrElse(lastFileCounter -> createActiveHintFile(lastFileCounter), () -> createActiveHintFile(1));
    }

    private File createBaseDirectoryIfNotExists(String baseDirectory) {
        File file = new File(baseDirectory);
        if (!file.exists() && !file.mkdir()) {
            throw new RuntimeException(CREATION_FAILURE_ERROR_MESSAGE.formatted("base directory"));
        }
        return file;
    }

    private int getFileCounter(String filename, String delimiterKeyword) {
        return Integer.parseInt(filename.split(delimiterKeyword + "-")[1].split(".txt")[0]);
    }

    private File createActiveFile(File baseDirectory, String fileName) {
        return createFile(baseDirectory, fileName, CREATION_FAILURE_ERROR_MESSAGE.formatted("new active file"));
    }

    private File createFile(File baseDirectory, String filename, String errorMessage) {
        File file = new File(baseDirectory.getAbsolutePath() + "/" + filename);
        try {
            if (!file.createNewFile()) throw new RuntimeException(errorMessage);
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Payload read(ObjectAddress objectAddress) throws IOException {
        String[] payloadParts = read0(objectAddress).split(":");
        return new Payload(payloadParts[0], payloadParts[1]);
    }

    private String read0(ObjectAddress objectAddress) throws IOException {
        if (objectAddress.filename().equals(this.currentlyActiveLogFile.getName())) {
            return findInCurrentFile(objectAddress.offset(), objectAddress.payloadSize());
        }
        return findInOtherFiles(objectAddress);
    }

    // Returns key:value
    private String findInCurrentFile(long offset, long valueSize) throws IOException {
        return findInFile(this.currentlyActiveLogFileReader, offset, valueSize);
    }

    // Returns key:value
    private String findInOtherFiles(ObjectAddress objectAddress) throws IOException {
        RandomAccessFile fileReader = new RandomAccessFile(this.baseDirectory.getPath() + "/" + objectAddress.filename(), "r");
        return findInFile(fileReader, objectAddress.offset(), objectAddress.payloadSize());
    }

    private String findInFile(RandomAccessFile fileReader, long offset, long valueSize) throws IOException {
        fileReader.seek(offset);
        StringBuilder stringBuilder = new StringBuilder();
        for (int index = 0; index < valueSize; index++) {
            stringBuilder.append((char) fileReader.read());
        }
        return stringBuilder.toString();
    }

    // Writes key:value and returns starting position of key:value
    @Override
    public ObjectAddress write(Payload payload) throws IOException {
        createNewActiveFileIfSizeThresholdExceeded();
        long offset = this.size();
        this.currentlyActiveLogFileWriter.write("%s:%s".formatted(payload.key(), payload.value()));
        this.currentlyActiveLogFileWriter.flush();
        return new ObjectAddress(this.currentlyActiveLogFile.getName(), offset, payload.key().length() + 1 + payload.value().length());
    }

    private void createNewActiveFileIfSizeThresholdExceeded() {
        if (this.currentlyActiveLogFile.length() >= FILE_SIZE_THRESHOLD) {
            int logFileCounter = getFileCounter(this.currentlyActiveLogFile.getName(), LOG_FILE_NAME);
            int hintFileCounter = getFileCounter(this.currentlyActiveHintFile.getName(), HINT_FILE_NAME);
            assert logFileCounter == hintFileCounter;
            int newCounter = logFileCounter + 1;
            createActiveLogFile(newCounter);
            createActiveHintFile(newCounter);
        }
    }

    private void createActiveLogFile(int counter) {
        try {
            this.currentlyActiveLogFile = createActiveFile(baseDirectory, LOG_FILE_NAME_FUNCTION.apply(counter));
            this.currentlyActiveLogFileReader = new RandomAccessFile(this.currentlyActiveLogFile, "r");
            this.currentlyActiveLogFileWriter = new FileWriter(this.currentlyActiveLogFile);
        } catch (IOException e) {
            System.err.println("Failed to replace currently active file with new one.");
            System.err.println(e);
        }
    }

    private void createActiveHintFile(int counter) {
        try {
            this.currentlyActiveHintFile = createActiveFile(baseDirectory, HINT_FILE_NAME_FUNCTION.apply(counter));
            this.currentlyActiveHintFileReader = new Scanner(this.currentlyActiveHintFile);
            this.currentlyActiveHintFileWriter = new FileWriter(this.currentlyActiveHintFile);
        } catch (IOException e) {
            System.err.println("Failed to replace currently active file with new one.");
            System.err.println(e);
        }
    }

    private void deleteActiveHintFile() {
        try {
            this.currentlyActiveHintFileReader.close();
            this.currentlyActiveHintFileWriter.close();
            this.currentlyActiveHintFile.delete();
        } catch (Exception e) {
            System.err.println("Could not delete active hint file.");
            ;
        }
    }

    private long size() {
        return this.currentlyActiveLogFile.length();
    }

    @Override
    public List<Hint> fetchAllHints() {
        return Arrays.stream(this.baseDirectory.list())
                .sorted()
                .filter(file -> file.startsWith(HINT_FILE_NAME))
                .map(filename -> this.baseDirectory.getAbsoluteFile() + "/" + filename)
                .map(Path::of)
                .map(tryFunction(path -> Files.readAllLines(path)))
                .map(lines -> String.join("\n", lines))
                .map(Hint::of)
                .toList();
    }

    @Override
    public void flush(Hint hint) {
        int counter = getFileCounter(this.currentlyActiveHintFile.getName(), HINT_FILE_NAME);
        deleteActiveHintFile();
        createActiveHintFile(counter);
        try {
            this.currentlyActiveHintFileWriter.write(hint.toString());
            this.currentlyActiveHintFileWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        try {
            this.currentlyActiveLogFileReader.close();
            this.currentlyActiveLogFileWriter.close();
            this.currentlyActiveLogFile.delete();
            this.currentlyActiveHintFileReader.close();
            this.currentlyActiveHintFileWriter.close();
            this.currentlyActiveHintFile.delete();
            this.baseDirectory.delete();
        } catch (Exception e) {
            System.err.println("Failed to close the active streams and delete database.");
        }
    }
}
