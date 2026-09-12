package openmods.network.rpc;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.lang.reflect.Method;
import java.util.Map;

// 1.8.9 replacement for the 1.12.2 forge-registry method table: plain static map
// keyed like the original (declaring class + name + descriptor) to integer ids,
// filled by explicit registerInterface calls. Proxy and dispatch code is unchanged.
public class RpcMethodRegistry {

	private static final Map<String, MethodEntry> methods = Maps.newHashMap();
	private static final Map<Integer, MethodEntry> idToEntry = Maps.newHashMap();
	private static int nextId = 0;

	private static String keyOf(Method m) {
		final StringBuilder params = new StringBuilder();
		for (Class<?> p : m.getParameterTypes())
			params.append(p.getName()).append(';');
		return m.getDeclaringClass().getName() + "#" + m.getName() + "(" + params + ")";
	}

	public static synchronized void registerInterface(Class<?> intf) {
		Preconditions.checkArgument(intf.isInterface(), "Class %s is not interface", intf);
		for (Method m : intf.getMethods()) {
			final String key = keyOf(m);
			if (!methods.containsKey(key)) {
				final MethodEntry entry = new MethodEntry(nextId, m);
				methods.put(key, entry);
				idToEntry.put(nextId, entry);
				nextId++;
			}
		}
	}

	public static MethodEntry getEntry(Method m) {
		return methods.get(keyOf(m));
	}

	public static MethodEntry getEntry(int id) {
		return idToEntry.get(id);
	}
}
