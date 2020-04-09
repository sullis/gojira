/*
 * Copyright 2020 Flipkart Internet, pvt ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flipkart.gojira.serde.handlers.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.impl.StdSubtypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flipkart.gojira.serde.TestSerdeException;
import com.flipkart.gojira.serde.handlers.TestSerdeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Standard Implementation of {@link TestSerdeHandler}
 */
public class JsonStdSerdeHandler implements TestSerdeHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonStdSerdeHandler.class);
    private static final SimpleModule SIMPLE_MODULE = new SimpleModule();
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
            .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
            .enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .setSubtypeResolver(new StdSubtypeResolver())
            .registerModule(SIMPLE_MODULE
                    .addSerializer(List.class, new TestListSerializer())
                    .addDeserializer(List.class, new TestListDeserializer())
                    .addSerializer(Map.class, new TestMapSerializer())
                    .addDeserializer(Map.class, new TestMapDeserializer()));

    /**
     * @param obj object to be serialized
     * @param <T>
     * @return
     * @throws TestSerdeException
     */
    @Override
    public <T> byte[] serialize(T obj) throws TestSerdeException {
        try {
            return MAPPER.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            LOGGER.error("error serializing data.", e);
            throw new TestSerdeException("error serializing data.", e);
        }
    }

    /**
     * @param bytes serialized byte[] to be de-serialized.
     * @param clazz class to de-serialize
     * @param <T>
     * @return
     * @throws TestSerdeException
     */
    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) throws TestSerdeException {
        try {
            if (List.class.isAssignableFrom(clazz)) {
                return MAPPER.readValue(bytes, (Class<T>) List.class);
            }
            if (Map.class.isAssignableFrom(clazz)) {
                return MAPPER.readValue(bytes, (Class<T>) Map.class);
            }
            return MAPPER.readValue(bytes, clazz);
        } catch (IOException e) {
            LOGGER.error("error de-serializing data. class: " + clazz.toGenericString(), e);
            throw new TestSerdeException("error de-serializing data. class: " + clazz.toGenericString(),
                    e);
        }
    }

    /**
     * @param bytes serialized byte[] to be de-serialized
     * @param obj   object which needs to be updated with the above byte[]
     * @param <T>
     * @throws TestSerdeException
     */
    @Override
    public <T> void deserializeToInstance(byte[] bytes, T obj) throws TestSerdeException {
        try {
            if (List.class.isAssignableFrom(obj.getClass())) {
                ((List) obj).clear();
                List tmpList = MAPPER.readValue(bytes, List.class);
                ((List) obj).addAll(tmpList);
            } else if (Map.class.isAssignableFrom(obj.getClass())) {
                ((Map) obj).clear();
                Map tmpMap = MAPPER.readValue(bytes, Map.class);
                ((Map) obj).putAll(tmpMap);
            } else MAPPER.readerForUpdating(obj).readValue(bytes);
        } catch (IOException e) {
            throw new TestSerdeException("error updating object.", e);
        }

    }


    /**
     * @param type The class against which a custom serializer is to be added
     * @param ser  Custom serializer for <code>type</code>
     * @param <T>
     */
    public synchronized static <T> void registerSerializer(Class<T> type, JsonSerializer<T> ser) {
        SIMPLE_MODULE.addSerializer(type, ser);
        MAPPER.registerModule(SIMPLE_MODULE);
    }

    /**
     * @param type  The class against which a custom deserializer is to be added
     * @param deser Custom serializer for <code>type</code>
     * @param <T>
     */
    public synchronized static <T> void registerDeSerializer(Class<T> type,
                                                             JsonDeserializer<T> deser) {
        SIMPLE_MODULE.addDeserializer(type, deser);
        MAPPER.registerModule(SIMPLE_MODULE);
    }

    /**
     * Custom Serializer for List
     */
    public static class TestListSerializer extends JsonSerializer<List> {

        @Override
        public void serialize(List value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            String listType = value.getClass().getName();
            gen.writeStartArray();
            gen.writeString(listType);
            if (!value.isEmpty()) {
                for (Object element : value) {
                    if (element != null) {
                        gen.writeObject(element.getClass().getName());
                        gen.writeObject(element);
                    }
                }
            }
            gen.writeEndArray();
        }
    }

    /**
     * Custom Deserializer for List
     */
    public static class TestListDeserializer extends JsonDeserializer<List> {

        @Override
        public List deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException, JsonProcessingException {
            ObjectCodec oc = p.getCodec();
            JsonNode jsonNode = oc.readTree(p);
            ArrayNode arrayNode = (ArrayNode) jsonNode;
            String listType = (arrayNode.get(0)).asText();
            List list;
            try {
                list = (List) Class.forName((arrayNode.get(0)).asText()).newInstance();
            } catch (Exception e) {
                LOGGER.error("Error creating new list of type " + listType + " in JsonStdSerdeHandler. ", e);
                throw new IOException("Error creating new list of type " + listType + " in JsonStdSerdeHandler. ", e);
            }
            if (arrayNode.get(1) == null) {
                return list;
            }

            arrayNode.remove(0);

            try {
                boolean itemType = true;
                String listItemType = null;
                for (int i = 0; i < arrayNode.size(); i++) {
                    if (itemType) {
                        listItemType = arrayNode.get(i).asText();
                        itemType = false;
                    } else {
                        list.add(List.class.isAssignableFrom(Class.forName(listItemType)) ?
                                MAPPER.readValue(arrayNode.get(i).toString().getBytes(), List.class) :
                                Map.class.isAssignableFrom(Class.forName(listItemType)) ?
                                        MAPPER.readValue(arrayNode.get(i).toString().getBytes(), Map.class) :
                                        MAPPER.readValue(arrayNode.get(i).toString().getBytes(),
                                                Class.forName(listItemType)));
                        itemType = true;
                    }
                }
                return list;
            } catch (ClassNotFoundException e) {
                throw new IOException("ClassNotFoundException exception", e);
            }
        }
    }

    /**
     * Custom Serializer for Map
     */
    public static class TestMapSerializer extends JsonSerializer<Map> {

        @Override
        public void serialize(Map value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException, JsonProcessingException {
            String mapType = value.getClass().getName();
            gen.writeStartArray();
            gen.writeStartObject();
            gen.writeStringField("TestMapSerializer|mapType", mapType);
            gen.writeEndObject();
            if (!value.isEmpty()) {
                int size = value.size();
                for (int i = 0; i < size; i++) {
                    if (value.keySet().toArray()[i] != null
                            && value.get(value.keySet().toArray()[i]) != null) {
                        gen.writeStartObject();
                        gen.writeStringField("TestMapSerializer|mapElementKeyType",
                                value.keySet().toArray()[i].getClass().getName());
                        gen.writeEndObject();
                        gen.writeStartObject();
                        gen.writeStringField("TestMapSerializer|mapElementValueType",
                                value.get(value.keySet().toArray()[i]).getClass().getName());
                        gen.writeEndObject();
                        gen.writeStartObject();
                        gen.writeObjectField(MAPPER.writeValueAsString(value.keySet().toArray()[i]),
                                value.get(value.keySet().toArray()[i]));
                        gen.writeEndObject();
                    }
                }
            }
            gen.writeEndArray();
        }
    }

    /**
     * Custom Deserializer for Map
     */
    public static class TestMapDeserializer extends JsonDeserializer<Map> {

        @Override
        public Map deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException, JsonProcessingException {
            // maps get serialized as ArrayNode of ObjectNodes.
            ObjectCodec oc = p.getCodec();
            ArrayNode arrayNode = oc.readTree(p);
            String mapType =
                    arrayNode.get(0) != null ? arrayNode.get(0).get("TestMapSerializer|mapType").asText()
                            : null;
            if (mapType == null) {
                return new HashMap<>();
            }
            arrayNode.remove(0);
            Map map;
            try {
                map = (Map) Class.forName(mapType).newInstance();
            } catch (Exception e) {
                LOGGER.error("Error creating new map of type " + mapType + " in JsonStdSerdeHandler. ", e);
                throw new IOException("Error creating new map of type " + mapType + " in JsonStdSerdeHandler. ", e);
            }
            if (arrayNode.size() == 0) {
                return map;
            }

            try {
                Iterator<JsonNode> elements = arrayNode.elements();
                String mapKeyType = null;
                String mapValueType = null;
                boolean mapKey = true;
                boolean mapValue = false;
                while (elements.hasNext()) {
                    ObjectNode element = (ObjectNode) elements.next();
                    Iterator<String> keys = element.fieldNames();
                    if (mapKey || mapValue) {
                        if (mapKey) {
                            mapKeyType = element.get("TestMapSerializer|mapElementKeyType").asText();
                            mapKey = false;
                            mapValue = true;
                        } else {
                            mapValueType = element.get("TestMapSerializer|mapElementValueType").asText();
                            mapValue = false;
                        }

                    } else {
                        while (keys.hasNext()) {
                            String key = keys.next();
                            map.put(Map.class.isAssignableFrom(Class.forName(mapKeyType)) ?
                                            MAPPER.readValue(key.getBytes(), Map.class) :
                                            List.class.isAssignableFrom(Class.forName(mapKeyType)) ?
                                                    MAPPER.readValue(key.getBytes(), List.class) :
                                                    MAPPER.readValue(key.getBytes(), Class.forName(mapKeyType)),

                                    Map.class.isAssignableFrom(Class.forName(mapValueType)) ?
                                            MAPPER.readValue(element.get(key).toString().getBytes(), Map.class) :
                                            List.class.isAssignableFrom(Class.forName(mapValueType)) ?
                                                    MAPPER.readValue(element.get(key).toString().getBytes(), List.class) :
                                                    MAPPER.readValue(element.get(key).toString().getBytes(),
                                                            Class.forName(mapValueType)));
                        }
                        mapKey = true;
                    }
                }
                return map;
            } catch (ClassNotFoundException e) {
                throw new IOException("class cast exception", e);
            }
        }
    }

}