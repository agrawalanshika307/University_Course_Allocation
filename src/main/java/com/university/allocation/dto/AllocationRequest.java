package com.university.allocation.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /api/allocate.
 * Same content as the original input.txt, just sent as JSON instead of a file.
 */
public class AllocationRequest {

    @NotBlank(message = "inputText must not be blank")
    private String inputText;

    public AllocationRequest() {
    }

    public String getInputText() {
        return inputText;
    }

    public void setInputText(String inputText) {
        this.inputText = inputText;
    }
}
