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
import io.github.carpeventus.datamask.DataMaskCipherService;
import io.github.carpeventus.datamask.annotation.SensitiveData;
import io.github.carpeventus.datamask.annotation.SensitiveDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Jackson 模块：把标注 {@code @SensitiveData} 的 String 字段替换为脱敏序列化器；
 * {@code addCipherField = true} 的字段在命中脱敏标记时还会在同层级写出 {@code <字段名>Cipher} 密文字段。
 *
 * <p>替换发生在序列化器构建期（按类缓存），命中标记的请求里所有深度、含 List 内元素的标注字段
 * 都会经由此模块输出；未命中标记的请求沿用默认 String 序列化，输出与引入 SDK 前一致。
 */
public class SensitiveDataJacksonModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    public SensitiveDataJacksonModule(DataMaskCipherService cipherService) {
        super("data-mask");
        setSerializerModifier(new SensitiveDataSerializerModifier(cipherService));
    }

    private static final class SensitiveDataSerializerModifier extends BeanSerializerModifier {

        private final DataMaskCipherService cipherService;

        private SensitiveDataSerializerModifier(DataMaskCipherService cipherService) {
            this.cipherService = cipherService;
        }

        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc,
                List<BeanPropertyWriter> beanProperties) {
            Map<String, FieldRule> rules = new HashMap<>();
            Map<String, String> misconfigured = new HashMap<>();
            collectAnnotatedFields(beanDesc, rules, misconfigured);
            if (rules.isEmpty() && misconfigured.isEmpty()) {
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
                FieldRule rule = rules.get(writer.getName());
                if (rule == null) {
                    continue;
                }
                if (rule.addCipherField) {
                    beanProperties.set(beanProperties.indexOf(writer),
                            new CipherFieldWriter(writer, rule.type, cipherService));
                } else {
                    writer.assignSerializer(asObjectSerializer(new SensitiveStringSerializer(rule.type)));
                }
            }
            return beanProperties;
        }

        private void collectAnnotatedFields(BeanDescription beanDesc, Map<String, FieldRule> rules,
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
                rules.put(definition.getName(), new FieldRule(annotation.type(), annotation.addCipherField()));
            }
        }

        @SuppressWarnings("unchecked")
        private static JsonSerializer<Object> asObjectSerializer(JsonSerializer<?> serializer) {
            return (JsonSerializer<Object>) serializer;
        }
    }

    /**
     * 单个标注字段的规则：脱敏类型与是否附带密文字段。
     */
    private static final class FieldRule {

        private final SensitiveDataType type;
        private final boolean addCipherField;

        private FieldRule(SensitiveDataType type, boolean addCipherField) {
            this.type = type;
            this.addCipherField = addCipherField;
        }
    }

    /**
     * {@code addCipherField = true} 的字段写出器：命中标记且值非空时写出脱敏值 +
     * {@code <字段名>Cipher} 密文；否则回退默认写出（未标注接口输出与原样一致，null/空值不产生密文字段）。
     */
    private static final class CipherFieldWriter extends BeanPropertyWriter {

        private static final long serialVersionUID = 1L;

        private final SensitiveDataType type;
        private final DataMaskCipherService cipherService;
        private final String cipherFieldName;

        private CipherFieldWriter(BeanPropertyWriter base, SensitiveDataType type, DataMaskCipherService cipherService) {
            super(base);
            this.type = type;
            this.cipherService = cipherService;
            this.cipherFieldName = base.getName() + "Cipher";
        }

        @Override
        public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {
            Object value = get(bean);
            if (!(value instanceof String) || ((String) value).isEmpty() || !MaskRequestFlag.isActive()) {
                super.serializeAsField(bean, gen, prov);
                return;
            }
            String plaintext = (String) value;
            gen.writeFieldName(getName());
            gen.writeString(MaskingRules.mask(type, plaintext));
            gen.writeFieldName(cipherFieldName);
            gen.writeString(cipherService.encrypt(plaintext));
        }

        @Override
        public void serializeAsElement(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {
            Object value = get(bean);
            if (value instanceof String && !((String) value).isEmpty() && MaskRequestFlag.isActive()) {
                gen.writeString(MaskingRules.mask(type, (String) value));
                return;
            }
            super.serializeAsElement(bean, gen, prov);
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
