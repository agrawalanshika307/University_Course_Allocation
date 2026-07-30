package com.university.allocation.dto;

/**
 * Response body for POST /api/allocate.
 * Same content as the original output.txt, just sent as JSON instead of a file.
 */
public class AllocationResponse {

    private String result;

    public AllocationResponse() {
    }

    public AllocationResponse(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
