package net.minecraftforge.fml.packs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import net.minecraft.resource.AbstractFileResourcePack;

public abstract class DelegatableResourcePack extends AbstractFileResourcePack {
	protected DelegatableResourcePack(File resourcePackFileIn) {
		super(resourcePackFileIn);
	}

	@Override
	public abstract InputStream openFile(String resourcePath) throws IOException;

	@Override
	public abstract boolean containsFile(String resourcePath);
}
