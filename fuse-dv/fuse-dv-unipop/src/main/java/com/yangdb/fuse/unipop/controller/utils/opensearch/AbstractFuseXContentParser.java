package com.yangdb.fuse.unipop.controller.utils.opensearch;

/*-
 * #%L
 * fuse-dv-unipop
 * %%
 * Copyright (C) 2016 - 2019 The YangDb Graph Database Project
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.opensearch.OpenSearchParseException;
import org.opensearch.common.Booleans;
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.*;

public abstract class AbstractFuseXContentParser implements XContentParser {
    // Currently this is not a setting that can be changed and is a policy
    // that relates to how parsing of things like "boost" are done across
    // the whole of Elasticsearch (eg if String "1.0" is a valid float).
    // The idea behind keeping it as a constant is that we can track
    // references to this policy decision throughout the codebase and find
    // and change any code that needs to apply an alternative policy.
    public static final boolean DEFAULT_NUMBER_COERCE_POLICY = true;

    private static void checkCoerceString(boolean coerce, Class<? extends Number> clazz) {
        if (!coerce) {
            //Need to throw type IllegalArgumentException as current catch logic in
            //NumberFieldMapper.parseCreateField relies on this for "malformed" value detection
            throw new IllegalArgumentException(clazz.getSimpleName() + " value passed as String");
        }
    }

    private final NamedXContentRegistry xContentRegistry;

    public AbstractFuseXContentParser(NamedXContentRegistry xContentRegistry) {
        this.xContentRegistry = xContentRegistry;
    }

    // The 3rd party parsers we rely on are known to silently truncate fractions: see
    //   http://fasterxml.github.io/jackson-core/javadoc/2.3.0/com/fasterxml/jackson/core/JsonParser.html#getShortValue()
    // If this behaviour is flagged as undesirable and any truncation occurs
    // then this method is called to trigger the"malformed" handling logic
    void ensureNumberConversion(boolean coerce, long result, Class<? extends Number> clazz) throws IOException {
        if (!coerce) {
            double fullVal = doDoubleValue();
            if (result != fullVal) {
                // Need to throw type IllegalArgumentException as current catch
                // logic in NumberFieldMapper.parseCreateField relies on this
                // for "malformed" value detection
                throw new IllegalArgumentException(fullVal + " cannot be converted to " + clazz.getSimpleName() + " without data loss");
            }
        }
    }

    @Override
    public boolean isBooleanValue() throws IOException {
        switch (currentToken()) {
            case VALUE_BOOLEAN:
                return true;
            case VALUE_NUMBER:
                NumberType numberType = numberType();
                return numberType == NumberType.LONG || numberType == NumberType.INT;
            case VALUE_STRING:
                return Booleans.isBoolean(textCharacters(), textOffset(), textLength());
            default:
                return false;
        }
    }

    @Override
    public boolean booleanValue() throws IOException {
        boolean booleanValue;
        String rawValue = null;

        Token token = currentToken();
        if (token == Token.VALUE_NUMBER) {
            interpretedAsLenient = true;
            booleanValue = intValue() != 0;
            rawValue = String.valueOf(intValue());
        } else if (token == Token.VALUE_STRING) {
            rawValue = new String(textCharacters(), textOffset(), textLength());
            interpretedAsLenient = Booleans.isBoolean(rawValue) == false;
            booleanValue = Booleans.parseBoolean(rawValue, false /* irrelevant */);
        } else {
            booleanValue = doBooleanValue();
        }

        return booleanValue;

    }

    protected abstract boolean doBooleanValue() throws IOException;

    @Override
    public short shortValue() throws IOException {
        return shortValue(DEFAULT_NUMBER_COERCE_POLICY);
    }

    @Override
    public short shortValue(boolean coerce) throws IOException {
        Token token = currentToken();
        if (token == Token.VALUE_STRING) {
            checkCoerceString(coerce, Short.class);
            return Short.parseShort(text());
        }
        short result = doShortValue();
        ensureNumberConversion(coerce, result, Short.class);
        return result;
    }

    protected abstract short doShortValue() throws IOException;

    @Override
    public int intValue() throws IOException {
        return intValue(DEFAULT_NUMBER_COERCE_POLICY);
    }


    @Override
    public int intValue(boolean coerce) throws IOException {
        Token token = currentToken();
        if (token == Token.VALUE_STRING) {
            checkCoerceString(coerce, Integer.class);
            return Integer.parseInt(text());
        }
        int result = doIntValue();
        ensureNumberConversion(coerce, result, Integer.class);
        return result;
    }

    protected abstract int doIntValue() throws IOException;

    @Override
    public long longValue() throws IOException {
        return longValue(DEFAULT_NUMBER_COERCE_POLICY);
    }

    @Override
    public long longValue(boolean coerce) throws IOException {
        Token token = currentToken();
        if (token == Token.VALUE_STRING) {
            checkCoerceString(coerce, Long.class);
            return Long.parseLong(text());
        }
        long result = doLongValue();
        ensureNumberConversion(coerce, result, Long.class);
        return result;
    }

    protected abstract long doLongValue() throws IOException;

    @Override
    public float floatValue() throws IOException {
        return floatValue(DEFAULT_NUMBER_COERCE_POLICY);
    }

    @Override
    public float floatValue(boolean coerce) throws IOException {
        Token token = currentToken();
        if (token == Token.VALUE_STRING) {
            checkCoerceString(coerce, Float.class);
            return Float.parseFloat(text());
        }
        return doFloatValue();
    }

    protected abstract float doFloatValue() throws IOException;


    @Override
    public double doubleValue() throws IOException {
        return doubleValue(DEFAULT_NUMBER_COERCE_POLICY);
    }

    @Override
    public double doubleValue(boolean coerce) throws IOException {
        Token token = currentToken();
        if (token == Token.VALUE_STRING) {
            checkCoerceString(coerce, Double.class);
            return Double.parseDouble(text());
        }
        return doDoubleValue();
    }

    protected abstract double doDoubleValue() throws IOException;

    @Override
    public final String textOrNull() throws IOException {
        if (currentToken() == Token.VALUE_NULL) {
            return null;
        }
        return text();
    }


    @Override
    public Map<String, Object> map() throws IOException {
        return readMap(this);
    }

    @Override
    public Map<String, Object> mapOrdered() throws IOException {
        return readOrderedMap(this);
    }

    @Override
    public Map<String, String> mapStrings() throws IOException {
        return readMapStrings(this);
    }

    @Override
    public List<Object> list() throws IOException {
        return readList(this);
    }

    @Override
    public List<Object> listOrderedMap() throws IOException {
        return readListOrderedMap(this);
    }

    interface MapFactory {
        Map<String, Object> newMap();
    }

    interface MapStringsFactory {
        Map<String, String> newMap();
    }

    static final AbstractFuseXContentParser.MapFactory SIMPLE_MAP_FACTORY = HashMap::new;

    static final AbstractFuseXContentParser.MapFactory ORDERED_MAP_FACTORY = LinkedHashMap::new;

    static final AbstractFuseXContentParser.MapStringsFactory SIMPLE_MAP_STRINGS_FACTORY = HashMap::new;

    static final AbstractFuseXContentParser.MapStringsFactory ORDERED_MAP_STRINGS_FACTORY = LinkedHashMap::new;

    static Map<String, Object> readMap(XContentParser parser) throws IOException {
        return readMap(parser, SIMPLE_MAP_FACTORY);
    }

    static Map<String, Object> readOrderedMap(XContentParser parser) throws IOException {
        return readMap(parser, ORDERED_MAP_FACTORY);
    }

    static Map<String, String> readMapStrings(XContentParser parser) throws IOException {
        return readMapStrings(parser, SIMPLE_MAP_STRINGS_FACTORY);
    }

    static Map<String, String> readOrderedMapStrings(XContentParser parser) throws IOException {
        return readMapStrings(parser, ORDERED_MAP_STRINGS_FACTORY);
    }

    static List<Object> readList(XContentParser parser) throws IOException {
        return readList(parser, SIMPLE_MAP_FACTORY);
    }

    static List<Object> readListOrderedMap(XContentParser parser) throws IOException {
        return readList(parser, ORDERED_MAP_FACTORY);
    }

    static Map<String, Object> readMap(XContentParser parser, AbstractFuseXContentParser.MapFactory mapFactory) throws IOException {
        Map<String, Object> map = mapFactory.newMap();
        XContentParser.Token token = parser.currentToken();
        if (token == null) {
            token = parser.nextToken();
        }
        if (token == XContentParser.Token.START_OBJECT) {
            token = parser.nextToken();
        }
        for (; token == XContentParser.Token.FIELD_NAME; token = parser.nextToken()) {
            // Must point to field name
            String fieldName = parser.currentName();
            // And then the value...
            token = parser.nextToken();
            Object value = readValue(parser, mapFactory, token);
            map.put(fieldName, value);
        }
        return map;
    }

    static Map<String, String> readMapStrings(XContentParser parser, AbstractFuseXContentParser.MapStringsFactory mapStringsFactory) throws IOException {
        Map<String, String> map = mapStringsFactory.newMap();
        XContentParser.Token token = parser.currentToken();
        if (token == null) {
            token = parser.nextToken();
        }
        if (token == XContentParser.Token.START_OBJECT) {
            token = parser.nextToken();
        }
        for (; token == XContentParser.Token.FIELD_NAME; token = parser.nextToken()) {
            // Must point to field name
            String fieldName = parser.currentName();
            // And then the value...
            parser.nextToken();
            String value = parser.text();
            map.put(fieldName, value);
        }
        return map;
    }

    static List<Object> readList(XContentParser parser, AbstractFuseXContentParser.MapFactory mapFactory) throws IOException {
        XContentParser.Token token = parser.currentToken();
        if (token == null) {
            token = parser.nextToken();
        }
        if (token == XContentParser.Token.FIELD_NAME) {
            token = parser.nextToken();
        }
        if (token == XContentParser.Token.START_ARRAY) {
            token = parser.nextToken();
        } else {
            throw new OpenSearchParseException("Failed to parse list:  expecting "
                    + XContentParser.Token.START_ARRAY + " but got " + token);
        }

        ArrayList<Object> list = new ArrayList<>();
        for (; token != null && token != XContentParser.Token.END_ARRAY; token = parser.nextToken()) {
            list.add(readValue(parser, mapFactory, token));
        }
        return list;
    }

    static Object readValue(XContentParser parser, AbstractFuseXContentParser.MapFactory mapFactory, XContentParser.Token token) throws IOException {
        if (token == XContentParser.Token.VALUE_NULL) {
            return null;
        } else if (token == XContentParser.Token.VALUE_STRING) {
            return parser.text();
        } else if (token == XContentParser.Token.VALUE_NUMBER) {
            return parser.numberValue();
        } else if (token == XContentParser.Token.VALUE_BOOLEAN) {
            return parser.booleanValue();
        } else if (token == XContentParser.Token.START_OBJECT) {
            return readMap(parser, mapFactory);
        } else if (token == XContentParser.Token.START_ARRAY) {
            return readList(parser, mapFactory);
        } else if (token == XContentParser.Token.VALUE_EMBEDDED_OBJECT) {
            return parser.binaryValue();
        }
        return null;
    }

    @Override
    public <T> T namedObject(Class<T> categoryClass, String name, Object context) throws IOException {
        return xContentRegistry.parseNamedObject(categoryClass, name, this, context);
    }

    @Override
    public NamedXContentRegistry getXContentRegistry() {
        return xContentRegistry;
    }

    @Override
    public abstract boolean isClosed();

    boolean interpretedAsLenient = false;
}
