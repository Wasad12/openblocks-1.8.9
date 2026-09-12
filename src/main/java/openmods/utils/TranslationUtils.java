package openmods.utils;

import net.minecraft.util.StatCollector;

// 1.8.9 port of OpenModsLib TranslationUtils: 1.8.9 has no util.text.translation.I18n,
// the same calls live on StatCollector. Signatures unchanged.
public class TranslationUtils {

	public static String translateToLocal(String key) {
		return StatCollector.translateToLocal(key);
	}

	public static String translateToLocalFormatted(String key, Object... args) {
		return StatCollector.translateToLocalFormatted(key, args);
	}

}
