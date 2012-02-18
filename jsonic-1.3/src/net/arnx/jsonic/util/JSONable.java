package net.arnx.jsonic.util;

public interface JSONable {
	/**
	 * Never suppress null.
	 */
	public static final JSONable NULL = new JSONable() {
		@Override
		public String toJSON() {
			return "null";
		}
		
		@Override
		public String toString() {
			return "null";
		}
	};
	
	public String toJSON();
}
