/*
 * Minecraft Forge
 * Copyright (c) 2016-2019.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.common.data;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.server.BlockTagsProvider;
import net.minecraft.tag.Tag;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

import static net.minecraftforge.common.Tags.Blocks.*;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ForgeBlockTagsProvider extends BlockTagsProvider
{
    private Set<Identifier> filter = null;

    public ForgeBlockTagsProvider(DataGenerator gen)
    {
        super(gen);
    }

    @Override
    public void configure()
    {
        super.configure();
        filter = this.tagBuilders.entrySet().stream().map(e -> e.getKey().getId()).collect(Collectors.toSet());

        getOrCreateTagBuilder(CHESTS).add(CHESTS_ENDER, CHESTS_TRAPPED, CHESTS_WOODEN);
        getOrCreateTagBuilder(CHESTS_ENDER).add(Blocks.field_10443);
        getOrCreateTagBuilder(CHESTS_TRAPPED).add(Blocks.field_10380);
        getOrCreateTagBuilder(CHESTS_WOODEN).add(Blocks.field_10034, Blocks.field_10380);
        getOrCreateTagBuilder(COBBLESTONE).add(Blocks.field_10445, Blocks.field_10492, Blocks.field_9989);
        getOrCreateTagBuilder(DIRT).add(Blocks.field_10566, Blocks.field_10219, Blocks.field_10253, Blocks.field_10520, Blocks.field_10402);
        getOrCreateTagBuilder(END_STONES).add(Blocks.field_10471);
        getOrCreateTagBuilder(FENCE_GATES).add(FENCE_GATES_WOODEN);
        getOrCreateTagBuilder(FENCE_GATES_WOODEN).add(Blocks.field_10188, Blocks.field_10291, Blocks.field_10513, Blocks.field_10041, Blocks.field_10457, Blocks.field_10196);
        getOrCreateTagBuilder(FENCES).add(FENCES_NETHER_BRICK, FENCES_WOODEN);
        getOrCreateTagBuilder(FENCES_NETHER_BRICK).add(Blocks.field_10364);
        getOrCreateTagBuilder(FENCES_WOODEN).add(Blocks.field_10620, Blocks.field_10020, Blocks.field_10299, Blocks.field_10319, Blocks.field_10144, Blocks.field_10132);
        getOrCreateTagBuilder(GLASS).add(GLASS_COLORLESS, STAINED_GLASS);
        getOrCreateTagBuilder(GLASS_COLORLESS).add(Blocks.field_10033);
        addColored(getOrCreateTagBuilder(STAINED_GLASS)::add, GLASS, "{color}_stained_glass");
        getOrCreateTagBuilder(GLASS_PANES).add(GLASS_PANES_COLORLESS, STAINED_GLASS_PANES);
        getOrCreateTagBuilder(GLASS_PANES_COLORLESS).add(Blocks.field_10285);
        addColored(getOrCreateTagBuilder(STAINED_GLASS_PANES)::add, GLASS_PANES, "{color}_stained_glass_pane");
        getOrCreateTagBuilder(GRAVEL).add(Blocks.field_10255);
        getOrCreateTagBuilder(NETHERRACK).add(Blocks.field_10515);
        getOrCreateTagBuilder(OBSIDIAN).add(Blocks.field_10540);
        getOrCreateTagBuilder(ORES).add(ORES_COAL, ORES_DIAMOND, ORES_EMERALD, ORES_GOLD, ORES_IRON, ORES_LAPIS, ORES_REDSTONE, ORES_QUARTZ);
        getOrCreateTagBuilder(ORES_COAL).add(Blocks.field_10418);
        getOrCreateTagBuilder(ORES_DIAMOND).add(Blocks.field_10442);
        getOrCreateTagBuilder(ORES_EMERALD).add(Blocks.field_10013);
        getOrCreateTagBuilder(ORES_GOLD).add(Blocks.field_10571);
        getOrCreateTagBuilder(ORES_IRON).add(Blocks.field_10212);
        getOrCreateTagBuilder(ORES_LAPIS).add(Blocks.field_10090);
        getOrCreateTagBuilder(ORES_QUARTZ).add(Blocks.field_10213);
        getOrCreateTagBuilder(ORES_REDSTONE).add(Blocks.field_10080);
        getOrCreateTagBuilder(SAND).add(SAND_COLORLESS, SAND_RED);
        getOrCreateTagBuilder(SAND_COLORLESS).add(Blocks.field_10102);
        getOrCreateTagBuilder(SAND_RED).add(Blocks.field_10534);
        getOrCreateTagBuilder(SANDSTONE).add(Blocks.field_9979, Blocks.field_10361, Blocks.field_10292, Blocks.field_10467, Blocks.field_10344, Blocks.field_10518, Blocks.field_10117, Blocks.field_10483);
        getOrCreateTagBuilder(STONE).add(Blocks.field_10115, Blocks.field_10508, Blocks.field_10474, Blocks.field_10277, Blocks.field_10340, Blocks.field_10093, Blocks.field_10346, Blocks.field_10289);
        getOrCreateTagBuilder(STORAGE_BLOCKS).add(STORAGE_BLOCKS_COAL, STORAGE_BLOCKS_DIAMOND, STORAGE_BLOCKS_EMERALD, STORAGE_BLOCKS_GOLD, STORAGE_BLOCKS_IRON, STORAGE_BLOCKS_LAPIS, STORAGE_BLOCKS_QUARTZ, STORAGE_BLOCKS_REDSTONE);
        getOrCreateTagBuilder(STORAGE_BLOCKS_COAL).add(Blocks.field_10381);
        getOrCreateTagBuilder(STORAGE_BLOCKS_DIAMOND).add(Blocks.field_10201);
        getOrCreateTagBuilder(STORAGE_BLOCKS_EMERALD).add(Blocks.field_10234);
        getOrCreateTagBuilder(STORAGE_BLOCKS_GOLD).add(Blocks.field_10205);
        getOrCreateTagBuilder(STORAGE_BLOCKS_IRON).add(Blocks.field_10085);
        getOrCreateTagBuilder(STORAGE_BLOCKS_LAPIS).add(Blocks.field_10441);
        getOrCreateTagBuilder(STORAGE_BLOCKS_QUARTZ).add(Blocks.field_10153);
        getOrCreateTagBuilder(STORAGE_BLOCKS_REDSTONE).add(Blocks.field_10002);
    }

    private void addColored(Consumer<Block> consumer, Tag<Block> group, String pattern)
    {
        String prefix = group.getId().getPath().toUpperCase(Locale.ENGLISH) + '_';
        for (DyeColor color  : DyeColor.values())
        {
            Identifier key = new Identifier("minecraft", pattern.replace("{color}",  color.getName()));
            Tag<Block> tag = getForgeTag(prefix + color.getName());
            Block block = ForgeRegistries.BLOCKS.getValue(key);
            if (block == null || block  == Blocks.field_10124)
                throw new IllegalStateException("Unknown vanilla block: " + key.toString());
            getOrCreateTagBuilder(tag).add(block);
            consumer.accept(block);
        }
    }

    @SuppressWarnings("unchecked")
    private Tag<Block> getForgeTag(String name)
    {
        try
        {
            name = name.toUpperCase(Locale.ENGLISH);
            return (Tag<Block>)Tags.Blocks.class.getDeclaredField(name).get(null);
        }
        catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e)
        {
            throw new IllegalStateException(Tags.Blocks.class.getName() + " is missing tag name: " + name);
        }
    }

    @Override
    protected Path getOutput(Identifier id)
    {
        return filter != null && filter.contains(id) ? null : super.getOutput(id); //We don't want to save vanilla tags.
    }

    @Override
    public String getName()
    {
        return "Forge Block Tags";
    }
}
