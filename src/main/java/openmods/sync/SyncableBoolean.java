package openmods.sync;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

// 1.8.9 port of OpenModsLib SyncableBoolean: verbatim (only 1.12.2-isms were the
// package layout, which is preserved). Registered in OpenBlocks.preInit like the
// other sync types. First used by the Vacuum Hopper (vacuumDisabled flag).
public class SyncableBoolean extends SyncableObjectBase implements ISyncableValueProvider<Boolean> {

	private boolean value;

	public SyncableBoolean(boolean value) {
		this.value = value;
	}

	public SyncableBoolean() {}

	public void set(boolean newValue) {
		if (newValue != value) {
			value = newValue;
			markDirty();
		}
	}

	public boolean get() {
		return value;
	}

	@Override
	public Boolean getValue() {
		return value;
	}

	@Override
	public void readFromStream(PacketBuffer stream) {
		value = stream.readBoolean();
	}

	@Override
	public void writeToStream(PacketBuffer stream) {
		stream.writeBoolean(value);
	}

	@Override
	public void writeToNBT(NBTTagCompound tag, String name) {
		tag.setBoolean(name, value);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag, String name) {
		value = tag.getBoolean(name);
	}

	public void toggle() {
		value = !value;
		markDirty();
	}
}
