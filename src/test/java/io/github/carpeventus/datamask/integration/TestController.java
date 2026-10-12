package io.github.carpeventus.datamask.integration;

import io.github.carpeventus.datamask.annotation.SensitiveApi;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

/**
 * 集成测试用的 HTTP 接口：一个标注 {@code @SensitiveApi}、一个未标注、一个回显 POST 请求体。
 */
@RestController
public class TestController {

    @SensitiveApi
    @GetMapping("/sensitive/order")
    public TestFixtures.OrderView sensitiveOrder() {
        return sampleOrder();
    }

    @GetMapping("/plain/order")
    public TestFixtures.OrderView plainOrder() {
        return sampleOrder();
    }

    @SensitiveApi
    @PostMapping("/sensitive/echo")
    public TestFixtures.EchoView echo(@RequestBody TestFixtures.EchoRequest request) {
        return new TestFixtures.EchoView(request.getValue(), request.getValue());
    }

    private TestFixtures.OrderView sampleOrder() {
        return new TestFixtures.OrderView(
                "13812345678",
                "Sam",
                new TestFixtures.AddressView("dgs@qq.com", "Hangzhou"),
                Collections.singletonList(new TestFixtures.CardView("6222020200112233", "ICBC")));
    }
}
