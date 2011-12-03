package net.arnx.jsonic;

import static org.junit.Assert.*;

import org.junit.Test;

public class CaseStyleTest {

	@Test
	public void testLowerCase() {
		assertEquals("aaaaaa", CaseStyle.LOWER_CASE.to("aaaaaa"));
		assertEquals("aaaaaa", CaseStyle.LOWER_CASE.to("AAAAAA"));
		assertEquals("aaaaaa", CaseStyle.LOWER_CASE.to("Aaaaaa"));
		assertEquals("aaaaaa", CaseStyle.LOWER_CASE.to("AaaAaa"));
		assertEquals("aaaaaa", CaseStyle.LOWER_CASE.to("aaaAaa"));
		assertEquals("aaa aaa", CaseStyle.LOWER_CASE.to("AAA AAA"));
		assertEquals("aaa_aaa", CaseStyle.LOWER_CASE.to("AAA_AAA"));
		assertEquals("aaa-aaa", CaseStyle.LOWER_CASE.to("AAA-AAA"));
		assertEquals("aaa aaa", CaseStyle.LOWER_CASE.to("aaa aaa"));
		assertEquals("aaa_aaa", CaseStyle.LOWER_CASE.to("aaa_aaa"));
		assertEquals("aaa-aaa", CaseStyle.LOWER_CASE.to("aaa-aaa"));
		assertEquals("あああaaa", CaseStyle.LOWER_CASE.to("あああAaa"));
		assertEquals("あああaaa", CaseStyle.LOWER_CASE.to("あああaaa"));
		assertEquals("aあああああ", CaseStyle.LOWER_CASE.to("Aあああああ"));
		assertEquals("aああaaa", CaseStyle.LOWER_CASE.to("AああAaa"));
		assertEquals("aあああああ", CaseStyle.LOWER_CASE.to("aあああああ"));
		assertEquals("aああaaa", CaseStyle.LOWER_CASE.to("aああAaa"));
		
		assertEquals("_aaaaaa", CaseStyle.LOWER_CASE.to("_aaaaaa"));
		assertEquals("_aaaaaa", CaseStyle.LOWER_CASE.to("_AAAAAA"));
		assertEquals("_aaaaaa", CaseStyle.LOWER_CASE.to("_Aaaaaa"));
		assertEquals("_aaaaaa", CaseStyle.LOWER_CASE.to("_AaaAaa"));
		assertEquals("_aaaaaa", CaseStyle.LOWER_CASE.to("_aaaAaa"));
		assertEquals("_aaa aaa", CaseStyle.LOWER_CASE.to("_AAA AAA"));
		assertEquals("_aaa_aaa", CaseStyle.LOWER_CASE.to("_AAA_AAA"));
		assertEquals("_aaa-aaa", CaseStyle.LOWER_CASE.to("_AAA-AAA"));
		assertEquals("_aaa aaa", CaseStyle.LOWER_CASE.to("_aaa aaa"));
		assertEquals("_aaa_aaa", CaseStyle.LOWER_CASE.to("_aaa_aaa"));
		assertEquals("_aaa-aaa", CaseStyle.LOWER_CASE.to("_aaa-aaa"));
		assertEquals("_あああaaa", CaseStyle.LOWER_CASE.to("_あああAaa"));
		assertEquals("_あああaaa", CaseStyle.LOWER_CASE.to("_あああaaa"));
		assertEquals("_aあああああ", CaseStyle.LOWER_CASE.to("_Aあああああ"));
		assertEquals("_aああaaa", CaseStyle.LOWER_CASE.to("_AああAaa"));
		assertEquals("_aあああああ", CaseStyle.LOWER_CASE.to("_aあああああ"));
		assertEquals("_aああaaa", CaseStyle.LOWER_CASE.to("_aああAaa"));
		
		assertEquals("aaaaaa_", CaseStyle.LOWER_CASE.to("aaaaaa_"));
		assertEquals("aaaaaa_", CaseStyle.LOWER_CASE.to("AAAAAA_"));
		assertEquals("aaaaaa_", CaseStyle.LOWER_CASE.to("Aaaaaa_"));
		assertEquals("aaaaaa_", CaseStyle.LOWER_CASE.to("AaaAaa_"));
		assertEquals("aaaaaa_", CaseStyle.LOWER_CASE.to("aaaAaa_"));
		assertEquals("aaa aaa_", CaseStyle.LOWER_CASE.to("AAA AAA_"));
		assertEquals("aaa_aaa_", CaseStyle.LOWER_CASE.to("AAA_AAA_"));
		assertEquals("aaa-aaa_", CaseStyle.LOWER_CASE.to("AAA-AAA_"));
		assertEquals("aaa aaa_", CaseStyle.LOWER_CASE.to("aaa aaa_"));
		assertEquals("aaa_aaa_", CaseStyle.LOWER_CASE.to("aaa_aaa_"));
		assertEquals("aaa-aaa_", CaseStyle.LOWER_CASE.to("aaa-aaa_"));
		assertEquals("あああaaa_", CaseStyle.LOWER_CASE.to("あああAaa_"));
		assertEquals("あああaaa_", CaseStyle.LOWER_CASE.to("あああaaa_"));
		assertEquals("aあああああ_", CaseStyle.LOWER_CASE.to("Aあああああ_"));
		assertEquals("aああaaa_", CaseStyle.LOWER_CASE.to("AああAaa_"));
		assertEquals("aあああああ_", CaseStyle.LOWER_CASE.to("aあああああ_"));
		assertEquals("aああaaa_", CaseStyle.LOWER_CASE.to("aああAaa_"));
	}

