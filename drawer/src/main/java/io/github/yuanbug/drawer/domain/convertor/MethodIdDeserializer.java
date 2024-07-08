package io.github.yuanbug.drawer.domain.convertor;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import io.github.yuanbug.drawer.domain.info.MethodId;

import java.io.IOException;

/**
 * @author yuanbug
 */
public class MethodIdDeserializer extends JsonDeserializer<MethodId> {

    @Override
    public MethodId deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        return MethodId.parse(jsonParser.getText());
    }

}
