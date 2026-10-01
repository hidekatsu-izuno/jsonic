package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class Base64ConversionTest {
	@Test
	public void encodesAndDecodesKnownVectors() {
		String[] plain = {"", "f", "fo", "foo", "foob", "fooba", "foobar"};
		String[] encoded = {"", "Zg==", "Zm8=", "Zm9v", "Zm9vYg==", "Zm9vYmE=", "Zm9vYmFy"};
		for (int i = 0; i < plain.length; i++) {
			byte[] bytes = plain[i].getBytes(StandardCharsets.US_ASCII);
			assertEquals("\"" + encoded[i] + "\"", JSON.encode(bytes));
			assertArrayEquals(bytes, JSON.decode("\"" + encoded[i] + "\"", byte[].class));
		}
		assertEquals("\"+/8=\"", JSON.encode(new byte[] {(byte) 0xfb, (byte) 0xff}));
		assertArrayEquals(new byte[] {(byte) 0xfb, (byte) 0xff}, JSON.decode("\"+/8=\"", byte[].class));
		assertNull(JSON.decode("null", byte[].class));
	}

	@Test
	public void acceptsMimeWhitespaceAndUnpaddedInput() {
		JSON json = new JSON();
		assertArrayEquals("foobar".getBytes(StandardCharsets.US_ASCII),
				(byte[]) json.convert(" Zm9v\r\nYmFy\t", byte[].class));
		assertArrayEquals(new byte[] {'f'}, (byte[]) json.convert("Zg", byte[].class));
		assertArrayEquals(new byte[] {'f', 'o'}, (byte[]) json.convert("Zm8", byte[].class));
	}

	@Test
	public void rejectsMalformedInput() {
		for (String value : new String[] {"Z", "Zg=", "=Zm9v", "Zg==Zm9v"}) {
			JSONException exception = assertThrows(JSONException.class,
					() -> new JSON().convert(value, byte[].class));
			assertTrue(exception.getCause() instanceof IllegalArgumentException);
		}
	}

}
