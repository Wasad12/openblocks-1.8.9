package openmods.tileentity;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.Set;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import openmods.network.rpc.IRpcTarget;
import openmods.network.rpc.RpcCallDispatcher;
import openmods.network.rpc.targets.SyncRpcTarget;
import openmods.network.senders.IPacketSender;
import openmods.reflection.TypeUtils;
import openmods.sync.ISyncListener;
import openmods.sync.ISyncMapProvider;
import openmods.sync.ISyncableObject;
import openmods.sync.SyncMap;
import openmods.sync.SyncMapClient;
import openmods.sync.SyncMapServer.UpdateStrategy;
import openmods.sync.SyncMapTile;
import openmods.sync.SyncObjectScanner;

// 1.8.9 port of OpenModsLib SyncedTileEntity: same SyncMap lifecycle + init-data
// description packets + RPC proxy seams. 1.8.9 adaptations: S35PacketUpdateTileEntity
// (no getUpdateTag/handleUpdateTag/SPacketUpdateTileEntity in 1.8.9), setWorldObj as
// the map-creation hook (no setWorldCreate in 1.8.9; idempotent with validate).
// getDropSerializer dropped per §19 (unused by ported features).
public abstract class SyncedTileEntity extends OpenTileEntity implements ISyncMapProvider {

	private static final String TAG_SYNC_INIT = "SyncInit";

	private SyncMap syncMap;

	public SyncedTileEntity() {
		createSyncedFields();
	}

	private void createSyncMap(World world) {
		if (syncMap != null) return;
		if (world == null) return;
		final SyncMap syncMap = world.isRemote? new SyncMapClient() : new SyncMapTile(this, UpdateStrategy.WITH_INITIAL_PACKET);

		SyncObjectScanner.INSTANCE.registerAllFields(syncMap, this);

		syncMap.addSyncListener(new ISyncListener() {
			@Override
			public void onSync(Set<ISyncableObject> changes) {
				markUpdated();
			}
		});

		this.syncMap = syncMap;
		onSyncMapCreate(syncMap);
	}

	protected void onSyncMapCreate(SyncMap syncMap) {}

	@Override
	public void setWorldObj(World worldIn) {
		super.setWorldObj(worldIn);
		createSyncMap(worldIn);
	}

	@Override
	public void validate() {
		super.validate();
		createSyncMap(worldObj);
	}

	protected ISyncListener createRenderUpdateListener() {
		return new ISyncListener() {
			@Override
			public void onSync(Set<ISyncableObject> changes) {
				markBlockForRenderUpdate(getPos());
			}
		};
	}

	protected ISyncListener createRenderUpdateListener(final ISyncableObject target) {
		return new ISyncListener() {
			@Override
			public void onSync(Set<ISyncableObject> changes) {
				if (changes.contains(target)) markBlockForRenderUpdate(getPos());
			}
		};
	}

	protected ISyncListener createRenderUpdateListener(final Set<ISyncableObject> targets) {
		return new ISyncListener() {
			@Override
			public void onSync(Set<ISyncableObject> changes) {
				if (!Sets.intersection(changes, targets).isEmpty()) markBlockForRenderUpdate(getPos());
			}
		};
	}

	protected void markBlockForRenderUpdate(BlockPos pos) {
		worldObj.markBlockRangeForRenderUpdate(pos, pos);
	}

	protected abstract void createSyncedFields();

	public void sync() {
		getSyncMap().sendUpdates();
	}

	public boolean trySync() {
		return getSyncMap().trySendUpdates();
	}

	@Override
	public SyncMap getSyncMap() {
		Preconditions.checkState(syncMap != null, "Tile entity not initialized properly");
		return syncMap;
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		if (syncMap != null) getSyncMap().tryWrite(tag);
		else writeFieldsToNBT(tag);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		// NOTE: on 1.8.9 chunk-load NBT read precedes setWorldObj, so the map may
		// not exist yet. The fields themselves are created in the constructor, so
		// read them directly (same per-field NBT layout as SyncMapServer.read).
		if (syncMap != null) getSyncMap().tryRead(tag);
		else readFieldsFromNBT(tag);
	}

	private void writeFieldsToNBT(NBTTagCompound tag) {
		for (java.lang.reflect.Field field : SyncObjectScanner.INSTANCE.getFields(getClass())) {
			try {
				((ISyncableObject)field.get(this)).writeToNBT(tag, field.getName());
			} catch (Exception e) {
				throw new SyncMap.SyncFieldException(e, field.getName());
			}
		}
	}

	private void readFieldsFromNBT(NBTTagCompound tag) {
		for (java.lang.reflect.Field field : SyncObjectScanner.INSTANCE.getFields(getClass())) {
			try {
				((ISyncableObject)field.get(this)).readFromNBT(tag, field.getName());
			} catch (Exception e) {
				throw new SyncMap.SyncFieldException(e, field.getName());
			}
		}
	}

	private NBTTagCompound serializeInitializationData(NBTTagCompound tag) {
		final PacketBuffer tmp = new PacketBuffer(Unpooled.buffer());
		try {
			getSyncMap().writeInitializationData(tmp);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		byte[] data = new byte[tmp.readableBytes()];
		tmp.readBytes(data);
		tag.setByteArray(TAG_SYNC_INIT, data);

		return tag;
	}

	private void applyInitializationData(NBTTagCompound tag) {
		if (tag.hasKey(TAG_SYNC_INIT, Constants.NBT.TAG_BYTE_ARRAY)) {
			final byte[] syncInit = tag.getByteArray(TAG_SYNC_INIT);
			final PacketBuffer tmp = new PacketBuffer(Unpooled.buffer());
			tmp.writeBytes(syncInit);

			try {
				getSyncMap().readIntializationData(tmp);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public S35PacketUpdateTileEntity getDescriptionPacket() {
		return new S35PacketUpdateTileEntity(getPos(), 43, serializeInitializationData(new NBTTagCompound()));
	}

	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
		applyInitializationData(pkt.getNbtCompound());
	}

	public <T> T createRpcProxy(ISyncableObject object, Class<? extends T> mainIntf, Class<?>... extraIntf) {
		TypeUtils.isInstance(object, mainIntf, extraIntf);
		IRpcTarget target = new SyncRpcTarget.SyncTileEntityRpcTarget(this, object);
		final IPacketSender sender = RpcCallDispatcher.instance().senders.client;
		return RpcCallDispatcher.instance().createProxy(target, sender, mainIntf, extraIntf);
	}
}
