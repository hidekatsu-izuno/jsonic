package net.arnx.jsonic;

import static org.junit.Assert.*;

import org.junit.Test;

public class NamingStyleTest {

	@Test
	public void testLowerCase() {
		assertEquals("aaaaaa", NamingStyle.LOWER_CASE.to("aaaaaa"));
		assertEquals("aaaaaa", NamingStyle.LOWER_CASE.to("AAAAAA"));
		assertEquals("aaaaaa", NamingStyle.LOWER_CASE.to("Aaaaaa"));
		assertEquals("aaaaaa", NamingStyle.LOWER_CASE.to("AaaAaa"));
		assertEquals("aaaaaa", NamingStyle.LOWER_CASE.to("aaaAaa"));
		assertEquals("aaa aaa", NamingStyle.LOWER_CASE.to("AAA AAA"));
		assertEquals("aaa_aaa", NamingStyle.LOWER_CASE.to("AAA_AAA"));
		assertEquals("aaa-aaa", NamingStyle.LOWER_CASE.to("AAA-AAA"));
		assertEquals("aaa aaa", NamingStyle.LOWER_CASE.to("aaa aaa"));
		assertEquals("aaa_aaa", NamingStyle.LOWER_CASE.to("aaa_aaa"));
		assertEquals("aaa-aaa", NamingStyle.LOWER_CASE.to("aaa-aaa"));
		assertEquals("あああaaa", NamingStyle.LOWER_CASE.to("あああAaa"));
		assertEquals("あああaaa", NamingStyle.LOWER_CASE.to("あああaaa"));
		assertEquals("aあああああ", NamingStyle.LOWER_CASE.to("Aあああああ"));
		assertEquals("aああaaa", NamingStyle.LOWER_CASE.to("AああAaa"));
		assertEquals("aあああああ", NamingStyle.LOWER_CASE.to("aあああああ"));
		assertEquals("aああaaa", NamingStyle.LOWER_CASE.to("aああAaa"));
		
		assertEquals("_aaaaaa", NamingStyle.LOWER_CASE.to("_aaaaaa"));
		assertEquals("_aaaaaa", NamingStyle.LOWER_CASE.to("_AAAAAA"));
		assertEquals("_aaaaaa", NamingStyle.LOWER_CASE.to("_Aaaaaa"));
		assertEquals("_aaaaaa", NamingStyle.LOWER_CASE.to("_AaaAaa"));
		assertEquals("_aaaaaa", NamingStyle.LOWER_CASE.to("_aaaAaa"));
		assertEquals("_aaa aaa", NamingStyle.LOWER_CASE.to("_AAA AAA"));
		assertEquals("_aaa_aaa", NamingStyle.LOWER_CASE.to("_AAA_AAA"));
		assertEquals("_aaa-aaa", NamingStyle.LOWER_CASE.to("_AAA-AAA"));
		assertEquals("_aaa aaa", NamingStyle.LOWER_CASE.to("_aaa aaa"));
		assertEquals("_aaa_aaa", NamingStyle.LOWER_CASE.to("_aaa_aaa"));
		assertEquals("_aaa-aaa", NamingStyle.LOWER_CASE.to("_aaa-aaa"));
		assertEquals("_あああaaa", NamingStyle.LOWER_CASE.to("_あああAaa"));
		assertEquals("_あああaaa", NamingStyle.LOWER_CASE.to("_あああaaa"));
		assertEquals("_aあああああ", NamingStyle.LOWER_CASE.to("_Aあああああ"));
		assertEquals("_aああaaa", NamingStyle.LOWER_CASE.to("_AああAaa"));
		assertEquals("_aあああああ", NamingStyle.LOWER_CASE.to("_aあああああ"));
		assertEquals("_aああaaa", NamingStyle.LOWER_CASE.to("_aああAaa"));
		
		assertEquals("aaaaaa_", NamingStyle.LOWER_CASE.to("aaaaaa_"));
		assertEquals("aaaaaa_", NamingStyle.LOWER_CASE.to("AAAAAA_"));
		assertEquals("aaaaaa_", NamingStyle.LOWER_CASE.to("Aaaaaa_"));
		assertEquals("aaaaaa_", NamingStyle.LOWER_CASE.to("AaaAaa_"));
		assertEquals("aaaaaa_", NamingStyle.LOWER_CASE.to("aaaAaa_"));
		assertEquals("aaa aaa_", NamingStyle.LOWER_CASE.to("AAA AAA_"));
		assertEquals("aaa_aaa_", NamingStyle.LOWER_CASE.to("AAA_AAA_"));
		assertEquals("aaa-aaa_", NamingStyle.LOWER_CASE.to("AAA-AAA_"));
		assertEquals("aaa aaa_", NamingStyle.LOWER_CASE.to("aaa aaa_"));
		assertEquals("aaa_aaa_", NamingStyle.LOWER_CASE.to("aaa_aaa_"));
		assertEquals("aaa-aaa_", NamingStyle.LOWER_CASE.to("aaa-aaa_"));
		assertEquals("あああaaa_", NamingStyle.LOWER_CASE.to("あああAaa_"));
		assertEquals("あああaaa_", NamingStyle.LOWER_CASE.to("あああaaa_"));
		assertEquals("aあああああ_", NamingStyle.LOWER_CASE.to("Aあああああ_"));
		assertEquals("aああaaa_", NamingStyle.LOWER_CASE.to("AああAaa_"));
		assertEquals("aあああああ_", NamingStyle.LOWER_CASE.to("aあああああ_"));
		assertEquals("aああaaa_", NamingStyle.LOWER_CASE.to("aああAaa_"));
	}

