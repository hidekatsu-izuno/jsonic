package net.arnx.jsonic;

import net.arnx.jsonic.util.PropertyInfo;

class ExtendPropertyInfo extends PropertyInfo {
	private int order = -1;
	
	public ExtendPropertyInfo(PropertyInfo base, String name, 
			boolean useField, boolean useMethod, int order) {
		super(base.getBeanClass(), name, 
				useField ? base.getField() : null, 
				useMethod ? base.getReadMethod() : null,
				useMethod ? base.getWriteMethod() : null, 
				base.isStatic());
		this.order = order;
	}
	
	@Override
	public int compareTo(PropertyInfo property) {
		if (property instanceof ExtendPropertyInfo) {
			ExtendPropertyInfo eprop = (ExtendPropertyInfo)property;
			if (order >= 0) {
				if (eprop.order >= 0) {
					if (order > eprop.order) {
						return 1;
					} else if (order < eprop.order) {
						return -1;
					}
				}
			} else if (eprop.order >= 0) {
				return -1;
			}
		}
		
		return super.compareTo(property);
	}
}
