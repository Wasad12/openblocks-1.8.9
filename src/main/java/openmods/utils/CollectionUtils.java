package openmods.utils;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Random;

// 1.8.9 port of OpenModsLib CollectionUtils: only the helpers needed by the ported
// feature code (random/first selection, array transform). Packet/optional/stream
// helpers dropped per §19.
public class CollectionUtils {

	public static final Random rnd = new Random();

	public static <T> T getFirst(Collection<T> collection) {
		Preconditions.checkArgument(!collection.isEmpty(), "Collection cannot be empty");
		return collection.iterator().next();
	}

	public static <T> T getRandom(Collection<T> collection) {
		return getRandom(collection, rnd);
	}

	public static <T> T getRandom(Collection<T> collection, Random rand) {
		final int size = collection.size();
		Preconditions.checkArgument(size > 0, "Can't select from empty collection");
		if (size == 1) return getFirst(collection);
		int randomIndex = rnd.nextInt(size);
		int i = 0;
		for (T obj : collection) {
			if (i == randomIndex) return obj;
			i = i + 1;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <A, B> B[] transform(Class<? extends B> cls, A[] input, Function<A, B> transformer) {
		final B[] result = (B[])Array.newInstance(cls, input.length);
		for (int i = 0; i < input.length; i++)
			result[i] = transformer.apply(input[i]);
		return result;
	}
}