	@Test
	public void testLowerCamel() {
		assertEquals("aaaaaa", NamingStyle.LOWER_CAMEL.to("aaaaaa"));
		assertEquals("aaaaaa", NamingStyle.LOWER_CAMEL.to("AAAAAA"));
		assertEquals("aaaaaa", NamingStyle.LOWER_CAMEL.to("Aaaaaa"));
		assertEquals("aaaAaa", NamingStyle.LOWER_CAMEL.to("AaaAaa"));
		assertEquals("aaaAaa", NamingStyle.LOWER_CAMEL.to("aaaAaa"));
		assertEquals("aaaAaa", NamingStyle.LOWER_CAMEL.to("AAA AAA"));
		assertEquals("aaaAaa", NamingStyle.LOWER_CAMEL.to("AAA_AAA"));
		assertEquals("aaaAaa", NamingStyle.LOWER_CAMEL.to("AAA-AAA"));
		assertEquals("aaaAaa", NamingStyle.LOWER_CAMEL.to("aaa aaa"));
		assertEquals("aaaAaa", NamingStyle.LOWER_CAMEL.to("aaa_aaa"));
		assertEquals("aaaAaa", NamingStyle.LOWER_CAMEL.to("aaa-aaa"));
		assertEquals("aaaAaa", NamingStyle.LOWER_CAMEL.to("AAA  AAA"));
		assertEquals("あああAaa", NamingStyle.LOWER_CAMEL.to("あああAaa"));
		assertEquals("あああaaa", NamingStyle.LOWER_CAMEL.to("あああaaa"));
		assertEquals("aあああああ", NamingStyle.LOWER_CAMEL.to("Aあああああ"));
		assertEquals("aああAaa", NamingStyle.LOWER_CAMEL.to("AああAaa"));
		assertEquals("aあああああ", NamingStyle.LOWER_CAMEL.to("aあああああ"));
		assertEquals("aああAaa", NamingStyle.LOWER_CAMEL.to("aああAaa"));
		
		assertEquals("_aaaaaa", NamingStyle.LOWER_CAMEL.to("_aaaaaa"));
		assertEquals("_aaaaaa", NamingStyle.LOWER_CAMEL.to("_AAAAAA"));
		assertEquals("_aaaaaa", NamingStyle.LOWER_CAMEL.to("_Aaaaaa"));
		assertEquals("_aaaAaa", NamingStyle.LOWER_CAMEL.to("_AaaAaa"));
		assertEquals("_aaaAaa", NamingStyle.LOWER_CAMEL.to("_aaaAaa"));
		assertEquals("_aaaAaa", NamingStyle.LOWER_CAMEL.to("_AAA AAA"));
		assertEquals("_aaaAaa", NamingStyle.LOWER_CAMEL.to("_AAA_AAA"));
		assertEquals("_aaaAaa", NamingStyle.LOWER_CAMEL.to("_AAA-AAA"));
		assertEquals("_aaaAaa", NamingStyle.LOWER_CAMEL.to("_aaa aaa"));
		assertEquals("_aaaAaa", NamingStyle.LOWER_CAMEL.to("_aaa_aaa"));
		assertEquals("_aaaAaa", NamingStyle.LOWER_CAMEL.to("_aaa-aaa"));
		assertEquals("_aaaAaa", NamingStyle.LOWER_CAMEL.to("_AAA  AAA"));
		assertEquals("_あああAaa", NamingStyle.LOWER_CAMEL.to("_あああAaa"));
		assertEquals("_あああaaa", NamingStyle.LOWER_CAMEL.to("_あああaaa"));
		assertEquals("_aあああああ", NamingStyle.LOWER_CAMEL.to("_Aあああああ"));
		assertEquals("_aああAaa", NamingStyle.LOWER_CAMEL.to("_AああAaa"));
		assertEquals("_aあああああ", NamingStyle.LOWER_CAMEL.to("_aあああああ"));
		assertEquals("_aああAaa", NamingStyle.LOWER_CAMEL.to("_aああAaa"));
		
		assertEquals("aaaaaa_", NamingStyle.LOWER_CAMEL.to("aaaaaa_"));
		assertEquals("aaaaaa_", NamingStyle.LOWER_CAMEL.to("AAAAAA_"));
		assertEquals("aaaaaa_", NamingStyle.LOWER_CAMEL.to("Aaaaaa_"));
		assertEquals("aaaAaa_", NamingStyle.LOWER_CAMEL.to("AaaAaa_"));
		assertEquals("aaaAaa_", NamingStyle.LOWER_CAMEL.to("aaaAaa_"));
		assertEquals("aaaAaa_", NamingStyle.LOWER_CAMEL.to("AAA AAA_"));
		assertEquals("aaaAaa_", NamingStyle.LOWER_CAMEL.to("AAA_AAA_"));
		assertEquals("aaaAaa_", NamingStyle.LOWER_CAMEL.to("AAA-AAA_"));
		assertEquals("aaaAaa_", NamingStyle.LOWER_CAMEL.to("aaa aaa_"));
		assertEquals("aaaAaa_", NamingStyle.LOWER_CAMEL.to("aaa_aaa_"));
		assertEquals("aaaAaa_", NamingStyle.LOWER_CAMEL.to("aaa-aaa_"));
		assertEquals("aaaAaa_", NamingStyle.LOWER_CAMEL.to("AAA  AAA_"));
		assertEquals("あああAaa_", NamingStyle.LOWER_CAMEL.to("あああAaa_"));
		assertEquals("あああaaa_", NamingStyle.LOWER_CAMEL.to("あああaaa_"));
		assertEquals("aあああああ_", NamingStyle.LOWER_CAMEL.to("Aあああああ_"));
		assertEquals("aああAaa_", NamingStyle.LOWER_CAMEL.to("AああAaa_"));
		assertEquals("aあああああ_", NamingStyle.LOWER_CAMEL.to("aあああああ_"));
		assertEquals("aああAaa_", NamingStyle.LOWER_CAMEL.to("aああAaa_"));
	}

