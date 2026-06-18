package com.nongsabu.backend.infra.external;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nongsabu.backend.domain.agri.dto.Gov24Response;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

class Gov24ClientTest {

    @Test
    void parseStructuredReadsGov24KoreanFields() {
        Gov24Client client = new Gov24Client(WebClient.builder().build(), new ObjectMapper(), "test-key");
        String json = """
                {
                  "totalCount": 1,
                  "data": [
                    {
                      "\uC11C\uBE44\uC2A4ID": "SVC-1",
                      "\uC11C\uBE44\uC2A4\uBA85": "\uCCAD\uB144\uB18D \uC601\uB18D\uC815\uCC29 \uC9C0\uC6D0",
                      "\uC11C\uBE44\uC2A4\uBAA9\uC801\uC694\uC57D": "\uCCAD\uB144 \uB18D\uC5C5\uC778\uC758 \uC815\uCC29\uC744 \uC9C0\uC6D0",
                      "\uC9C0\uC6D0\uB300\uC0C1": "\uB9CC 40\uC138 \uBBF8\uB9CC \uB18D\uC5C5\uC778",
                      "\uC11C\uBE44\uC2A4\uBD84\uC57C": "\uB18D\uB9BC\uCD95\uC0B0\uC5B4\uC5C5",
                      "\uC2E0\uCCAD\uAE30\uD55C": "\uC0C1\uC2DC",
                      "\uC2E0\uCCAD\uBC29\uBC95": "\uBC29\uBB38 \uC2E0\uCCAD",
                      "\uC18C\uAD00\uAE30\uAD00\uBA85": "\uB18D\uB9BC\uCD95\uC0B0\uC2DD\uD488\uBD80",
                      "\uC0C1\uC138\uC870\uD68CURL": "https://example.test/policy"
                    }
                  ]
                }
                """;

        Gov24Response response = ReflectionTestUtils.invokeMethod(client, "parseStructured", json, 1, 10);

        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.items()).hasSize(1);
        Gov24Response.Item item = response.items().get(0);
        assertThat(item.serviceId()).isEqualTo("SVC-1");
        assertThat(item.serviceName()).isEqualTo("\uCCAD\uB144\uB18D \uC601\uB18D\uC815\uCC29 \uC9C0\uC6D0");
        assertThat(item.targetGroup()).isEqualTo("\uB9CC 40\uC138 \uBBF8\uB9CC \uB18D\uC5C5\uC778");
        assertThat(item.detailUrl()).isEqualTo("https://example.test/policy");
    }
}
