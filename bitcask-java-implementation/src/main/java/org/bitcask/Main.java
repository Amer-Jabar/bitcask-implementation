package org.bitcask;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
	public static void main(String[] args) throws IOException {

		Bitcask bitcask = new BitcaskImplementation("/Users/amermuhammed/Desktop/Coding/study/databases/bitcask-implementation/disk");

		String value = Stream.generate(() -> "aaa").limit(100_000).collect(Collectors.joining(""));

		bitcask.write("hello", value);
		bitcask.write("hello", value);
		bitcask.write("hello", value);
		bitcask.write("hello", value);
		bitcask.write("hello", value);
		bitcask.write("hello", value);
		bitcask.write("hello", value);
		bitcask.write("hello", value);
		bitcask.write("hello", value);
		bitcask.write("hello", value);
		bitcask.write("hello", value);
		bitcask.write("hello", value);
		bitcask.write("hello", value);
		bitcask.write("hello", value);
		bitcask.write("hello", value);
		System.out.println("");
	}
}