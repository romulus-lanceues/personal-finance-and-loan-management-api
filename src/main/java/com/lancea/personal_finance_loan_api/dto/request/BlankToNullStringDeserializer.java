package com.lancea.personal_finance_loan_api.dto.request;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

public class BlankToNullStringDeserializer extends StdDeserializer<String> {

    public BlankToNullStringDeserializer(){
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser jsonParser, DeserializationContext context){

        String value = jsonParser.getValueAsString();
        if(value == null) return null;
        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }
}
