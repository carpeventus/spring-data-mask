package io.github.carpeventus.datamask.guardrail;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ADR-0004 守卫：主代码不得引用 servlet 命名空间（javax.servlet / jakarta.servlet），
 * 以保证单一构件同时适配 Boot 2 与 Boot 3。
 *
 * <p>扫描编译产物（target/classes）的常量池：任何真实引用都会留下命名空间字符串；
 * 注释与 Javadoc 不进入 class 文件，因此不会误报。
 */
class ServletNamespaceGuardTest {

    private static final String[] FORBIDDEN = {"javax/servlet", "jakarta/servlet"};

    @Test
    void mainClassesDoNotReferenceServletNamespaces() throws IOException {
        Path classes = Paths.get("target", "classes");
        assertThat(Files.isDirectory(classes)).as("target/classes 应存在（mvn test 会先编译）").isTrue();

        List<String> offenders = new ArrayList<>();
        List<Path> classFiles;
        try (Stream<Path> files = Files.walk(classes)) {
            classFiles = files.filter(path -> path.toString().endsWith(".class")).collect(Collectors.toList());
        }
        for (Path classFile : classFiles) {
            byte[] bytes = Files.readAllBytes(classFile);
            for (String forbidden : FORBIDDEN) {
                if (indexOf(bytes, forbidden.getBytes(StandardCharsets.US_ASCII)) >= 0) {
                    offenders.add(classes.relativize(classFile) + " 引用了 " + forbidden);
                }
            }
        }
        assertThat(offenders).isEmpty();
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i + needle.length <= haystack.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
