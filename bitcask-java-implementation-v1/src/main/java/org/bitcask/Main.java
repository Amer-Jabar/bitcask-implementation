package org.bitcask;

import org.bitcask.application.Bitcask;
import org.bitcask.application.BitcaskImplementation;

import java.io.IOException;

public class Main {
	public static void main(String[] args) throws IOException {
		Bitcask bitcask = new BitcaskImplementation("database");

        bitcask.start();

		bitcask.write("hello", "aaaa");
        bitcask.read("hell");


        bitcask.shutdown();
	}
}