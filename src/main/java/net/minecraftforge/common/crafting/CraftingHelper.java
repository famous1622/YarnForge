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

package net.minecraftforge.common.crafting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.Identifier;

public class CraftingHelper {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LogManager.getLogger();
	@SuppressWarnings("unused")
	private static final Marker CRAFTHELPER = MarkerManager.getMarker("CRAFTHELPER");
	private static final Map<Identifier, IConditionSerializer<?>> conditions = new HashMap<>();
	private static final BiMap<Identifier, IIngredientSerializer<?>> ingredients = HashBiMap.create();
	private static Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	public static IConditionSerializer<?> register(IConditionSerializer<?> serializer) {
		Identifier key = serializer.getID();
		if (conditions.containsKey(key)) {
			throw new IllegalStateException("Duplicate recipe condition serializer: " + key);
		}
		conditions.put(key, serializer);
		return serializer;
	}

	public static <T extends Ingredient> IIngredientSerializer<T> register(Identifier key, IIngredientSerializer<T> serializer) {
		if (ingredients.containsKey(key)) {
			throw new IllegalStateException("Duplicate recipe ingredient serializer: " + key);
		}
		if (ingredients.containsValue(serializer)) {
			throw new IllegalStateException("Duplicate recipe ingredient serializer: " + key + " " + serializer);
		}
		ingredients.put(key, serializer);
		return serializer;
	}

	@Nullable
	public static Identifier getID(IIngredientSerializer<?> serializer) {
		return ingredients.inverse().get(serializer);
	}

	public static <T extends Ingredient> void write(PacketByteBuf buffer, T ingredient) {
		@SuppressWarnings("unchecked") //I wonder if there is a better way generic wise...
				IIngredientSerializer<T> serializer = (IIngredientSerializer<T>) ingredient.getSerializer();
		Identifier key = ingredients.inverse().get(serializer);
		if (key == null) {
			throw new IllegalArgumentException("Tried to serialize unregistered Ingredient: " + ingredient + " " + serializer);
		}
		if (serializer != VanillaIngredientSerializer.INSTANCE) {
			buffer.writeVarInt(-1); //Marker to know there is a custom ingredient
			buffer.writeIdentifier(key);
		}
		serializer.write(buffer, ingredient);
	}

	public static Ingredient getIngredient(Identifier type, PacketByteBuf buffer) {
		IIngredientSerializer<?> serializer = ingredients.get(type);
		if (serializer == null) {
			throw new IllegalArgumentException("Can not deserialize unknown Ingredient type: " + type);
		}
		return serializer.parse(buffer);
	}

	public static Ingredient getIngredient(JsonElement json) {
		if (json == null || json.isJsonNull()) {
			throw new JsonSyntaxException("Json cannot be null");
		}

		if (json.isJsonArray()) {
			List<Ingredient> ingredients = Lists.newArrayList();
			List<Ingredient> vanilla = Lists.newArrayList();
			json.getAsJsonArray().forEach((ele) ->
			{
				Ingredient ing = CraftingHelper.getIngredient(ele);

				if (ing.getClass() == Ingredient.class) //Vanilla, Due to how we read it splits each itemstack, so we pull out to re-merge later
				{
					vanilla.add(ing);
				} else {
					ingredients.add(ing);
				}
			});

			if (!vanilla.isEmpty()) {
				ingredients.add(Ingredient.merge(vanilla));
			}

			if (ingredients.size() == 0) {
				throw new JsonSyntaxException("Item array cannot be empty, at least one item must be defined");
			}

			if (ingredients.size() == 1) {
				return ingredients.get(0);
			}

			return new CompoundIngredient(ingredients);
		}

		if (!json.isJsonObject()) {
			throw new JsonSyntaxException("Expcted ingredient to be a object or array of objects");
		}

		JsonObject obj = (JsonObject) json;

		String type = JsonHelper.getString(obj, "type", "minecraft:item");
		if (type.isEmpty()) {
			throw new JsonSyntaxException("Ingredient type can not be an empty string");
		}

		IIngredientSerializer<?> serializer = ingredients.get(new Identifier(type));
		if (serializer == null) {
			throw new JsonSyntaxException("Unknown ingredient type: " + type);
		}

		return serializer.parse(obj);
	}

	public static ItemStack getItemStack(JsonObject json, boolean readNBT) {
		String itemName = JsonHelper.getString(json, "item");

		Item item = ForgeRegistries.ITEMS.getValue(new Identifier(itemName));

		if (item == null) {
			throw new JsonSyntaxException("Unknown item '" + itemName + "'");
		}

		if (readNBT && json.has("nbt")) {
			// Lets hope this works? Needs test
			try {
				JsonElement element = json.get("nbt");
				CompoundTag nbt;
				if (element.isJsonObject()) {
					nbt = StringNbtReader.parse(GSON.toJson(element));
				} else {
					nbt = StringNbtReader.parse(JsonHelper.asString(element, "nbt"));
				}

				CompoundTag tmp = new CompoundTag();
				if (nbt.containsKey("ForgeCaps")) {
					tmp.put("ForgeCaps", nbt.getTag("ForgeCaps"));
					nbt.remove("ForgeCaps");
				}

				tmp.put("tag", nbt);
				tmp.putString("id", itemName);
				tmp.putInt("Count", JsonHelper.getInt(json, "count", 1));

				return ItemStack.fromTag(tmp);
			} catch (CommandSyntaxException e) {
				throw new JsonSyntaxException("Invalid NBT Entry: " + e.toString());
			}
		}

		return new ItemStack(item, JsonHelper.getInt(json, "count", 1));
	}

	public static boolean processConditions(JsonObject json, String memberName) {
		return !json.has(memberName) || processConditions(JsonHelper.getArray(json, memberName));
	}

	public static boolean processConditions(JsonArray conditions) {
		for (int x = 0; x < conditions.size(); x++) {
			if (!conditions.get(x).isJsonObject()) {
				throw new JsonSyntaxException("Conditions must be an array of JsonObjects");
			}

			JsonObject json = conditions.get(x).getAsJsonObject();
			if (!CraftingHelper.getCondition(json).test()) {
				return false;
			}
		}
		return true;
	}

	public static ICondition getCondition(JsonObject json) {
		Identifier type = new Identifier(JsonHelper.getString(json, "type"));
		IConditionSerializer<?> serializer = conditions.get(type);
		if (serializer == null) {
			throw new JsonSyntaxException("Unknown condition type: " + type.toString());
		}
		return serializer.read(json);
	}

	public static <T extends ICondition> JsonObject serialize(T condition) {
		@SuppressWarnings("unchecked")
		IConditionSerializer<T> serializer = (IConditionSerializer<T>) conditions.get(condition.getID());
		if (serializer == null) {
			throw new JsonSyntaxException("Unknown condition type: " + condition.getID().toString());
		}
		return serializer.getJson(condition);
	}
}
