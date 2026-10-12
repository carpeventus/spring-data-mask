package io.github.carpeventus.datamask.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Seam 1（HTTP 集成）：卡证、地址、嵌套/List 覆盖、边界值与误标报错。
 */
@SpringBootTest
@AutoConfigureMockMvc
class MaskingEdgeBehaviorsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void masksBankCardAndIdCardKeepingFirstSixAndLastFour() throws Exception {
        mockMvc.perform(get("/sensitive/rules/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bankCard").value("622202******2233"))
                .andExpect(jsonPath("$.idCard").value("330100********1234"));
    }

    @Test
    void masksOnlyAddressDoorplateNumbers() throws Exception {
        mockMvc.perform(get("/sensitive/rules/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("浙江省杭州市西湖区文一西路**号**栋**单元**室"))
                .andExpect(jsonPath("$.buildingAddress").value("幸福小区**号楼**单元**室"));
    }

    @Test
    void masksAnnotatedFieldsInsideNestedObjectsAndListElements() throws Exception {
        mockMvc.perform(get("/sensitive/rules/contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nested.email").value("ro**@qq.com"))
                .andExpect(jsonPath("$.nested.phone").value("139****1111"))
                .andExpect(jsonPath("$.items[0].email").value("in*****@qq.com"))
                .andExpect(jsonPath("$.items[0].phone").value("139****2222"))
                .andExpect(jsonPath("$.items[1].email").value("se****@qq.com"))
                .andExpect(jsonPath("$.items[1].phone").value("010-****5678"));
    }

    @Test
    void passesNullAndEmptyValuesThroughUnchanged() throws Exception {
        mockMvc.perform(get("/sensitive/rules/edges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nullEmail").doesNotExist())
                .andExpect(content().json("{\"emptyEmail\":\"\"}", false));
    }

    @Test
    void fullyMasksNonMatchingNonEmptyValues() throws Exception {
        mockMvc.perform(get("/sensitive/rules/edges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notAnEmail").value("********"))
                .andExpect(jsonPath("$.tooShortPhone").value("********"))
                .andExpect(jsonPath("$.notAPhone").value("********"))
                .andExpect(jsonPath("$.tooShortCard").value("********"))
                .andExpect(jsonPath("$.addressWithoutDoorplate").value("********"));
    }

    @Test
    void rejectsSensitiveDataOnNonStringFieldWithClearError() throws Exception {
        MvcResult result = mockMvc.perform(get("/sensitive/rules/bad-field"))
                .andExpect(status().isInternalServerError())
                .andReturn();

        Throwable failure = result.getResolvedException();
        assertThat(failure).isNotNull();
        assertThat(messagesOf(failure))
                .contains("@SensitiveData")
                .contains("count");
    }

    @Test
    void leavesNonJsonResponsesUntouched() throws Exception {
        mockMvc.perform(get("/sensitive/rules/plain-text"))
                .andExpect(status().isOk())
                .andExpect(content().string("dgs@qq.com"));
    }

    private static String messagesOf(Throwable failure) {
        StringBuilder messages = new StringBuilder();
        for (Throwable current = failure; current != null; current = current.getCause()) {
            messages.append(current.getMessage()).append('\n');
        }
        return messages.toString();
    }
}
