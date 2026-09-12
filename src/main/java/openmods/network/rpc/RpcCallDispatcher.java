package openmods.network.rpc;

import java.util.Collection;
import openblocks.common.network.OpenBlocksChannel;
import openmods.network.senders.IPacketSender;

// 1.8.9 port of OpenModsLib RpcCallDispatcher: same singleton + senders + createProxy
// seams, so all RPC call sites port verbatim. The 1.12.2 FML embedded channel and
// method/target data registries don't exist in 1.8.9 — the client sender routes
// RpcCall through OpenBlocksChannel (SimpleNetworkWrapper) and method ids come
// from the local RpcMethodRegistry (registered in preInit).
public class RpcCallDispatcher {

	private static RpcCallDispatcher INSTANCE;

	public static RpcCallDispatcher instance() {
		return INSTANCE;
	}

	public static void init() {
		if (INSTANCE == null) INSTANCE = new RpcCallDispatcher();
	}

	public final Senders senders = new Senders();

	private final RpcProxyFactory proxyFactory = new RpcProxyFactory();

	public <T> T createProxy(IRpcTarget wrapper, IPacketSender sender, Class<? extends T> mainIntf, Class<?>... extraIntf) {
		return proxyFactory.createProxy(getClass().getClassLoader(), sender, wrapper, mainIntf, extraIntf);
	}

	public static class Senders {
		public final IPacketSender client = new IPacketSender() {
			@Override
			public void sendMessage(Object msg) {
				OpenBlocksChannel.sendRpcCall((RpcCall)msg);
			}

			@Override
			public void sendMessages(Collection<Object> msgs) {
				for (Object msg : msgs)
					sendMessage(msg);
			}
		};
	}
}
