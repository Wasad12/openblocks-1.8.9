package openmods.utils;

import com.google.common.base.Strings;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import openmods.api.IValueReceiver;

// 1.8.9 port of OpenModsLib MiscUtils: only unhandledEnum is needed by the ported
// feature code (URL/holiday/fluid helpers dropped per §19).
public class MiscUtils {

	public static RuntimeException unhandledEnum(Enum<?> e) {
		throw new IllegalArgumentException(e.toString());
	}

	public static <T> IValueReceiver<T> createTextValueReceiver(final IValueReceiver<String> target) {
		return value -> target.setValue(value != null? value.toString() : null);
	}

	public static String getTranslatedFluidName(FluidStack fluidStack) {
		if (fluidStack == null) return "";
		final Fluid fluid = fluidStack.getFluid();
		String localizedName = fluid.getLocalizedName(fluidStack);
		if (!Strings.isNullOrEmpty(localizedName) && !localizedName.equals(fluid.getUnlocalizedName())) {
			return fluid.getRarity(fluidStack).rarityColor.toString() + localizedName;
		} else {
			return EnumChatFormatting.OBFUSCATED + "LOLNOPE" + EnumChatFormatting.RESET;
		}
	}
}
