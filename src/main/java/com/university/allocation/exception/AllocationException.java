package com.university.allocation.exception;

/** Thrown when the supplied input can't be parsed or processed. */
public class AllocationException extends RuntimeException {
    public AllocationException(String message) {
        super(message);
    }
}
