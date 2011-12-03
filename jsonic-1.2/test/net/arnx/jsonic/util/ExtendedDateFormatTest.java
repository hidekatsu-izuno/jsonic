package net.arnx.jsonic.util;

import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.Date;

import org.junit.Test;

public class ExtendedDateFormatTest {
	@Test
	public void testComplexDateFormat() throws Exception {
		assertEquals("Z\0", ExtendedDateFormat.escape("ZZ"));
		assertEquals("'ZZ'", ExtendedDateFormat.escape("'ZZ'"));
		assertEquals("Z\0'ZZ'", ExtendedDateFormat.escape("ZZ'ZZ'"));
		assertEquals("'ZZ'Z\0", ExtendedDateFormat.escape("'ZZ'ZZ"));
		
		Date date = toDate(2000, 1, 1, 0, 0, 0, 0);
		
		assertEquals("2000/01/01 +0900", new ExtendedDateFormat("yyyy/MM/dd Z").format(date));
		assertEquals("2000/01/01 +09:00", new ExtendedDateFormat("yyyy/MM/dd ZZ").format(date));
		assertEquals(date, new ExtendedDateFormat("yyyy/MM/dd Z").parse("2000/01/01 +0900"));
		//assertEquals(date, new ExtendedDateFormat("yyyy/MM/dd ZZ").parse("2000/01/01 +09:00"));
	}
	
	private Date toDate(int year, int month, int date, int hour, int minute, int second, int msec) {
		Calendar c = Calendar.getInstance();
		c.clear();
		c.set(year, month-1, date, hour, minute, second);
		c.set(Calendar.MILLISECOND, msec);
		return c.getTime();
	}
}
