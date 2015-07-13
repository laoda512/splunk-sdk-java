/*
 * Copyright 2012 Splunk, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"): you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.splunk;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.splunk.HttpService.OutputMode;

/**
 * The {@code AtomEntry} class represents an Atom {@code <entry>} element.
 * <p/>
 * Edited by cwang.
 * <p/>
 * support some endpoints like properties/web/settings which do not have key-value pair in 'content'.
 * just put raw data into the Map called "raw_data"
 */
public class AtomEntry extends AtomObject {
    /**
     * The value of the Atom entry's {@code <published>} element.
     */
    public String published;

    /**
     * The value of the Atom entry's {@code <content>} element.
     */
    public Record content;

    /**
     * Creates a new {@code AtomEntry} instance.
     *
     * @return A new {@code AtomEntry} instance.
     */
    static AtomEntry create() {
        return new AtomEntry();
    }
    /**
     * Creates a new {@code AtomEntry} instance based on a given stream.
     * A few endpoints, such as {@code search/jobs/{sid}}, the format of the stream must be xml.
     * return an Atom {@code <entry>} element as the root of the response.
     *
     * @param input The input stream. 
     * @return An {@code AtomEntry} instance representing the parsed stream.
     */
    
    protected static AtomEntry parseStreamXML(InputStream input) {
		
		XMLStreamReader reader = createReader(input);

		AtomEntry result = AtomEntry.parse(reader);

		try {
			reader.close();
		} catch (XMLStreamException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		return result;
	}
    
    /**
     * Creates a new {@code AtomEntry} instance based on a given stream.
     * A few endpoints, such as {@code search/jobs/{sid}}, the format of the stream must be json.
     * return an Atom {@code <entry>} element as the root of the response.
     *
     * @param input The input stream. 
     * @return An {@code AtomEntry} instance representing the parsed stream.
     */
    
    protected static AtomEntry parseStreamJSON(InputStream input) {
    	try {
			Gson gson = new GsonBuilder().create();
			Map<String, Object> expectedData = gson.fromJson(
					StringUtil.streamToString(input), Map.class);
			List<Map<String, Object>> entryList = (List<Map<String, Object>>) expectedData
					.get("entry");
			
			//AtomEntry can not hold a list of entry
			
			for (Map<String, Object> map : entryList) {
				AtomEntry entry = AtomEntry.parse(map);
				return entry;
			}
			return null;
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

    /**
     * Creates a new {@code AtomEntry} instance based on a given stream.
     * A few endpoints, such as {@code search/jobs/{sid}}, the format of the stream can be xml or json.
     * return an Atom {@code <entry>} element as the root of the response.
     *
     * @param input The input stream.
     * @param outputMode {@code OutputMode} type, xml or json.
     * @return An {@code AtomEntry} instance representing the parsed stream.
     */

	public static AtomEntry parseStream(InputStream input, OutputMode outputMode) {
		
		if (outputMode == OutputMode.JSON) {
			return parseStreamJSON(input);
		}else {
			return parseStreamXML(input);
		}
	}

    /**
     * Creates a new {@code AtomEntry} instance based on a given XML reader.
     *
     * @param reader The XML reader.
     * @return An {@code AtomEntry} instance representing the parsed XML.
     */
    static AtomEntry parse(XMLStreamReader reader) {
        AtomEntry entry = AtomEntry.create();
        entry.load(reader, "entry");
        return entry;
    }
    
    static AtomEntry parse(Map<String, Object> input) {
    	AtomEntry entry = AtomEntry.create();
    	entry.load(input, "entry");
        return entry;
    }

    /**
     * Initializes the current instance using the given XML reader.
     *
     * @param reader The XML reader.
     */
    @Override
    void init(XMLStreamReader reader) {
        assert reader.isStartElement();

        String name = reader.getLocalName();

        if (name.equals("published")) {
            this.published = parseText(reader);
        } else if (name.equals("content")) {
            this.content = parseContent(reader);
        } else {
            super.init(reader);
        }
    }
    
    @Override
    void init(Map<String, Object> reader,String name) {


        if (name.equals("published")) {
            this.published = parseText(reader,name);
        } else if (name.equals("content")) {
            this.content = parseContent(reader,name);
        } else if(name.equals("id")){
        	this.id = parseText(reader,name);
        }else if(name.equals("name")){
        	this.title = parseText(reader,name);
        }else {
            super.init(reader,name);
        }
    }

    /**
     * Parses the {@code <content>} element of an Atom entry.
     *
     * @param reader The XML reader.
     * @return A {@code Record} object containing the parsed values.
     * @throws XMLStreamException
     */
  
    private Record parseContent(XMLStreamReader reader) {
        assert isStartElement(reader, "content");

        scan(reader);

        // The content element should contain a single <dict> element

        if (!isStartElement(reader, "dict")) {
            if (reader.isCharacters()) {
                String contentString = reader.getText();
                content = new Record();
                // just call to raw_data now..
                content.put("raw_data", contentString);
                //set empty meta data to avoid null pointer exception
                HashMap<String, String> entityMetadata = new HashMap<String, String>();
                content.put("eai:acl", entityMetadata);
//TODO: who changed 

                scan(reader);
            } else if (!isEndElement(reader, "content")) {
                syntaxError(reader);
            }
        } else {

            content = parseDict(reader);
        }
        if (!isEndElement(reader, "content"))
            syntaxError(reader);

        scan(reader); // Consume </content>

        return content;
    }
    
	private Record parseContent(Map<String, Object> reader, String name) {

		// The content element should contain a single <dict> element
		Object contentObject = reader.get(name);
		if (!(contentObject instanceof Map)) {
			String contentString = (String) reader.get(name);
			content = new Record();
			// just call to raw_data now..
			content.put("raw_data", contentString);
			// set empty meta data to avoid null pointer exception
			Record entityMetadata = new Record();
			content.put("eai:acl", entityMetadata);
		} else {
			content = parseDict((Map<String, Object>) contentObject, name);
			if (content.get("eai:acl") == null) {
				if (reader.containsKey("acl")) {
					Record entityMetadata = new Record();
					entityMetadata
							.putAll((Map<? extends String, ? extends Object>) reader
									.get("acl"));
					content.put("eai:acl", entityMetadata);
				} else {
					Record entityMetadata = new Record();
					content.put("eai:acl", entityMetadata);
				}
			}
		}

		return content;
	}

    /**
     * Parses a {@code <dict>} content element and returns a {@code Record}
     * object containing the parsed values.
     *
     * @param reader The {@code <dict>} element to parse.
     * @return A {@code Record} object containing the parsed values.
     */
    private Record parseDict(XMLStreamReader reader) {
        assert isStartElement(reader, "dict");

        Record result = new Record();

        scan(reader);
        while (isStartElement(reader, "key")) {
            String key = reader.getAttributeValue(null, "name");
            Object value = parseValue(reader);
            // Null values, the result of empty elements, are parsed as though
            // they don't exist, making it easier for the client framework to
            // supply more meaningful default values.
            if (value != null) result.put(key, value);
        }

        if (!isEndElement(reader, "dict"))
            syntaxError(reader);


        scan(reader); // Consume </dict>

        return result;
    }
    
    private Record parseDict(Map<String, Object> reader,String name) {
        Record result = new Record();
        removeNUll(reader);
        result.putAll(reader);
        return result;
    }

    /**
     * Parses a {@code <list>} element and returns a {@code List} object
     * containing the parsed values.
     *
     * @param reader The XML reader.
     * @return A {@code List} object containing the parsed values.
     */
    private List<Object> parseList(XMLStreamReader reader) {
        assert isStartElement(reader, "list");

        List<Object> result = new ArrayList<Object>();

        scan(reader);
        while (isStartElement(reader, "item")) {
            Object value = parseValue(reader);
            result.add(value);
        }

        if (!isEndElement(reader, "list"))
            syntaxError(reader);

        scan(reader); // Consume </list>

        return result;
    }

    // Parses either a dict or list structure.
    private Object parseStructure(XMLStreamReader reader) {
        String name = reader.getLocalName();

        if (name.equals("dict"))
            return parseDict(reader);

        if (name.equals("list"))
            return parseList(reader);

        syntaxError(reader);

        return null; // Unreached
    }

    /**
     * Parses the value contained by the element at the current cursor position
     * of the given reader.
     * <p/>
     * <b>Note:</b> This function takes the parent element as its starting point
     * so that it can correctly match the end element. The function takes the
     * start element and its corresponding end element, then returns the
     * contained value. The cursor is then located at the next element to be
     * parsed.
     *
     * @param reader The XML reader to parse.
     * @return An object containing the parsed values. If the source was a text
     * value, the object is a {@code String}. If the source was a {@code <dict>}
     * element, the object is a {@code Record}. If the source was a
     * {@code <list>} element, the object is a {@code List} object.
     */
    Object parseValue(XMLStreamReader reader) {
        assert reader.isStartElement();

        String name = reader.getLocalName();

        scan(reader);

        Object value;
        switch (reader.getEventType()) {
            case XMLStreamConstants.CHARACTERS:
                value = reader.getText();
                scan(reader); // Advance cursor
                break;

            case XMLStreamConstants.START_ELEMENT:
                value = parseStructure(reader);
                break;

            case XMLStreamConstants.END_ELEMENT:
                value = null; // Empty element
                break;

            default:
                value = null;
                syntaxError(reader);
        }

        if (!isEndElement(reader, name))
            syntaxError(reader);

        scan(reader); // Consume end element

        return value;
    }
}
