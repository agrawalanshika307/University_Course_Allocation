package com.university.allocation.controller;

import com.university.allocation.dto.AllocationRequest;
import com.university.allocation.dto.AllocationResponse;
import com.university.allocation.exception.AllocationException;
import com.university.allocation.service.CourseAllocationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
public class AllocationController {

    private final CourseAllocationService service;

    public AllocationController(CourseAllocationService service) {
        this.service = service;
    }

    /**
     * POST /api/allocate
     * Body:  { "inputText": "<contents of input.txt>" }
     * Reply: { "result": "<contents of output.txt>" }
     */
    @PostMapping("/api/allocate")
    public AllocationResponse allocate(@Valid @RequestBody AllocationRequest request) {
        String result = service.runAllocation(request.getInputText());
        return new AllocationResponse(result);
    }

    /**
     * POST /api/allocate/file
     * Body:  multipart/form-data, field name "file" = your input.txt (picked straight
     *        off disk, no manual JSON escaping needed)
     * Reply: plain text body = contents of output.txt, sent back as a downloadable
     *        "output.txt" attachment
     */
    @PostMapping(value = "/api/allocate/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> allocateFile(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new AllocationException("Uploaded file is empty");
        }
        String inputText = new String(file.getBytes(), StandardCharsets.UTF_8);
        String result = service.runAllocation(inputText);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"output.txt\"")
                .body(result);
    }
}
