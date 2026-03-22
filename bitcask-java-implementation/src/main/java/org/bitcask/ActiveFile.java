package org.bitcask;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Comparator;

public class ActiveFile {
	private static final String LOG_FILE_NAME = "log-file";
	private static final String HINT_FILE_NAME = "log-file";
	private static final long FILE_SIZE_THRESHOLD = 1_000_000;
	private static final String CREATION_FAILURE_ERROR_MESSAGE = "Failed to create %s.";

	private final String baseDirectoryPath;

	private File currentlyActiveFile;
	private RandomAccessFile currentlyActiveFileReader;
	private FileWriter currentlyActiveFileWriter;

	public ActiveFile(String baseDirectoryFilename) throws IOException {
		File baseDirectory = createBaseDirectoryIfNotExists(baseDirectoryFilename);
		this.baseDirectoryPath = baseDirectory.getAbsolutePath();
		this.currentlyActiveFile = Arrays.stream(baseDirectory.listFiles())
					.max(Comparator.naturalOrder())
					.map(File::getName)
					.map(fileName -> createLastActiveFile(baseDirectory, fileName))
					.orElseGet(() -> createFirstActiveFile(baseDirectory));
		this.currentlyActiveFileReader = new RandomAccessFile(this.currentlyActiveFile, "r");
		this.currentlyActiveFileWriter = new FileWriter(this.currentlyActiveFile);
	}

	private File createBaseDirectoryIfNotExists(String baseDirectory) {
		File file = new File(baseDirectory);
		if (!file.exists() && !file.mkdir()) {
			throw new RuntimeException(CREATION_FAILURE_ERROR_MESSAGE.formatted("base directory"));
		}
		return file;
	}

	private String getNewFileName(String lastFilename) {
		int counter = Integer.parseInt(lastFilename.split(LOG_FILE_NAME + "-")[1].split(".txt")[0]);
		return LOG_FILE_NAME + "-%d.txt".formatted(counter + 1);
	}

	private File createLastActiveFile(File baseDirectory, String lastFilename) {
		return createActiveFile(baseDirectory, getNewFileName(lastFilename), CREATION_FAILURE_ERROR_MESSAGE.formatted("new active file"));
	}

	private File createFirstActiveFile(File baseDirectory) {
		String filename = LOG_FILE_NAME + "-%d.txt".formatted(1);
		return createActiveFile(baseDirectory, filename, CREATION_FAILURE_ERROR_MESSAGE.formatted("new active file"));
	}

	private File createActiveFile(File baseDirectory, String filename, String errorMessage) {
		File file = new File(baseDirectory.getAbsolutePath() + "/" + filename);
		try {
			if (!file.createNewFile()) throw new RuntimeException(errorMessage);
			return file;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public String read(ObjectAddress objectAddress) throws IOException {
		return read0(objectAddress).split(":")[1];
	}

	public String read0(ObjectAddress objectAddress) throws IOException {
		if (objectAddress.filename().equals(this.currentlyActiveFile.getName())) {
			return findInCurrentFile(objectAddress.offset(), objectAddress.objectSize());
		}
		return findInOtherFiles(objectAddress);
	}

	// Returns key:value
	private String findInCurrentFile(long offset, long valueSize) throws IOException {
		return findInFile(this.currentlyActiveFileReader, offset, valueSize);
	}

	// Returns key:value
	private String findInOtherFiles(ObjectAddress objectAddress) throws IOException {
		RandomAccessFile fileReader = new RandomAccessFile(this.baseDirectoryPath + "/" + objectAddress.filename(), "r");
		return findInFile(fileReader, objectAddress.offset(), objectAddress.objectSize());
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
	public ObjectAddress write(String key, String value) throws IOException {
		replaceCurrentlyActiveFileIfSizeThresholdExceeded();
		long offset = this.size();
		this.currentlyActiveFileWriter.write("%s:%s".formatted(key, value));
		this.currentlyActiveFileWriter.flush();
		return new ObjectAddress(this.currentlyActiveFile.getName(), offset, key.length() + 1 + value.length());
	}

	private void replaceCurrentlyActiveFileIfSizeThresholdExceeded() throws IOException {
		if (this.currentlyActiveFile.length() >= FILE_SIZE_THRESHOLD) {
			this.currentlyActiveFile = createLastActiveFile(new File(this.baseDirectoryPath), this.currentlyActiveFile.getName());
			this.currentlyActiveFileReader = new RandomAccessFile(this.currentlyActiveFile, "r");
			this.currentlyActiveFileWriter = new FileWriter(this.currentlyActiveFile);
		}
	}

	private long size() {
		return this.currentlyActiveFile.length();
	}
}
