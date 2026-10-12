package io.github.carpeventus.datamask.internal;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.github.carpeventus.datamask.annotation.SensitiveDataType;

import java.io.IOException;

/**
 * 标注 {@code @SensitiveData} 的 String 字段的序列化器：命中脱敏标记时输出脱敏值。
 *
 * <p>null 与空串原样输出；未命中标记时原样输出。
 */
public class SensitiveStringSerializer extends StdSerializer<String> {

    private static final long serialVersionUID = 1L;

    private final SensitiveDataType type;

    public SensitiveStringSerializer(SensitiveDataType type) {
        super(String.class);
        this.type = type;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        if (value.isEmpty() || !MaskRequestFlag.isActive()) {
            gen.writeString(value);
            return;
        }
        gen.writeString(MaskingRules.mask(type, value));
    }
}
