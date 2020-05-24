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

package net.minecraftforge.common;

import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.*;
import static net.minecraftforge.common.BiomeDictionary.Type.*;
import net.minecraftforge.registries.ForgeRegistries;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class BiomeDictionary
{
    private static final boolean DEBUG = false;
    private static final Logger LOGGER = LogManager.getLogger();

    public static final class Type
    {

        private static final Map<String, Type> byName = new HashMap<String, Type>();
        private static Collection<Type> allTypes = Collections.unmodifiableCollection(byName.values());

        /*Temperature-based tags. Specifying neither implies a biome is temperate*/
        public static final Type HOT = new Type("HOT");
        public static final Type COLD = new Type("COLD");

        /*Tags specifying the amount of vegetation a biome has. Specifying neither implies a biome to have moderate amounts*/
        public static final Type SPARSE = new Type("SPARSE");
        public static final Type DENSE = new Type("DENSE");

        /*Tags specifying how moist a biome is. Specifying neither implies the biome as having moderate humidity*/
        public static final Type WET = new Type("WET");
        public static final Type DRY = new Type("DRY");

        /*Tree-based tags, SAVANNA refers to dry, desert-like trees (Such as Acacia), CONIFEROUS refers to snowy trees (Such as Spruce) and JUNGLE refers to jungle trees.
         * Specifying no tag implies a biome has temperate trees (Such as Oak)*/
        public static final Type SAVANNA = new Type("SAVANNA");
        public static final Type CONIFEROUS = new Type("CONIFEROUS");
        public static final Type JUNGLE = new Type("JUNGLE");

        /*Tags specifying the nature of a biome*/
        public static final Type SPOOKY = new Type("SPOOKY");
        public static final Type DEAD = new Type("DEAD");
        public static final Type LUSH = new Type("LUSH");
        public static final Type MUSHROOM = new Type("MUSHROOM");
        public static final Type MAGICAL = new Type("MAGICAL");
        public static final Type RARE = new Type("RARE");
        public static final Type PLATEAU = new Type("PLATEAU");
        public static final Type MODIFIED = new Type("MODIFIED");

        public static final Type OCEAN = new Type("OCEAN");
        public static final Type RIVER = new Type("RIVER");
        /**
         * A general tag for all water-based biomes. Shown as present if OCEAN or RIVER are.
         **/
        public static final Type WATER = new Type("WATER", OCEAN, RIVER);

        /*Generic types which a biome can be*/
        public static final Type MESA = new Type("MESA");
        public static final Type FOREST = new Type("FOREST");
        public static final Type PLAINS = new Type("PLAINS");
        public static final Type MOUNTAIN = new Type("MOUNTAIN");
        public static final Type HILLS = new Type("HILLS");
        public static final Type SWAMP = new Type("SWAMP");
        public static final Type SANDY = new Type("SANDY");
        public static final Type SNOWY = new Type("SNOWY");
        public static final Type WASTELAND = new Type("WASTELAND");
        public static final Type BEACH = new Type("BEACH");
        public static final Type VOID = new Type("VOID");

        /*Tags specifying the dimension a biome generates in. Specifying none implies a biome that generates in a modded dimension*/
        public static final Type OVERWORLD = new Type("OVERWORLD");
        public static final Type NETHER = new Type("NETHER");
        public static final Type END = new Type("END");

        private final String name;
        private final List<Type> subTypes;
        private final Set<Biome> biomes = new HashSet<Biome>();
        private final Set<Biome> biomesUn = Collections.unmodifiableSet(biomes);

        private Type(String name, Type... subTypes)
        {
            this.name = name;
            this.subTypes = ImmutableList.copyOf(subTypes);

            byName.put(name, this);
        }

        /**
         * Gets the name for this type.
         */
        public String getName()
        {
            return name;
        }

        public String toString()
        {
            return name;
        }

        /**
         * Retrieves a Type instance by name,
         * if one does not exist already it creates one.
         * This can be used as intermediate measure for modders to
         * add their own Biome types.
         * <p>
         * There are <i>no</i> naming conventions besides:
         * <ul><li><b>Must</b> be all upper case (enforced by name.toUpper())</li>
         * <li><b>No</b> Special characters. {Unenforced, just don't be a pain, if it becomes a issue I WILL
         * make this RTE with no worry about backwards compatibility}</li></ul>
         * <p>
         * Note: For performance sake, the return value of this function SHOULD be cached.
         * Two calls with the same name SHOULD return the same value.
         *
         * @param name The name of this Type
         * @return An instance of Type for this name.
         */
        public static Type getType(String name, Type... subTypes)
        {
            name = name.toUpperCase();
            Type t = byName.get(name);
            if (t == null)
            {
                t = new Type(name, subTypes);
            }
            return t;
        }

        /**
         * @return An unmodifiable collection of all current biome types.
         */
        public static Collection<Type> getAll()
        {
            return allTypes;
        }

        @Nullable
        public static Type fromVanilla(Biome.Category category)
        {
            if (category == Biome.Category.field_9371)
                return null;
            if (category == Biome.Category.THEEND)
                return VOID;
            return getType(category.name());
        }
    }

    private static final Map<Identifier, BiomeInfo> biomeInfoMap = new HashMap<Identifier, BiomeInfo>();

    private static class BiomeInfo
    {

        private final Set<Type> types = new HashSet<Type>();
        private final Set<Type> typesUn = Collections.unmodifiableSet(this.types);

    }

    static
    {
        registerVanillaBiomes();
    }

    /**
     * Adds the given types to the biome.
     *
     */
    public static void addTypes(Biome biome, Type... types)
    {
        Preconditions.checkArgument(ForgeRegistries.BIOMES.containsValue(biome), "Cannot add types to unregistered biome %s", biome);

        Collection<Type> supertypes = listSupertypes(types);
        Collections.addAll(supertypes, types);

        for (Type type : supertypes)
        {
            type.biomes.add(biome);
        }

        BiomeInfo biomeInfo = getBiomeInfo(biome);
        Collections.addAll(biomeInfo.types, types);
        biomeInfo.types.addAll(supertypes);
    }

    /**
     * Gets the set of biomes that have the given type.
     *
     */
    @Nonnull
    public static Set<Biome> getBiomes(Type type)
    {
        return type.biomesUn;
    }

    /**
     * Gets the set of types that have been added to the given biome.
     *
     */
    @Nonnull
    public static Set<Type> getTypes(Biome biome)
    {
        ensureHasTypes(biome);
        return getBiomeInfo(biome).typesUn;
    }

    /**
     * Checks if the two given biomes have types in common.
     *
     * @return returns true if a common type is found, false otherwise
     */
    public static boolean areSimilar(Biome biomeA, Biome biomeB)
    {
        for (Type type : getTypes(biomeA))
        {
            if (getTypes(biomeB).contains(type))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the given type has been added to the given biome.
     *
     */
    public static boolean hasType(Biome biome, Type type)
    {
        return getTypes(biome).contains(type);
    }

    /**
     * Checks if any type has been added to the given biome.
     *
     */
    public static boolean hasAnyType(Biome biome)
    {
        return !getBiomeInfo(biome).types.isEmpty();
    }

    /**
     * Automatically adds appropriate types to a given biome based on certain heuristics.
     * If a biome's types are requested and no types have been added to the biome so far, the biome's types
     * will be determined and added using this method.
     *
     */
    public static void makeBestGuess(Biome biome)
    {
        Type type = Type.fromVanilla(biome.getCategory());
        if (type != null)
        {
            BiomeDictionary.addTypes(biome, type);
        }

        if (biome.getRainfall() > 0.85f)
        {
            BiomeDictionary.addTypes(biome, WET);
        }

        if (biome.getRainfall() < 0.15f)
        {
            BiomeDictionary.addTypes(biome, DRY);
        }

        if (biome.getTemperature() > 0.85f)
        {
            BiomeDictionary.addTypes(biome, HOT);
        }

        if (biome.getTemperature() < 0.15f)
        {
            BiomeDictionary.addTypes(biome, COLD);
        }

        if (biome.hasHighHumidity() && biome.getDepth() < 0.0F && (biome.getScale() <= 0.3F && biome.getScale() >= 0.0F))
        {
            BiomeDictionary.addTypes(biome, SWAMP);
        }

        if (biome.getDepth() <= -0.5F)
        {
            if (biome.getScale() == 0.0F)
            {
                BiomeDictionary.addTypes(biome, RIVER);
            }
            else
            {
                BiomeDictionary.addTypes(biome, OCEAN);
            }
        }

        if (biome.getScale() >= 0.4F && biome.getScale() < 1.5F)
        {
            BiomeDictionary.addTypes(biome, HILLS);
        }

        if (biome.getScale() >= 1.5F)
        {
            BiomeDictionary.addTypes(biome, MOUNTAIN);
        }
    }

    //Internal implementation
    private static BiomeInfo getBiomeInfo(Biome biome)
    {
        return biomeInfoMap.computeIfAbsent(biome.getRegistryName(), k -> new BiomeInfo());
    }

    /**
     * Ensure that at least one type has been added to the given biome.
     */
    static void ensureHasTypes(Biome biome)
    {
        if (!hasAnyType(biome))
        {
            makeBestGuess(biome);
            LOGGER.warn("No types have been added to Biome {}, types have been assigned on a best-effort guess: {}", biome.getRegistryName(), !getBiomeInfo(biome).types.isEmpty() ? getBiomeInfo(biome).types : "could not guess types");
        }
    }

    private static Collection<Type> listSupertypes(Type... types)
    {
        Set<Type> supertypes = new HashSet<Type>();
        Deque<Type> next = new ArrayDeque<Type>();
        Collections.addAll(next, types);

        while (!next.isEmpty())
        {
            Type type = next.remove();

            for (Type sType : Type.byName.values())
            {
                if (sType.subTypes.contains(type) && supertypes.add(sType))
                    next.add(sType);
            }
        }

        return supertypes;
    }

    private static void registerVanillaBiomes()
    {
        addTypes(Biomes.field_9423,                            OCEAN, OVERWORLD                                                   );
        addTypes(Biomes.field_9451,                           PLAINS, OVERWORLD                                                  );
        addTypes(Biomes.field_9424,                           HOT,      DRY,        SANDY, OVERWORLD                             );
        addTypes(Biomes.field_9472,                    MOUNTAIN, HILLS, OVERWORLD                                         );
        addTypes(Biomes.field_9409,                           FOREST, OVERWORLD                                                  );
        addTypes(Biomes.field_9420,                            COLD,     CONIFEROUS, FOREST, OVERWORLD                            );
        addTypes(Biomes.field_9471,                        WET,      SWAMP, OVERWORLD                                         );
        addTypes(Biomes.field_9438,                            RIVER, OVERWORLD                                                   );
        addTypes(Biomes.field_9461,                             HOT,      DRY,        NETHER                            );
        addTypes(Biomes.field_9411,                              COLD,     DRY,        END                               );
        addTypes(Biomes.field_9435,                     COLD,     OCEAN,      SNOWY, OVERWORLD                             );
        addTypes(Biomes.field_9463,                     COLD,     RIVER,      SNOWY, OVERWORLD                             );
        addTypes(Biomes.field_9452,                       COLD,     SNOWY,      WASTELAND, OVERWORLD                         );
        addTypes(Biomes.field_9444,                    COLD,     SNOWY,      MOUNTAIN, OVERWORLD                          );
        addTypes(Biomes.field_9462,                  MUSHROOM, RARE, OVERWORLD                                          );
        addTypes(Biomes.field_9407,            MUSHROOM, BEACH,      RARE, OVERWORLD                              );
        addTypes(Biomes.field_9434,                            BEACH, OVERWORLD                                                   );
        addTypes(Biomes.field_9466,                     HOT,      DRY,        SANDY,    HILLS, OVERWORLD                   );
        addTypes(Biomes.field_9459,                     FOREST,   HILLS, OVERWORLD                                         );
        addTypes(Biomes.field_9428,                      COLD,     CONIFEROUS, FOREST,   HILLS, OVERWORLD                   );
        addTypes(Biomes.field_9464,               MOUNTAIN, OVERWORLD                                                );
        addTypes(Biomes.field_9417,                           HOT,      WET,        DENSE,    JUNGLE, OVERWORLD                  );
        addTypes(Biomes.field_9432,                     HOT,      WET,        DENSE,    JUNGLE,   HILLS, OVERWORLD         );
        addTypes(Biomes.field_9474,                      HOT,      WET,        JUNGLE,   FOREST,   RARE, OVERWORLD          );
        addTypes(Biomes.field_9446,                       OCEAN, OVERWORLD                                                   );
        addTypes(Biomes.field_9419,                      BEACH, OVERWORLD                                                   );
        addTypes(Biomes.field_9478,                       COLD,     BEACH,      SNOWY, OVERWORLD                             );
        addTypes(Biomes.field_9412,                     FOREST, OVERWORLD                                                  );
        addTypes(Biomes.field_9421,               FOREST,   HILLS, OVERWORLD                                         );
        addTypes(Biomes.field_9475,                    SPOOKY,   DENSE,      FOREST, OVERWORLD                            );
        addTypes(Biomes.field_9454,                       COLD,     CONIFEROUS, FOREST,   SNOWY, OVERWORLD                   );
        addTypes(Biomes.field_9425,                 COLD,     CONIFEROUS, FOREST,   SNOWY,    HILLS, OVERWORLD         );
        addTypes(Biomes.field_9477,                    COLD,     CONIFEROUS, FOREST, OVERWORLD                            );
        addTypes(Biomes.field_9429,              COLD,     CONIFEROUS, FOREST,   HILLS, OVERWORLD                   );
        addTypes(Biomes.field_9460,         MOUNTAIN, FOREST,     SPARSE, OVERWORLD                            );
        addTypes(Biomes.field_9449,                          HOT,      SAVANNA,    PLAINS,   SPARSE, OVERWORLD                  );
        addTypes(Biomes.field_9430,                  HOT,      SAVANNA,    PLAINS,   SPARSE,   RARE, OVERWORLD, PLATEAU          );
        addTypes(Biomes.field_9415,                             MESA,     SANDY,  DRY, OVERWORLD                               );
        addTypes(Biomes.field_9410,                        MESA,     SANDY,    DRY,    SPARSE, OVERWORLD, PLATEAU        );
        addTypes(Biomes.field_9433,                  MESA,     SANDY,     DRY, OVERWORLD, PLATEAU                               );
        addTypes(Biomes.field_9457,                   END                                                     );
        addTypes(Biomes.field_9447,                   END                                                     );
        addTypes(Biomes.field_9442,                   END                                                     );
        addTypes(Biomes.field_9465,                   END                                                     );
        addTypes(Biomes.field_9408,                   OCEAN,   HOT, OVERWORLD                                            );
        addTypes(Biomes.field_9441,                   OCEAN, OVERWORLD                                                   );
        addTypes(Biomes.field_9467,                   OCEAN,   COLD, OVERWORLD                                           );
        addTypes(Biomes.field_9448,                   OCEAN,   HOT, OVERWORLD                                            );
        addTypes(Biomes.field_9439,                   OCEAN, OVERWORLD                                                   );
        addTypes(Biomes.field_9470,                   OCEAN,   COLD, OVERWORLD                                           );
        addTypes(Biomes.field_9418,                   OCEAN,   COLD, OVERWORLD                                           );
        addTypes(Biomes.field_9473,                             VOID                                                    );
        addTypes(Biomes.field_9455,                   PLAINS,   RARE, OVERWORLD                                          );
        addTypes(Biomes.field_9427,                   HOT,      DRY,        SANDY,    RARE, OVERWORLD                    );
        addTypes(Biomes.field_9476,            MOUNTAIN, SPARSE,     RARE, OVERWORLD                              );
        addTypes(Biomes.field_9414,                   FOREST,   HILLS,      RARE, OVERWORLD                              );
        addTypes(Biomes.field_9422,                    COLD,     CONIFEROUS, FOREST,   MOUNTAIN, RARE, OVERWORLD          );
        addTypes(Biomes.field_9479,                WET,      SWAMP,      HILLS,    RARE, OVERWORLD                    );
        addTypes(Biomes.field_9453,                COLD,     SNOWY,      HILLS,    RARE, OVERWORLD                    );
        addTypes(Biomes.field_9426,                   HOT,      WET,        DENSE,    JUNGLE,   MOUNTAIN, RARE, OVERWORLD, MODIFIED);
        addTypes(Biomes.field_9405,              HOT,      SPARSE,     JUNGLE,   HILLS,    RARE, OVERWORLD, MODIFIED          );
        addTypes(Biomes.field_9431,             FOREST,   DENSE,      HILLS,    RARE, OVERWORLD                    );
        addTypes(Biomes.field_9458,       FOREST,   DENSE,      MOUNTAIN, RARE, OVERWORLD                    );
        addTypes(Biomes.field_9450,            SPOOKY,   DENSE,      FOREST,   MOUNTAIN, RARE, OVERWORLD          );
        addTypes(Biomes.field_9437,               COLD,     CONIFEROUS, FOREST,   SNOWY,    MOUNTAIN, RARE, OVERWORLD);
        addTypes(Biomes.field_9416,            DENSE,    FOREST,     RARE, OVERWORLD                              );
        addTypes(Biomes.field_9404,      DENSE,    FOREST,     HILLS,    RARE, OVERWORLD                    );
        addTypes(Biomes.field_9436, MOUNTAIN, SPARSE,     RARE, OVERWORLD, MODIFIED                              );
        addTypes(Biomes.field_9456,                  HOT,      DRY,        SPARSE,   SAVANNA,  MOUNTAIN, RARE, OVERWORLD);
        addTypes(Biomes.field_9445,             HOT,      DRY,        SPARSE,   SAVANNA,  HILLS,    RARE, OVERWORLD, PLATEAU);
        addTypes(Biomes.field_9443,                     HOT,      DRY,        SPARSE,  MOUNTAIN, RARE, OVERWORLD);
        addTypes(Biomes.field_9413,                HOT,      DRY,        SPARSE,   HILLS,    RARE, OVERWORLD, PLATEAU, MODIFIED          );
        addTypes(Biomes.field_9406,          HOT,      DRY,        SPARSE,  MOUNTAIN, RARE, OVERWORLD, PLATEAU, MODIFIED);


        if (DEBUG)
        {
            StringBuilder buf = new StringBuilder();
            buf.append("BiomeDictionary:\n");
            Type.byName.forEach((name, type) -> buf.append("    ").append(type.name).append(": ").append(type.biomes.stream().map(b -> b.getRegistryName().toString()).collect(Collectors.joining(", "))).append('\n'));
            LOGGER.debug(buf.toString());
        }
    }
}
