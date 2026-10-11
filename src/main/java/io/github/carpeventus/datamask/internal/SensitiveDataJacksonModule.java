package io.github.carpeventus.datamask.internal;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
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
            Map<String, SensitiveDataType> maskable = new HashMap<>();
            Map<String, String> misconfigured = new HashMap<>();
            collectAnnotatedFields(beanDesc, maskable, misconfigured);
            if (maskable.isEmpty() && misconfigured.isEmpty()) {
                return beanProperties;
            }
            for (BeanPropertyWriter writer : beanProperties) {
                String failure = misconfigured.get(writer.getName());
                if (failure != null) {
                    // 不在构建期抛错：Spring 的 canWrite 会吞掉构建期异常并回退到未注册本模块的转换器，
                    // 导致字段被悄悄原样输出；改为在真实写出该字段时抛错，保证配置错误以 500 暴露。
                    writer.assignSerializer(asObjectSerializer(new MisconfiguredFieldSerializer(failure)));
                    continue;
                }
                SensitiveDataType type = maskable.get(writer.getName());
                if (type != null) {
                    writer.assignSerializer(asObjectSerializer(new SensitiveStringSerializer(type)));
                }
            }
            return beanProperties;
        }

        private void collectAnnotatedFields(BeanDescription beanDesc, Map<String, SensitiveDataType> maskable,
                Map<String, String> misconfigured) {
            for (BeanPropertyDefinition definition : beanDesc.findProperties()) {
                AnnotatedField field = definition.getField();
                if (field == null) {
                    continue;
                }
                SensitiveData annotation = field.getAnnotation(SensitiveData.class);
                if (annotation == null) {
                    continue;
                }
                if (field.getRawType() != String.class) {
                    misconfigured.put(definition.getName(), String.format(
                            "字段 %s.%s 标注了 @SensitiveData，但其类型为 %s；@SensitiveData 仅支持 String 字段。",
                            beanDesc.getBeanClass().getName(), field.getName(), field.getRawType().getTypeName()));
                    continue;
                }
                maskable.put(definition.getName(), annotation.type());
            }
        }

        @SuppressWarnings("unchecked")
        private static JsonSerializer<Object> asObjectSerializer(JsonSerializer<?> serializer) {
            return (JsonSerializer<Object>) serializer;
        }
    }

    /**
     * 误标字段的序列化器：字段被真实写出时抛出清晰异常（见 {@link SensitiveDataSerializerModifier} 中的说明）。
     */
    private static final class MisconfiguredFieldSerializer extends StdSerializer<Object> {

        private static final long serialVersionUID = 1L;

        private final String failure;

        private MisconfiguredFieldSerializer(String failure) {
            super(Object.class);
            this.failure = failure;
        }

        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) {
            throw new IllegalStateException(failure);
        }
    }
}
