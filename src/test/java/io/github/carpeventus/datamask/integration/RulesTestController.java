package io.github.carpeventus.datamask.integration;

import io.github.carpeventus.datamask.annotation.SensitiveApi;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 脱敏规则集成测试用的 HTTP 接口：联系方式、证件地址、边界值、非 String 误标与非 JSON 响应。
 */
@RestController
public class RulesTestController {

    @SensitiveApi
    @GetMapping("/sensitive/rules/contacts")
    public TestFixtures.ContactView contacts() {
        return TestFixtures.sampleContacts();
    }

    @SensitiveApi
    @GetMapping("/sensitive/rules/documents")
    public TestFixtures.DocumentView documents() {
        return TestFixtures.sampleDocuments();
    }

    @SensitiveApi
    @GetMapping("/sensitive/rules/edges")
    public TestFixtures.EdgeCaseView edges() {
        return TestFixtures.sampleEdgeCases();
    }

    @SensitiveApi
    @GetMapping("/sensitive/rules/bad-field")
    public TestFixtures.BadFieldView badField() {
        return new TestFixtures.BadFieldView(1);
    }

    @SensitiveApi
    @GetMapping(value = "/sensitive/rules/plain-text", produces = MediaType.TEXT_PLAIN_VALUE)
    public String plainText() {
        return "dgs@qq.com";
    }
}
