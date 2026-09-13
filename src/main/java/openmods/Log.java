package openmods;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// Minimal 1.8.9 equivalent of OpenModsLib Log: only the warn/severe shapes used
// by the ported code. Backend is log4j (project convention).
public class Log {

	private static final Logger log = LogManager.getLogger("OpenBlocks");

	public static void warn(String message, Object... args) {
		log.warn(String.format(message, args));
	}

	public static void warn(Throwable e, String message, Object... args) {
		log.warn(String.format(message, args), e);
	}

	public static void severe(Throwable e, String message, Object... args) {
		log.error(String.format(message, args), e);
	}
}