	@Test
	public void testLowerCamel() {
		assertEquals("aaaaaa", CaseStyle.LOWER_CAMEL.to("aaaaaa"));
		assertEquals("aaaaaa", CaseStyle.LOWER_CAMEL.to("AAAAAA"));
		assertEquals("aaaaaa", CaseStyle.LOWER_CAMEL.to("Aaaaaa"));
		assertEquals("aaaAaa", CaseStyle.LOWER_CAMEL.to("AaaAaa"));
		assertEquals("aaaAaa", CaseStyle.LOWER_CAMEL.to("aaaAaa"));
		assertEquals("aaaAaa", CaseStyle.LOWER_CAMEL.to("AAA AAA"));
		assertEquals("aaaAaa", CaseStyle.LOWER_CAMEL.to("AAA_AAA"));
		assertEquals("aaaAaa", CaseStyle.LOWER_CAMEL.to("AAA-AAA"));
		assertEquals("aaaAaa", CaseStyle.LOWER_CAMEL.to("aaa aaa"));
		assertEquals("aaaAaa", CaseStyle.LOWER_CAMEL.to("aaa_aaa"));
		assertEquals("aaaAaa", CaseStyle.LOWER_CAMEL.to("aaa-aaa"));
		assertEquals("aaaAaa", CaseStyle.LOWER_CAMEL.to("AAA  AAA"));
		assertEquals("あああAaa", CaseStyle.LOWER_CAMEL.to("あああAaa"));
		assertEquals("あああaaa", CaseStyle.LOWER_CAMEL.to("あああaaa"));
		assertEquals("aあああああ", CaseStyle.LOWER_CAMEL.to("Aあああああ"));
		assertEquals("aああAaa", CaseStyle.LOWER_CAMEL.to("AああAaa"));
		assertEquals("aあああああ", CaseStyle.LOWER_CAMEL.to("aあああああ"));
		assertEquals("aああAaa", CaseStyle.LOWER_CAMEL.to("aああAaa"));
		
		assertEquals("_aaaaaa", CaseStyle.LOWER_CAMEL.to("_aaaaaa"));
		assertEquals("_aaaaaa", CaseStyle.LOWER_CAMEL.to("_AAAAAA"));
		assertEquals("_aaaaaa", CaseStyle.LOWER_CAMEL.to("_Aaaaaa"));
		assertEquals("_aaaAaa", CaseStyle.LOWER_CAMEL.to("_AaaAaa"));
		assertEquals("_aaaAaa", CaseStyle.LOWER_CAMEL.to("_aaaAaa"));
		assertEquals("_aaaAaa", CaseStyle.LOWER_CAMEL.to("_AAA AAA"));
		assertEquals("_aaaAaa", CaseStyle.LOWER_CAMEL.to("_AAA_AAA"));
		assertEquals("_aaaAaa", CaseStyle.LOWER_CAMEL.to("_AAA-AAA"));
		assertEquals("_aaaAaa", CaseStyle.LOWER_CAMEL.to("_aaa aaa"));
		assertEquals("_aaaAaa", CaseStyle.LOWER_CAMEL.to("_aaa_aaa"));
		assertEquals("_aaaAaa", CaseStyle.LOWER_CAMEL.to("_aaa-aaa"));
		assertEquals("_aaaAaa", CaseStyle.LOWER_CAMEL.to("_AAA  AAA"));
		assertEquals("_あああAaa", CaseStyle.LOWER_CAMEL.to("_あああAaa"));
		assertEquals("_あああaaa", CaseStyle.LOWER_CAMEL.to("_あああaaa"));
		assertEquals("_aあああああ", CaseStyle.LOWER_CAMEL.to("_Aあああああ"));
		assertEquals("_aああAaa", CaseStyle.LOWER_CAMEL.to("_AああAaa"));
		assertEquals("_aあああああ", CaseStyle.LOWER_CAMEL.to("_aあああああ"));
		assertEquals("_aああAaa", CaseStyle.LOWER_CAMEL.to("_aああAaa"));
		
		assertEquals("aaaaaa_", CaseStyle.LOWER_CAMEL.to("aaaaaa_"));
		assertEquals("aaaaaa_", CaseStyle.LOWER_CAMEL.to("AAAAAA_"));
		assertEquals("aaaaaa_", CaseStyle.LOWER_CAMEL.to("Aaaaaa_"));
		assertEquals("aaaAaa_", CaseStyle.LOWER_CAMEL.to("AaaAaa_"));
		assertEquals("aaaAaa_", CaseStyle.LOWER_CAMEL.to("aaaAaa_"));
		assertEquals("aaaAaa_", CaseStyle.LOWER_CAMEL.to("AAA AAA_"));
		assertEquals("aaaAaa_", CaseStyle.LOWER_CAMEL.to("AAA_AAA_"));
		assertEquals("aaaAaa_", CaseStyle.LOWER_CAMEL.to("AAA-AAA_"));
		assertEquals("aaaAaa_", CaseStyle.LOWER_CAMEL.to("aaa aaa_"));
		assertEquals("aaaAaa_", CaseStyle.LOWER_CAMEL.to("aaa_aaa_"));
		assertEquals("aaaAaa_", CaseStyle.LOWER_CAMEL.to("aaa-aaa_"));
		assertEquals("aaaAaa_", CaseStyle.LOWER_CAMEL.to("AAA  AAA_"));
		assertEquals("あああAaa_", CaseStyle.LOWER_CAMEL.to("あああAaa_"));
		assertEquals("あああaaa_", CaseStyle.LOWER_CAMEL.to("あああaaa_"));
		assertEquals("aあああああ_", CaseStyle.LOWER_CAMEL.to("Aあああああ_"));
		assertEquals("aああAaa_", CaseStyle.LOWER_CAMEL.to("AああAaa_"));
		assertEquals("aあああああ_", CaseStyle.LOWER_CAMEL.to("aあああああ_"));
		assertEquals("aああAaa_", CaseStyle.LOWER_CAMEL.to("aああAaa_"));
	}
	
