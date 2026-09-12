package openmods.network.rpc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import openmods.network.senders.IPacketSender;

// 1.8.9 port of OpenModsLib RpcProxyFactory: identical proxy logic, method ids come
// from the local RpcMethodRegistry instead of the forge registry.
public class RpcProxyFactory {

	@SuppressWarnings("unchecked")
	public <T> T createProxy(ClassLoader loader, final IPacketSender sender, final IRpcTarget wrapper, Class<? extends T> mainIntf, Class<?>... extraIntf) {
		final Class<?>[] allInterfaces = new Class<?>[extraIntf.length + 1];
		allInterfaces[0] = mainIntf;
		System.arraycopy(extraIntf, 0, allInterfaces, 1, extraIntf.length);

		Object proxy = Proxy.newProxyInstance(loader, allInterfaces, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				final MethodEntry entry = RpcMethodRegistry.getEntry(method);
				if (entry != null) {
					RpcCall call = new RpcCall(wrapper, entry, args);
					sender.sendMessage(call);
				}
				return null;
			}
		});

		return (T)proxy;
	}
}
