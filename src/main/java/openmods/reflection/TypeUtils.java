package openmods.reflection;

import com.google.common.base.Preconditions;

// 1.8.9 port of OpenModsLib TypeUtils: only isInstance is needed by the ported RPC
// code (generic type-variable machinery dropped per §19).
public class TypeUtils {

	public static void isInstance(Object o, Class<?> mainCls, Class<?>... extraCls) {
		Preconditions.checkArgument(mainCls.isInstance(o), "%s is not instance of %s", o, mainCls);
		for (Class<?> cls : extraCls)
			Preconditions.checkArgument(cls.isInstance(o), "%s is not instance of %s", o, cls);
	}
}
