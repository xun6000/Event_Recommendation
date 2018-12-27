package rpc;

import static org.junit.Assert.*;

import org.junit.Test;

public class StringTest {
	@Test
	public void testSubString0(){
		String str = getString();
		assertEquals("unit", str.substring(10, 14));
	}
	
	@Test
	public void testSubString1(){
		String str = getString();
		assertEquals("This", str.substring(0, 4));
	}
	
	@Test
	public void testSubString2(){
		String str = getString();
		assertEquals("unit", str.substring(10, 15));
	}
	
	private String getString() {
		return new String("This is a unit test.");
	}
}