	@Test
	public void testLowerUnderscore() {
		assertEquals("aaaaaa", NamingStyle.LOWER_UNDERSCORE.to("aaaaaa"));
		assertEquals("aaaaaa", NamingStyle.LOWER_UNDERSCORE.to("AAAAAA"));
		assertEquals("aaaaaa", NamingStyle.LOWER_UNDERSCORE.to("Aaaaaa"));
		assertEquals("aaa_aaa", NamingStyle.LOWER_UNDERSCORE.to("AaaAaa"));
		assertEquals("aaa_aaa", NamingStyle.LOWER_UNDERSCORE.to("aaaAaa"));
		assertEquals("aaa_aaa", NamingStyle.LOWER_UNDERSCORE.to("AAA AAA"));
		assertEquals("aaa_aaa", NamingStyle.LOWER_UNDERSCORE.to("AAA_AAA"));
		assertEquals("aaa_aaa", NamingStyle.LOWER_UNDERSCORE.to("AAA-AAA"));
		assertEquals("aaa_aaa", NamingStyle.LOWER_UNDERSCORE.to("aaa aaa"));
		assertEquals("aaa_aaa", NamingStyle.LOWER_UNDERSCORE.to("aaa_aaa"));
		assertEquals("aaa_aaa", NamingStyle.LOWER_UNDERSCORE.to("aaa-aaa"));
		assertEquals("aaa_aaa", NamingStyle.LOWER_UNDERSCORE.to("AAA  AAA"));
		assertEquals("あああ_aaa", NamingStyle.LOWER_UNDERSCORE.to("あああAaa"));
		assertEquals("あああaaa", NamingStyle.LOWER_UNDERSCORE.to("あああaaa"));
		assertEquals("aあああああ", NamingStyle.LOWER_UNDERSCORE.to("Aあああああ"));
		assertEquals("aああ_aaa", NamingStyle.LOWER_UNDERSCORE.to("AああAaa"));
		assertEquals("aあああああ", NamingStyle.LOWER_UNDERSCORE.to("aあああああ"));
		assertEquals("aああ_aaa", NamingStyle.LOWER_UNDERSCORE.to("aああAaa"));
		
		assertEquals("_aaaaaa", NamingStyle.LOWER_UNDERSCORE.to("_aaaaaa"));
		assertEquals("_aaaaaa", NamingStyle.LOWER_UNDERSCORE.to("_AAAAAA"));
		assertEquals("_aaaaaa", NamingStyle.LOWER_UNDERSCORE.to("_Aaaaaa"));
		assertEquals("_aaa_aaa", NamingStyle.LOWER_UNDERSCORE.to("_AaaAaa"));
		assertEquals("_aaa_aaa", NamingStyle.LOWER_UNDERSCORE.to("_aaaAaa"));
		assertEquals("_aaa_aaa", NamingStyle.LOWER_UNDERSCORE.to("_AAA AAA"));
		assertEquals("_aaa_aaa", NamingStyle.LOWER_UNDERSCORE.to("_AAA_AAA"));
		assertEquals("_aaa_aaa", NamingStyle.LOWER_UNDERSCORE.to("_AAA-AAA"));
		assertEquals("_aaa_aaa", NamingStyle.LOWER_UNDERSCORE.to("_aaa aaa"));
		assertEquals("_aaa_aaa", NamingStyle.LOWER_UNDERSCORE.to("_aaa_aaa"));
		assertEquals("_aaa_aaa", NamingStyle.LOWER_UNDERSCORE.to("_aaa-aaa"));
		assertEquals("_aaa_aaa", NamingStyle.LOWER_UNDERSCORE.to("_AAA  AAA"));
		assertEquals("_あああ_aaa", NamingStyle.LOWER_UNDERSCORE.to("_あああAaa"));
		assertEquals("_あああaaa", NamingStyle.LOWER_UNDERSCORE.to("_あああaaa"));
		assertEquals("_aあああああ", NamingStyle.LOWER_UNDERSCORE.to("_Aあああああ"));
		assertEquals("_aああ_aaa", NamingStyle.LOWER_UNDERSCORE.to("_AああAaa"));
		assertEquals("_aあああああ", NamingStyle.LOWER_UNDERSCORE.to("_aあああああ"));
		assertEquals("_aああ_aaa", NamingStyle.LOWER_UNDERSCORE.to("_aああAaa"));
		
		assertEquals("aaaaaa_", NamingStyle.LOWER_UNDERSCORE.to("aaaaaa_"));
		assertEquals("aaaaaa_", NamingStyle.LOWER_UNDERSCORE.to("AAAAAA_"));
		assertEquals("aaaaaa_", NamingStyle.LOWER_UNDERSCORE.to("Aaaaaa_"));
		assertEquals("aaa_aaa_", NamingStyle.LOWER_UNDERSCORE.to("AaaAaa_"));
		assertEquals("aaa_aaa_", NamingStyle.LOWER_UNDERSCORE.to("aaaAaa_"));
		assertEquals("aaa_aaa_", NamingStyle.LOWER_UNDERSCORE.to("AAA AAA_"));
		assertEquals("aaa_aaa_", NamingStyle.LOWER_UNDERSCORE.to("AAA_AAA_"));
		assertEquals("aaa_aaa_", NamingStyle.LOWER_UNDERSCORE.to("AAA-AAA_"));
		assertEquals("aaa_aaa_", NamingStyle.LOWER_UNDERSCORE.to("aaa aaa_"));
		assertEquals("aaa_aaa_", NamingStyle.LOWER_UNDERSCORE.to("aaa_aaa_"));
		assertEquals("aaa_aaa_", NamingStyle.LOWER_UNDERSCORE.to("aaa-aaa_"));
		assertEquals("aaa_aaa_", NamingStyle.LOWER_UNDERSCORE.to("AAA  AAA_"));
		assertEquals("あああ_aaa_", NamingStyle.LOWER_UNDERSCORE.to("あああAaa_"));
		assertEquals("あああaaa_", NamingStyle.LOWER_UNDERSCORE.to("あああaaa_"));
		assertEquals("aあああああ_", NamingStyle.LOWER_UNDERSCORE.to("Aあああああ_"));
		assertEquals("aああ_aaa_", NamingStyle.LOWER_UNDERSCORE.to("AああAaa_"));
		assertEquals("aあああああ_", NamingStyle.LOWER_UNDERSCORE.to("aあああああ_"));
		assertEquals("aああ_aaa_", NamingStyle.LOWER_UNDERSCORE.to("aああAaa_"));
	}
	
