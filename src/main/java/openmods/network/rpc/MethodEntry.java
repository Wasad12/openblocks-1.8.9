package openmods.network.rpc;

import com.google.common.base.Preconditions;
import java.lang.reflect.Method;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;

// 1.8.9 port of OpenModsLib MethodEntry: the 1.12.2 version is a forge-registry entry
// carrying a SerializerRegistry codec. 1.8.9 has no data registries, so methods are
// identified by a local integer table (RpcMethodRegistry) and args use a tiny typed
// codec covering exactly the RPC types the ported code uses (int, boolean, EnumFacing,
// enums). RPC call sites are unchanged.
public class MethodEntry {

	public final int id;

	public final Method method;

	public MethodEntry(int id, Method method) {
		this.id = id;
		this.method = method;
		Preconditions.checkArgument(method.getReturnType() == void.class, "RPC methods cannot have return type (method = %s)", method);
	}

	public void writeArgs(PacketBuffer output, Object... args) {
		final Class<?>[] types = method.getParameterTypes();
		Preconditions.checkArgument(args.length == types.length,
				"Argument list length mismatch, expected %d, got %d", types.length, args.length);
		for (int i = 0; i < args.length; i++)
			writeArg(output, types[i], args[i]);
	}

	private static void writeArg(PacketBuffer output, Class<?> type, Object value) {
		if (type == int.class || type == Integer.class) {
			output.writeInt((Integer)value);
		} else if (type == boolean.class || type == Boolean.class) {
			output.writeBoolean((Boolean)value);
		} else if (type == EnumFacing.class) {
			output.writeVarIntToBuffer(((EnumFacing)value).ordinal());
		} else if (type.isEnum()) {
			output.writeVarIntToBuffer(((Enum<?>)value).ordinal());
		} else {
			throw new IllegalArgumentException("Unsupported RPC arg type: " + type);
		}
	}

	public Object[] readArgs(PacketBuffer input) {
		final Class<?>[] types = method.getParameterTypes();
		if (types.length == 0) return null;

		final Object[] result = new Object[types.length];
		for (int i = 0; i < types.length; i++)
			result[i] = readArg(input, types[i]);
		return result;
	}

	private static Object readArg(PacketBuffer input, Class<?> type) {
		if (type == int.class || type == Integer.class) {
			final int value = input.readInt();
			return type == int.class? value : Integer.valueOf(value);
		} else if (type == boolean.class || type == Boolean.class) {
			final boolean value = input.readBoolean();
			return type == boolean.class? value : Boolean.valueOf(value);
		} else if (type == EnumFacing.class) {
			return EnumFacing.VALUES[input.readVarIntFromBuffer()];
		} else if (type.isEnum()) {
			return type.getEnumConstants()[input.readVarIntFromBuffer()];
		} else {
			throw new IllegalArgumentException("Unsupported RPC arg type: " + type);
		}
	}

	@Override
	public String toString() {
		return "Method{" + method + "}";
	}
}
