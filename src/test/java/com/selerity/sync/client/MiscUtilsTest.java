package com.selerity.sync.client;


import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;


public class MiscUtilsTest {

	
	@Test
	public void testParseNanoTime() throws ParseException{
		assertEquals(0, MiscUtils.parseNanoTime("1970-01-01T00:00:00.000000000"));
		assertEquals(1, MiscUtils.parseNanoTime("1970-01-01T00:00:00.000000001"));
		assertEquals(123456789, MiscUtils.parseNanoTime("1970-01-01T00:00:00.123456789"));
		assertEquals(1000000000, MiscUtils.parseNanoTime("1970-01-01T00:00:01.000000000"));
		assertEquals(86400000000000L, MiscUtils.parseNanoTime("1970-01-02T00:00:00.000000000"));
	}
	
	@Test
	public void testFormatNanoTime() throws ParseException{
		assertEquals("1970-01-01T00:00:00.000000000", MiscUtils.formatNanoTime(0));
		assertEquals("1970-01-01T00:00:00.000000001", MiscUtils.formatNanoTime(1));
		assertEquals("1970-01-01T00:00:01.000000000", MiscUtils.formatNanoTime(1000000000));
		assertEquals("1970-01-01T00:00:01.123456789", MiscUtils.formatNanoTime(1123456789));
		assertEquals("1970-01-02T00:00:00.000000000", MiscUtils.formatNanoTime(86400000000000L));
	}
	
	@Test
	public void testLeftPadNumber() throws ParseException{
		assertEquals("00000", MiscUtils.leftPadNumber(0, 5));
		assertEquals("00123", MiscUtils.leftPadNumber(123, 5));
		assertEquals("12345", MiscUtils.leftPadNumber(12345, 5));
		assertEquals("1234567", MiscUtils.leftPadNumber(1234567, 5));
	}
	

	@Test
	public void testMakeStringStringMap(){
		Map<String,String> map = new HashMap<String,String>();
		map.put("foo", "blah");
		assertEquals("{\"foo\":\"blah\"}", MiscUtils.makeStringStringMap(map).toString());
	}
	
	
}
