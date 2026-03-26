package org.bitcask.application;

public record ObjectAddress(String filename, long offset, long payloadSize) {}
