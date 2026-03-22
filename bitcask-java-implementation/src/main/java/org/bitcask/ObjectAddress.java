package org.bitcask;

public record ObjectAddress(String filename, long offset, long objectSize) {}
