package net.arnx.jsonic.internal.formatter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.JSONHint;
import net.arnx.jsonic.internal.io.OutputSource;
import net.arnx.jsonic.internal.util.ClassUtil;

public class DynaBeanFormatter implements Formatter {
	public static final DynaBeanFormatter INSTANCE = new DynaBeanFormatter();

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		out.append('{');
		int count = 0;
		try {
			Class<?> dynaBeanClass = ClassUtil
					.findClass("org.apache.commons.beanutils.DynaBean");

			Object dynaClass = dynaBeanClass.getMethod("getDynaClass")
					.invoke(o);
			Object[] dynaProperties = (Object[]) dynaClass.getClass()
					.getMethod("getDynaProperties").invoke(dynaClass);

			if (dynaProperties != null && dynaProperties.length > 0) {
				Method getName = dynaProperties[0].getClass().getMethod(
						"getName");
				Method get = dynaBeanClass.getMethod("get", String.class);
				
				JSONHint hint = context.getHint();
				for (Object dp : dynaProperties) {
					Object name = null;
					try {
						name = getName.invoke(dp);
					} catch (InvocationTargetException e) {
						throw e;
					} catch (Exception e) {
					}
					if (name == null)
						continue;

					Object value = null;
					Exception cause = null;

					try {
						value = get.invoke(o, name);
					} catch (Exception e) {
						cause = e;
					}

					if (value == src
							|| (cause == null && context.isSuppressNull() && value == null)) {
						continue;
					}

					if (count != 0)
						out.append(',');
					if (context.isPrettyPrint()) {
						out.append('\n');
						for (int j = 0; j < context.getDepth() + 1; j++)
							out.append('\t');
					}
					StringFormatter.serialize(context, name.toString(), out);
					out.append(':');
					if (context.isPrettyPrint())
						out.append(' ');
					context.enter(name, hint);
					if (cause != null)
						throw cause;
					value = context.preformat(value);
					context.format(value, out);
					context.exit();
					count++;
				}
			}
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof Error) {
				throw (Error) e.getCause();
			} else if (e.getCause() instanceof RuntimeException) {
				throw (RuntimeException) e.getCause();
			} else {
				throw (Exception) e.getCause();
			}
		} catch (Exception e) {
			// no handle
		}
		if (context.isPrettyPrint() && count > 0) {
			out.append('\n');
			for (int j = 0; j < context.getDepth(); j++)
				out.append('\t');
		}
		out.append('}');
		return true;
	}
}