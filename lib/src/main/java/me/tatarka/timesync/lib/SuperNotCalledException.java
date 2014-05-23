package me.tatarka.timesync.lib;

public class SuperNotCalledException extends RuntimeException {
    public SuperNotCalledException(String detailMessage) {
        super(detailMessage);
    }
}
