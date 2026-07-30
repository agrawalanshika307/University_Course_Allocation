package com.university.allocation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AllocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Same sample data as the original project's input.txt / expected_output.txt
    private static final String SAMPLE_INPUT = """
            7
            pC,0.5,0,0,1,fde1,0
            pD,1,0,0,1,fde1,1,hde1
            pB,1,0,1,hdc1,1,fde1,0
            pE,0.5,1,fdc1,1,hdc1,0,0
            pA,1,2,fdc1,fdc2,0,0,0
            pF,1,1,fdc1,0,0,0
            pG,1,1,fdc1,0,0,0
            """;

    @Test
    void allocateReturnsExpectedAssignments() throws Exception {
        String requestJson = "{\"inputText\": " + toJsonString(SAMPLE_INPUT) + "}";

        mockMvc.perform(post("/api/allocate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "pF is assigned the following:")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Full course of fdc1")));
    }

    @Test
    void allocateFileReturnsExpectedAssignments() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "input.txt",
                MediaType.TEXT_PLAIN_VALUE,
                SAMPLE_INPUT.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/allocate/file").file(file))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "pF is assigned the following:")));
    }

    @Test
    void blankInputIsRejected() throws Exception {
        mockMvc.perform(post("/api/allocate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inputText\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    private String toJsonString(String raw) {
        return "\"" + raw.replace("\\", "\\\\")
                          .replace("\"", "\\\"")
                          .replace("\n", "\\n") + "\"";
    }
}
