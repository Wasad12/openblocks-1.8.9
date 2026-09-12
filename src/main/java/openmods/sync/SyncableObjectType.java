package openmods.sync;

import net.minecraft.util.ResourceLocation;

// 1.8.9 port of OpenModsLib SyncableObjectType: the 1.12.2 IForgeRegistryEntry
// machinery doesn't exist in 1.8.9, so the name is a plain field managed by the
// local SyncableObjectTypeRegistry. Wire protocol (numeric type ids) unchanged.
public abstract class SyncableObjectType {

	private ResourceLocation name;

	public abstract ISyncableObject createDummyObject();

	public abstract Class<? extends ISyncableObject> getObjectClass();

	public boolean isValidType(ISyncableObject object) {
		return getObjectClass().isInstance(object);
	}

	public SyncableObjectType setRegistryName(ResourceLocation name) {
		this.name = name;
		return this;
	}

	public ResourceLocation getRegistryName() {
		return name;
	}
}
