package net.minecraftforge.client.model.generators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.block.Block;
import net.minecraft.state.property.Property;

public final class MultiPartBlockStateBuilder implements IGeneratedBlockstate {

	private final List<PartBuilder> parts = new ArrayList<>();
	private final Block owner;

	public MultiPartBlockStateBuilder(Block owner) {
		this.owner = owner;
	}

	/**
	 * Creates a builder for models to assign to a {@link PartBuilder}, which when
	 * completed via {@link ConfiguredModel.Builder#addModel()} will assign the
	 * resultant set of models to the part and return it for further processing.
	 *
	 * @return the model builder
	 * @see ConfiguredModel.Builder
	 */
	public ConfiguredModel.Builder<PartBuilder> part() {
		return ConfiguredModel.builder(this);
	}

	MultiPartBlockStateBuilder addPart(PartBuilder part) {
		this.parts.add(part);
		return this;
	}

	@Override
	public JsonObject toJson() {
		JsonArray variants = new JsonArray();
		for (PartBuilder part : parts) {
			variants.add(part.toJson());
		}
		JsonObject main = new JsonObject();
		main.add("multipart", variants);
		return main;
	}

	public class PartBuilder {
		public final Multimap<Property<?>, Comparable<?>> conditions = HashMultimap.create();
		public BlockStateProvider.ConfiguredModelList models;
		public boolean useOr;

		PartBuilder(BlockStateProvider.ConfiguredModelList models) {
			this.models = models;
		}

		public PartBuilder useOr() {
			this.useOr = true;
			return this;
		}

		/**
		 * Set a condition for this part, which consists of a property and a set of
		 * valid values. Can be called multiple times for multiple different properties.
		 *
		 * @param <T>    the type of the property value
		 * @param prop   the property
		 * @param values a set of valid values
		 * @return this builder
		 * @throws NullPointerException     if {@code prop} is {@code null}
		 * @throws NullPointerException     if {@code values} is {@code null}
		 * @throws IllegalArgumentException if {@code values} is empty
		 * @throws IllegalArgumentException if {@code prop} has already been configured
		 * @throws IllegalArgumentException if {@code prop} is not applicable to the
		 *                                  current block's state
		 */
		@SafeVarargs
		public final <T extends Comparable<T>> PartBuilder condition(Property<T> prop, T... values) {
			Preconditions.checkNotNull(prop, "Property must not be null");
			Preconditions.checkNotNull(values, "Value list must not be null");
			Preconditions.checkArgument(values.length > 0, "Value list must not be empty");
			Preconditions.checkArgument(!conditions.containsKey(prop), "Cannot set condition for property \"%s\" more than once", prop.getName());
			Preconditions.checkArgument(canApplyTo(owner), "IProperty %s is not valid for the block %s", prop, owner);
			this.conditions.putAll(prop, Arrays.asList(values));
			return this;
		}

		public MultiPartBlockStateBuilder end() {
			return MultiPartBlockStateBuilder.this;
		}

		JsonObject toJson() {
			JsonObject out = new JsonObject();
			if (!conditions.isEmpty()) {
				JsonObject when = new JsonObject();
				for (Entry<Property<?>, Collection<Comparable<?>>> e : conditions.asMap().entrySet()) {
					StringBuilder activeString = new StringBuilder();
					for (Object val : e.getValue()) {
						if (activeString.length() > 0) {
							activeString.append("|");
						}
						activeString.append(val.toString());
					}
					when.addProperty(e.getKey().getName(), activeString.toString());
				}
				if (useOr) {
					JsonObject innerWhen = when;
					when = new JsonObject();
					when.add("OR", innerWhen);
				}
				out.add("when", when);
			}
			out.add("apply", models.toJSON());
			return out;
		}

		public boolean canApplyTo(Block b) {
			return b.getStateFactory().getProperties().containsAll(conditions.keySet());
		}
	}
}
