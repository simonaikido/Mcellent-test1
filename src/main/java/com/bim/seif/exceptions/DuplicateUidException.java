package com.bim.seif.exceptions;

public class DuplicateUidException extends RuntimeException {
    private final String uid;
    public DuplicateUidException(String uid) {
        super("Ya existe uid=" + uid);
        this.uid = uid;
    }
    public String getUid() { return uid; }
}