	@Test
	public void testUpperCase() {
		assertEquals("AAAAAA", NamingStyle.UPPER_CASE.to("aaaaaa"));
		assertEquals("AAAAAA", NamingStyle.UPPER_CASE.to("AAAAAA"));
		assertEquals("AAAAAA", NamingStyle.UPPER_CASE.to("Aaaaaa"));
		assertEquals("AAAAAA", NamingStyle.UPPER_CASE.to("AaaAaa"));
		assertEquals("AAAAAA", NamingStyle.UPPER_CASE.to("aaaAaa"));
		assertEquals("AAA AAA", NamingStyle.UPPER_CASE.to("AAA AAA"));
		assertEquals("AAA_AAA", NamingStyle.UPPER_CASE.to("AAA_AAA"));
		assertEquals("AAA-AAA", NamingStyle.UPPER_CASE.to("AAA-AAA"));
		assertEquals("AAA AAA", NamingStyle.UPPER_CASE.to("aaa aaa"));
		assertEquals("AAA_AAA", NamingStyle.UPPER_CASE.to("aaa_aaa"));
		assertEquals("AAA-AAA", NamingStyle.UPPER_CASE.to("aaa-aaa"));
		assertEquals("あああAAA", NamingStyle.UPPER_CASE.to("あああAaa"));
		assertEquals("あああAAA", NamingStyle.UPPER_CASE.to("あああaaa"));
		assertEquals("Aあああああ", NamingStyle.UPPER_CASE.to("Aあああああ"));
		assertEquals("AああAAA", NamingStyle.UPPER_CASE.to("AああAaa"));
		assertEquals("Aあああああ", NamingStyle.UPPER_CASE.to("aあああああ"));
		assertEquals("AああAAA", NamingStyle.UPPER_CASE.to("aああAaa"));
		
		assertEquals("_AAAAAA", NamingStyle.UPPER_CASE.to("_aaaaaa"));
		assertEquals("_AAAAAA", NamingStyle.UPPER_CASE.to("_AAAAAA"));
		assertEquals("_AAAAAA", NamingStyle.UPPER_CASE.to("_Aaaaaa"));
		assertEquals("_AAAAAA", NamingStyle.UPPER_CASE.to("_AaaAaa"));
		assertEquals("_AAAAAA", NamingStyle.UPPER_CASE.to("_aaaAaa"));
		assertEquals("_AAA AAA", NamingStyle.UPPER_CASE.to("_AAA AAA"));
		assertEquals("_AAA_AAA", NamingStyle.UPPER_CASE.to("_AAA_AAA"));
		assertEquals("_AAA-AAA", NamingStyle.UPPER_CASE.to("_AAA-AAA"));
		assertEquals("_AAA AAA", NamingStyle.UPPER_CASE.to("_aaa aaa"));
		assertEquals("_AAA_AAA", NamingStyle.UPPER_CASE.to("_aaa_aaa"));
		assertEquals("_AAA-AAA", NamingStyle.UPPER_CASE.to("_aaa-aaa"));
		assertEquals("_あああAAA", NamingStyle.UPPER_CASE.to("_あああAaa"));
		assertEquals("_あああAAA", NamingStyle.UPPER_CASE.to("_あああaaa"));
		assertEquals("_Aあああああ", NamingStyle.UPPER_CASE.to("_Aあああああ"));
		assertEquals("_AああAAA", NamingStyle.UPPER_CASE.to("_AああAaa"));
		assertEquals("_Aあああああ", NamingStyle.UPPER_CASE.to("_aあああああ"));
		assertEquals("_AああAAA", NamingStyle.UPPER_CASE.to("_aああAaa"));
		
		assertEquals("AAAAAA_", NamingStyle.UPPER_CASE.to("aaaaaa_"));
		assertEquals("AAAAAA_", NamingStyle.UPPER_CASE.to("AAAAAA_"));
		assertEquals("AAAAAA_", NamingStyle.UPPER_CASE.to("Aaaaaa_"));
		assertEquals("AAAAAA_", NamingStyle.UPPER_CASE.to("AaaAaa_"));
		assertEquals("AAAAAA_", NamingStyle.UPPER_CASE.to("aaaAaa_"));
		assertEquals("AAA AAA_", NamingStyle.UPPER_CASE.to("AAA AAA_"));
		assertEquals("AAA_AAA_", NamingStyle.UPPER_CASE.to("AAA_AAA_"));
		assertEquals("AAA-AAA_", NamingStyle.UPPER_CASE.to("AAA-AAA_"));
		assertEquals("AAA AAA_", NamingStyle.UPPER_CASE.to("aaa aaa_"));
		assertEquals("AAA_AAA_", NamingStyle.UPPER_CASE.to("aaa_aaa_"));
		assertEquals("AAA-AAA_", NamingStyle.UPPER_CASE.to("aaa-aaa_"));
		assertEquals("あああAAA_", NamingStyle.UPPER_CASE.to("あああAaa_"));
		assertEquals("あああAAA_", NamingStyle.UPPER_CASE.to("あああaaa_"));
		assertEquals("Aあああああ_", NamingStyle.UPPER_CASE.to("Aあああああ_"));
		assertEquals("AああAAA_", NamingStyle.UPPER_CASE.to("AああAaa_"));
		assertEquals("Aあああああ_", NamingStyle.UPPER_CASE.to("aあああああ_"));
		assertEquals("AああAAA_", NamingStyle.UPPER_CASE.to("aああAaa_"));
	}

