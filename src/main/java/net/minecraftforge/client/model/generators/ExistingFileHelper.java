package net.minecraftforge.client.model.generators;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

import com.google.common.annotations.VisibleForTesting;

import net.minecraft.resource.ZipResourcePack;
import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.resource.DefaultResourcePack;
import net.minecraft.util.Identifier;

/**
 * Enables data providers to check if other data files currently exist. The
 * instance provided in the {@link GatherDataEvent} utilizes the standard
 * resources (via {@link VanillaPack}), as well as any extra resource packs
 * passed in via the {@code --existing} argument.
 */
public class ExistingFileHelper {

	private final ResourceManager clientResources, serverData;
	private final boolean enable;

	public ExistingFileHelper(Collection<Path> existingPacks, boolean enable) {
		this.clientResources = new ReloadableResourceManagerImpl(ResourceType.CLIENT_RESOURCES, Thread.currentThread());
		this.serverData = new ReloadableResourceManagerImpl(ResourceType.SERVER_DATA, Thread.currentThread());
		this.clientResources.addPack(new DefaultResourcePack("minecraft", "realms"));
		this.serverData.addPack(new DefaultResourcePack("minecraft"));
		for (Path existing : existingPacks) {
			File file = existing.toFile();
			ResourcePack pack = file.isDirectory() ? new DirectoryResourcePack(file) : new ZipResourcePack(file);
			this.clientResources.addPack(pack);
			this.serverData.addPack(pack);
		}
		this.enable = enable;
	}

	private ResourceManager getManager(ResourceType type) {
		return type == ResourceType.CLIENT_RESOURCES ? clientResources : serverData;
	}

	private Identifier getLocation(Identifier base, String suffix, String prefix) {
		return new Identifier(base.getNamespace(), prefix + "/" + base.getPath() + suffix);
	}

	/**
	 * Check if a given resource exists in the known resource packs.
	 *
	 * @param loc        the base location of the resource, e.g.
	 *                   {@code "minecraft:block/stone"}
	 * @param type       the type of resources to check
	 * @param pathSuffix a string to append after the path, e.g. {@code ".json"}
	 * @param pathPrefix a string to append before the path, before a slash, e.g.
	 *                   {@code "models"}
	 * @return {@code true} if the resource exists in any pack, {@code false}
	 * otherwise
	 */
	public boolean exists(Identifier loc, ResourceType type, String pathSuffix, String pathPrefix) {
		if (!enable) {
			return true;
		}
		return getManager(type).containsResource(getLocation(loc, pathSuffix, pathPrefix));
	}

	@VisibleForTesting
	public Resource getResource(Identifier loc, ResourceType type, String pathSuffix, String pathPrefix) throws IOException {
		return getManager(type).getResource(getLocation(loc, pathSuffix, pathPrefix));
	}

	/**
	 * @return {@code true} if validation is enabled, {@code false} otherwise
	 */
	public boolean isEnabled() {
		return enable;
	}
}
