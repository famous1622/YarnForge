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

package net.minecraftforge.client.model;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import com.google.common.collect.ImmutableList;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import net.minecraftforge.common.model.TRSRTransformation;

import net.minecraft.client.options.GameOptions;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.math.Direction;
import net.minecraft.util.Identifier;

public abstract class SimpleModelFontRenderer extends TextRenderer {

	private final TRSRTransformation transform;
	private final VertexFormat format;
	private final Vector3f normal = new Vector3f(0, 0, 1);
	private final Direction orientation;
	private final Vector4f vec = new Vector4f();
	private float r, g, b, a;
	private ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();
	private boolean fillBlanks = false;
	private Sprite sprite;

	public SimpleModelFontRenderer(GameOptions settings, Identifier font, TextureManager manager, boolean isUnicode, Matrix4f matrix, VertexFormat format) {
		super(manager, null);
		this.transform = new TRSRTransformation(matrix);
		this.format = format;
		transform.transformNormal(normal);
		orientation = Direction.getFacing(normal.x, normal.y, normal.z);
	}

	public void setSprite(Sprite sprite) {
		this.sprite = sprite;
	}

	public void setFillBlanks(boolean fillBlanks) {
		this.fillBlanks = fillBlanks;
	}

	private void addVertex(UnpackedBakedQuad.Builder quadBuilder, float x, float y, float u, float v) {
		for (int e = 0; e < format.getElementCount(); e++) {
			switch (format.getElement(e).getType()) {
			case POSITION:
				vec.set(x, y, 0f, 1f);
				transform.transformPosition(vec);
				quadBuilder.put(e, vec.x, vec.y, vec.z, vec.w);
				break;
			case COLOR:
				quadBuilder.put(e, r, g, b, a);
				break;
			case NORMAL:
				//quadBuilder.put(e, normal.x, normal.y, normal.z, 1);
				quadBuilder.put(e, 0, 0, 1, 1);
				break;
			case UV:
				if (format.getElement(e).getIndex() == 0) {
					quadBuilder.put(e, sprite.getU(u * 16), sprite.getV(v * 16), 0, 1);
					break;
				}
				// else fallthrough to default
			default:
				quadBuilder.put(e);
				break;
			}
		}
	}

	public ImmutableList<BakedQuad> build() {
		ImmutableList<BakedQuad> ret = builder.build();
		builder = ImmutableList.builder();
		return ret;
	}
}