	@Test
	public void testUpperCase() {
		assertEquals("AAAAAA", CaseStyle.UPPER_CASE.to("aaaaaa"));
		assertEquals("AAAAAA", CaseStyle.UPPER_CASE.to("AAAAAA"));
		assertEquals("AAAAAA", CaseStyle.UPPER_CASE.to("Aaaaaa"));
		assertEquals("AAAAAA", CaseStyle.UPPER_CASE.to("AaaAaa"));
		assertEquals("AAAAAA", CaseStyle.UPPER_CASE.to("aaaAaa"));
		assertEquals("AAA AAA", CaseStyle.UPPER_CASE.to("AAA AAA"));
		assertEquals("AAA_AAA", CaseStyle.UPPER_CASE.to("AAA_AAA"));
		assertEquals("AAA-AAA", CaseStyle.UPPER_CASE.to("AAA-AAA"));
		assertEquals("AAA AAA", CaseStyle.UPPER_CASE.to("aaa aaa"));
		assertEquals("AAA_AAA", CaseStyle.UPPER_CASE.to("aaa_aaa"));
		assertEquals("AAA-AAA", CaseStyle.UPPER_CASE.to("aaa-aaa"));
		assertEquals("あああAAA", CaseStyle.UPPER_CASE.to("あああAaa"));
		assertEquals("あああAAA", CaseStyle.UPPER_CASE.to("あああaaa"));
		assertEquals("Aあああああ", CaseStyle.UPPER_CASE.to("Aあああああ"));
		assertEquals("AああAAA", CaseStyle.UPPER_CASE.to("AああAaa"));
		assertEquals("Aあああああ", CaseStyle.UPPER_CASE.to("aあああああ"));
		assertEquals("AああAAA", CaseStyle.UPPER_CASE.to("aああAaa"));
		
		assertEquals("_AAAAAA", CaseStyle.UPPER_CASE.to("_aaaaaa"));
		assertEquals("_AAAAAA", CaseStyle.UPPER_CASE.to("_AAAAAA"));
		assertEquals("_AAAAAA", CaseStyle.UPPER_CASE.to("_Aaaaaa"));
		assertEquals("_AAAAAA", CaseStyle.UPPER_CASE.to("_AaaAaa"));
		assertEquals("_AAAAAA", CaseStyle.UPPER_CASE.to("_aaaAaa"));
		assertEquals("_AAA AAA", CaseStyle.UPPER_CASE.to("_AAA AAA"));
		assertEquals("_AAA_AAA", CaseStyle.UPPER_CASE.to("_AAA_AAA"));
		assertEquals("_AAA-AAA", CaseStyle.UPPER_CASE.to("_AAA-AAA"));
		assertEquals("_AAA AAA", CaseStyle.UPPER_CASE.to("_aaa aaa"));
		assertEquals("_AAA_AAA", CaseStyle.UPPER_CASE.to("_aaa_aaa"));
		assertEquals("_AAA-AAA", CaseStyle.UPPER_CASE.to("_aaa-aaa"));
		assertEquals("_あああAAA", CaseStyle.UPPER_CASE.to("_あああAaa"));
		assertEquals("_あああAAA", CaseStyle.UPPER_CASE.to("_あああaaa"));
		assertEquals("_Aあああああ", CaseStyle.UPPER_CASE.to("_Aあああああ"));
		assertEquals("_AああAAA", CaseStyle.UPPER_CASE.to("_AああAaa"));
		assertEquals("_Aあああああ", CaseStyle.UPPER_CASE.to("_aあああああ"));
		assertEquals("_AああAAA", CaseStyle.UPPER_CASE.to("_aああAaa"));
		
		assertEquals("AAAAAA_", CaseStyle.UPPER_CASE.to("aaaaaa_"));
		assertEquals("AAAAAA_", CaseStyle.UPPER_CASE.to("AAAAAA_"));
		assertEquals("AAAAAA_", CaseStyle.UPPER_CASE.to("Aaaaaa_"));
		assertEquals("AAAAAA_", CaseStyle.UPPER_CASE.to("AaaAaa_"));
		assertEquals("AAAAAA_", CaseStyle.UPPER_CASE.to("aaaAaa_"));
		assertEquals("AAA AAA_", CaseStyle.UPPER_CASE.to("AAA AAA_"));
		assertEquals("AAA_AAA_", CaseStyle.UPPER_CASE.to("AAA_AAA_"));
		assertEquals("AAA-AAA_", CaseStyle.UPPER_CASE.to("AAA-AAA_"));
		assertEquals("AAA AAA_", CaseStyle.UPPER_CASE.to("aaa aaa_"));
		assertEquals("AAA_AAA_", CaseStyle.UPPER_CASE.to("aaa_aaa_"));
		assertEquals("AAA-AAA_", CaseStyle.UPPER_CASE.to("aaa-aaa_"));
		assertEquals("あああAAA_", CaseStyle.UPPER_CASE.to("あああAaa_"));
		assertEquals("あああAAA_", CaseStyle.UPPER_CASE.to("あああaaa_"));
		assertEquals("Aあああああ_", CaseStyle.UPPER_CASE.to("Aあああああ_"));
		assertEquals("AああAAA_", CaseStyle.UPPER_CASE.to("AああAaa_"));
		assertEquals("Aあああああ_", CaseStyle.UPPER_CASE.to("aあああああ_"));
		assertEquals("AああAAA_", CaseStyle.UPPER_CASE.to("aああAaa_"));
	}

}
