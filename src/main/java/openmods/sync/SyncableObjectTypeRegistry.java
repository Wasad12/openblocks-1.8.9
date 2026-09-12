package openmods.sync;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Map;
import net.minecraft.util.ResourceLocation;

// 1.8.9 port of OpenModsLib SyncableObjectTypeRegistry: 1.8.9 Forge has no data
// registries, so this is a plain static table filled by explicit register() calls
// in preInit. The static query API (getType/getTypeId) is unchanged, so all
// SyncMap wire code ports verbatim.
public class SyncableObjectTypeRegistry {

	private static final BiMap<SyncableObjectType, Integer> typeToId = HashBiMap.create();
	private static final Map<Class<? extends ISyncableObject>, SyncableObjectType> classToType = Maps.newHashMap();
	private static int nextId = 0;

	public static synchronized void register(Class<? extends ISyncableObject> cls, Supplier<ISyncableObject> supplier) {
		Preconditions.checkState(!Modifier.isAbstract(cls.getModifiers()), "Class %s is abstract", cls);

		final ResourceLocation typeId = new ResourceLocation("openblocks", cls.getName());
		final SyncableObjectType type = new SyncableObjectType() {
			@Override
			public Class<? extends ISyncableObject> getObjectClass() {
				return cls;
			}

			@Override
			public ISyncableObject createDummyObject() {
				return supplier.get();
			}

			@Override
			public String toString() {
				return "Wrapper{" + cls + "}";
			}
		};
		type.setRegistryName(typeId);
		typeToId.put(type, nextId++);
		classToType.put(cls, type);
	}

	public static void register(Class<? extends ISyncableObject> cls) {
		Preconditions.checkState(!Modifier.isAbstract(cls.getModifiers()), "Class %s is abstract", cls);

		final Constructor<? extends ISyncableObject> ctor;
		try {
			ctor = cls.getConstructor();
		} catch (Exception e) {
			throw new IllegalArgumentException("Class " + cls + " has no parameterless constructor");
		}

		register(cls, () -> {
			try {
				return ctor.newInstance();
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		});
	}

	public static SyncableObjectType getType(int typeId) {
		return typeToId.inverse().get(typeId);
	}

	public static int getTypeId(SyncableObjectType type) {
		return typeToId.get(type);
	}

	public static SyncableObjectType getType(Class<? extends ISyncableObject> cls) {
		return classToType.get(cls);
	}
}