	@Test
	public void testUpperCamel() {
		assertEquals("Aaaaaa", NamingStyle.UPPER_CAMEL.to("aaaaaa"));
		assertEquals("Aaaaaa", NamingStyle.UPPER_CAMEL.to("AAAAAA"));
		assertEquals("Aaaaaa", NamingStyle.UPPER_CAMEL.to("Aaaaaa"));
		assertEquals("AaaAaa", NamingStyle.UPPER_CAMEL.to("AaaAaa"));
		assertEquals("AaaAaa", NamingStyle.UPPER_CAMEL.to("aaaAaa"));
		assertEquals("AaaAaa", NamingStyle.UPPER_CAMEL.to("AAA AAA"));
		assertEquals("AaaAaa", NamingStyle.UPPER_CAMEL.to("AAA_AAA"));
		assertEquals("AaaAaa", NamingStyle.UPPER_CAMEL.to("AAA-AAA"));
		assertEquals("AaaAaa", NamingStyle.UPPER_CAMEL.to("aaa aaa"));
		assertEquals("AaaAaa", NamingStyle.UPPER_CAMEL.to("aaa_aaa"));
		assertEquals("AaaAaa", NamingStyle.UPPER_CAMEL.to("aaa-aaa"));
		assertEquals("AaaAaa", NamingStyle.UPPER_CAMEL.to("AAA  AAA"));
		assertEquals("あああAaa", NamingStyle.UPPER_CAMEL.to("あああAaa"));
		assertEquals("あああaaa", NamingStyle.UPPER_CAMEL.to("あああaaa"));
		assertEquals("Aあああああ", NamingStyle.UPPER_CAMEL.to("Aあああああ"));
		assertEquals("AああAaa", NamingStyle.UPPER_CAMEL.to("AああAaa"));
		assertEquals("Aあああああ", NamingStyle.UPPER_CAMEL.to("aあああああ"));
		assertEquals("AああAaa", NamingStyle.UPPER_CAMEL.to("aああAaa"));
		
		assertEquals("_Aaaaaa", NamingStyle.UPPER_CAMEL.to("_aaaaaa"));
		assertEquals("_Aaaaaa", NamingStyle.UPPER_CAMEL.to("_AAAAAA"));
		assertEquals("_Aaaaaa", NamingStyle.UPPER_CAMEL.to("_Aaaaaa"));
		assertEquals("_AaaAaa", NamingStyle.UPPER_CAMEL.to("_AaaAaa"));
		assertEquals("_AaaAaa", NamingStyle.UPPER_CAMEL.to("_aaaAaa"));
		assertEquals("_AaaAaa", NamingStyle.UPPER_CAMEL.to("_AAA AAA"));
		assertEquals("_AaaAaa", NamingStyle.UPPER_CAMEL.to("_AAA_AAA"));
		assertEquals("_AaaAaa", NamingStyle.UPPER_CAMEL.to("_AAA-AAA"));
		assertEquals("@AaaAaa", NamingStyle.UPPER_CAMEL.to("@aaaAaa"));
		assertEquals("@@AaaAaa", NamingStyle.UPPER_CAMEL.to("@@AAA AAA"));
		assertEquals("@AaaAaa", NamingStyle.UPPER_CAMEL.to("@AAA_AAA"));
		assertEquals("@@AaaAaa", NamingStyle.UPPER_CAMEL.to("@@AAA-AAA"));
		assertEquals("_AaaAaa", NamingStyle.UPPER_CAMEL.to("_aaa aaa"));
		assertEquals("_AaaAaa", NamingStyle.UPPER_CAMEL.to("_aaa_aaa"));
		assertEquals("_AaaAaa", NamingStyle.UPPER_CAMEL.to("_aaa-aaa"));
		assertEquals("_AaaAaa", NamingStyle.UPPER_CAMEL.to("_AAA  AAA"));
		assertEquals("_あああAaa", NamingStyle.UPPER_CAMEL.to("_あああAaa"));
		assertEquals("_あああaaa", NamingStyle.UPPER_CAMEL.to("_あああaaa"));
		assertEquals("_Aあああああ", NamingStyle.UPPER_CAMEL.to("_Aあああああ"));
		assertEquals("_AああAaa", NamingStyle.UPPER_CAMEL.to("_AああAaa"));
		assertEquals("_Aあああああ", NamingStyle.UPPER_CAMEL.to("_aあああああ"));
		assertEquals("_AああAaa", NamingStyle.UPPER_CAMEL.to("_aああAaa"));
		
		assertEquals("Aaaaaa_", NamingStyle.UPPER_CAMEL.to("aaaaaa_"));
		assertEquals("Aaaaaa_", NamingStyle.UPPER_CAMEL.to("AAAAAA_"));
		assertEquals("Aaaaaa_", NamingStyle.UPPER_CAMEL.to("Aaaaaa_"));
		assertEquals("AaaAaa_", NamingStyle.UPPER_CAMEL.to("AaaAaa_"));
		assertEquals("AaaAaa_", NamingStyle.UPPER_CAMEL.to("aaaAaa_"));
		assertEquals("AaaAaa_", NamingStyle.UPPER_CAMEL.to("AAA AAA_"));
		assertEquals("AaaAaa_", NamingStyle.UPPER_CAMEL.to("AAA_AAA_"));
		assertEquals("AaaAaa_", NamingStyle.UPPER_CAMEL.to("AAA-AAA_"));
		assertEquals("AaaAaa@", NamingStyle.UPPER_CAMEL.to("aaaAaa@"));
		assertEquals("AaaAaa@@", NamingStyle.UPPER_CAMEL.to("AAA AAA@@"));
		assertEquals("AaaAaa@", NamingStyle.UPPER_CAMEL.to("AAA_AAA@"));
		assertEquals("AaaAaa@@", NamingStyle.UPPER_CAMEL.to("AAA-AAA@@"));
		assertEquals("AaaAaa_", NamingStyle.UPPER_CAMEL.to("aaa aaa_"));
		assertEquals("AaaAaa_", NamingStyle.UPPER_CAMEL.to("aaa_aaa_"));
		assertEquals("AaaAaa_", NamingStyle.UPPER_CAMEL.to("aaa-aaa_"));
		assertEquals("AaaAaa_", NamingStyle.UPPER_CAMEL.to("AAA  AAA_"));
		assertEquals("あああAaa_", NamingStyle.UPPER_CAMEL.to("あああAaa_"));
		assertEquals("あああaaa_", NamingStyle.UPPER_CAMEL.to("あああaaa_"));
		assertEquals("Aあああああ_", NamingStyle.UPPER_CAMEL.to("Aあああああ_"));
		assertEquals("AああAaa_", NamingStyle.UPPER_CAMEL.to("AああAaa_"));
		assertEquals("Aあああああ_", NamingStyle.UPPER_CAMEL.to("aあああああ_"));
		assertEquals("AああAaa_", NamingStyle.UPPER_CAMEL.to("aああAaa_"));
	}
	@Test
	public void testUpperUnderscore() {
		assertEquals("AAAAAA", NamingStyle.UPPER_UNDERSCORE.to("aaaaaa"));
		assertEquals("AAAAAA", NamingStyle.UPPER_UNDERSCORE.to("AAAAAA"));
		assertEquals("AAAAAA", NamingStyle.UPPER_UNDERSCORE.to("Aaaaaa"));
		assertEquals("AAA_AAA", NamingStyle.UPPER_UNDERSCORE.to("AaaAaa"));
		assertEquals("AAA_AAA", NamingStyle.UPPER_UNDERSCORE.to("aaaAaa"));
		assertEquals("AAA_AAA", NamingStyle.UPPER_UNDERSCORE.to("AAA AAA"));
		assertEquals("AAA_AAA", NamingStyle.UPPER_UNDERSCORE.to("AAA_AAA"));
		assertEquals("AAA_AAA", NamingStyle.UPPER_UNDERSCORE.to("AAA-AAA"));
		assertEquals("AAA_AAA", NamingStyle.UPPER_UNDERSCORE.to("aaa aaa"));
		assertEquals("AAA_AAA", NamingStyle.UPPER_UNDERSCORE.to("aaa_aaa"));
		assertEquals("AAA_AAA", NamingStyle.UPPER_UNDERSCORE.to("aaa-aaa"));
		assertEquals("AAA_AAA", NamingStyle.UPPER_UNDERSCORE.to("AAA  AAA"));
		assertEquals("あああ_AAA", NamingStyle.UPPER_UNDERSCORE.to("あああAaa"));
		assertEquals("あああAAA", NamingStyle.UPPER_UNDERSCORE.to("あああaaa"));
		assertEquals("Aあああああ", NamingStyle.UPPER_UNDERSCORE.to("Aあああああ"));
		assertEquals("Aああ_AAA", NamingStyle.UPPER_UNDERSCORE.to("AああAaa"));
		assertEquals("Aあああああ", NamingStyle.UPPER_UNDERSCORE.to("aあああああ"));
		assertEquals("Aああ_AAA", NamingStyle.UPPER_UNDERSCORE.to("aああAaa"));
		
		assertEquals("_AAAAAA", NamingStyle.UPPER_UNDERSCORE.to("_aaaaaa"));
		assertEquals("_AAAAAA", NamingStyle.UPPER_UNDERSCORE.to("_AAAAAA"));
		assertEquals("_AAAAAA", NamingStyle.UPPER_UNDERSCORE.to("_Aaaaaa"));
		assertEquals("_AAA_AAA", NamingStyle.UPPER_UNDERSCORE.to("_AaaAaa"));
		assertEquals("_AAA_AAA", NamingStyle.UPPER_UNDERSCORE.to("_aaaAaa"));
		assertEquals("_AAA_AAA", NamingStyle.UPPER_UNDERSCORE.to("_AAA AAA"));
		assertEquals("_AAA_AAA", NamingStyle.UPPER_UNDERSCORE.to("_AAA_AAA"));
		assertEquals("_AAA_AAA", NamingStyle.UPPER_UNDERSCORE.to("_AAA-AAA"));
		assertEquals("_AAA_AAA", NamingStyle.UPPER_UNDERSCORE.to("_aaa aaa"));
		assertEquals("_AAA_AAA", NamingStyle.UPPER_UNDERSCORE.to("_aaa_aaa"));
		assertEquals("_AAA_AAA", NamingStyle.UPPER_UNDERSCORE.to("_aaa-aaa"));
		assertEquals("_AAA_AAA", NamingStyle.UPPER_UNDERSCORE.to("_AAA  AAA"));
		assertEquals("_あああ_AAA", NamingStyle.UPPER_UNDERSCORE.to("_あああAaa"));
		assertEquals("_あああAAA", NamingStyle.UPPER_UNDERSCORE.to("_あああaaa"));
		assertEquals("_Aあああああ", NamingStyle.UPPER_UNDERSCORE.to("_Aあああああ"));
		assertEquals("_Aああ_AAA", NamingStyle.UPPER_UNDERSCORE.to("_AああAaa"));
		assertEquals("_Aあああああ", NamingStyle.UPPER_UNDERSCORE.to("_aあああああ"));
		assertEquals("_Aああ_AAA", NamingStyle.UPPER_UNDERSCORE.to("_aああAaa"));
		
		assertEquals("AAAAAA_", NamingStyle.UPPER_UNDERSCORE.to("aaaaaa_"));
		assertEquals("AAAAAA_", NamingStyle.UPPER_UNDERSCORE.to("AAAAAA_"));
		assertEquals("AAAAAA_", NamingStyle.UPPER_UNDERSCORE.to("Aaaaaa_"));
		assertEquals("AAA_AAA_", NamingStyle.UPPER_UNDERSCORE.to("AaaAaa_"));
		assertEquals("AAA_AAA_", NamingStyle.UPPER_UNDERSCORE.to("aaaAaa_"));
		assertEquals("AAA_AAA_", NamingStyle.UPPER_UNDERSCORE.to("AAA AAA_"));
		assertEquals("AAA_AAA_", NamingStyle.UPPER_UNDERSCORE.to("AAA_AAA_"));
		assertEquals("AAA_AAA_", NamingStyle.UPPER_UNDERSCORE.to("AAA-AAA_"));
		assertEquals("AAA_AAA_", NamingStyle.UPPER_UNDERSCORE.to("aaa aaa_"));
		assertEquals("AAA_AAA_", NamingStyle.UPPER_UNDERSCORE.to("aaa_aaa_"));
		assertEquals("AAA_AAA_", NamingStyle.UPPER_UNDERSCORE.to("aaa-aaa_"));
		assertEquals("AAA_AAA_", NamingStyle.UPPER_UNDERSCORE.to("AAA  AAA_"));
		assertEquals("あああ_AAA_", NamingStyle.UPPER_UNDERSCORE.to("あああAaa_"));
		assertEquals("あああAAA_", NamingStyle.UPPER_UNDERSCORE.to("あああaaa_"));
		assertEquals("Aあああああ_", NamingStyle.UPPER_UNDERSCORE.to("Aあああああ_"));
		assertEquals("Aああ_AAA_", NamingStyle.UPPER_UNDERSCORE.to("AああAaa_"));
		assertEquals("Aあああああ_", NamingStyle.UPPER_UNDERSCORE.to("aあああああ_"));
		assertEquals("Aああ_AAA_", NamingStyle.UPPER_UNDERSCORE.to("aああAaa_"));
	}
	
}
