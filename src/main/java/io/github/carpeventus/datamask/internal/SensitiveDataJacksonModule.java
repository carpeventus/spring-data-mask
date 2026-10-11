package io.github.carpeventus.datamask.internal;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import io.github.carpeventus.datamask.annotation.SensitiveData;
import io.github.carpeventus.datamask.annotation.SensitiveDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Jackson 模块：把标注 {@code @SensitiveData} 的 String 字段替换为脱敏序列化器。
 *
 * <p>替换发生在序列化器构建期（按类缓存），命中标记的请求里所有深度、含 List 内元素的标注字段
 * 都会经由 {@link SensitiveStringSerializer} 输出。
 */
public class SensitiveDataJacksonModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    public SensitiveDataJacksonModule() {
        super("data-mask");
        setSerializerModifier(new SensitiveDataSerializerModifier());
    }

    private static final class SensitiveDataSerializerModifier extends BeanSerializerModifier {

        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc,
                List<BeanPropertyWriter> beanProperties) {
            Map<String, SensitiveDataType> annotated = collectAnnotatedStringFields(beanDesc);
            if (annotated.isEmpty()) {
                return beanProperties;
            }
            for (BeanPropertyWriter writer : beanProperties) {
                SensitiveDataType type = annotated.get(writer.getName());
                if (type != null) {
                    writer.assignSerializer(asObjectSerializer(new SensitiveStringSerializer(type)));
                }
            }
            return beanProperties;
        }

        private Map<String, SensitiveDataType> collectAnnotatedStringFields(BeanDescription beanDesc) {
            Map<String, SensitiveDataType> annotated = new HashMap<>();
            for (BeanPropertyDefinition definition : beanDesc.findProperties()) {
                AnnotatedField field = definition.getField();
                if (field == null || field.getRawType() != String.class) {
                    continue;
                }
                SensitiveData annotation = field.getAnnotation(SensitiveData.class);
                if (annotation != null) {
                    annotated.put(definition.getName(), annotation.type());
                }
            }
            return annotated;
        }

        @SuppressWarnings("unchecked")
        private static JsonSerializer<Object> asObjectSerializer(JsonSerializer<?> serializer) {
            return (JsonSerializer<Object>) serializer;
        }
    }
}
