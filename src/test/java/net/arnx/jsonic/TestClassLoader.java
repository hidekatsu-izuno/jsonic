package net.arnx.jsonic;

public class TestClassLoader extends ClassLoader {
	public TestClassLoader(ClassLoader parent) {
		super(parent);
	}

	public void setVulnerability(boolean flag) {
		throw new IllegalStateException("Vulnerability!");
	}

	public static class TestBean {
		public TestBean() {
		}

		@Override
		public int hashCode() {
			return 1;
		}

		@Override
		public boolean equals(Object obj) {
			return (obj instanceof TestBean);
		}
	}
}
