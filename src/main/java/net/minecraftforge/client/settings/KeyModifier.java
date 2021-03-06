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

package net.minecraftforge.client.settings;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.InputUtil;

public enum KeyModifier {
	CONTROL {
		@Override
		public boolean matches(InputUtil.KeyCode key) {
			int keyCode = key.getKeyCode();
			if (MinecraftClient.IS_SYSTEM_MAC) {
				return keyCode == GLFW.GLFW_KEY_LEFT_ALT || keyCode == GLFW.GLFW_KEY_RIGHT_ALT;
			} else {
				return keyCode == GLFW.GLFW_KEY_LEFT_CONTROL || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL;
			}
		}

		@Override
		public boolean isActive(@Nullable IKeyConflictContext conflictContext) {
			return Screen.hasControlDown();
		}

		@Override
		public String getLocalizedComboName(InputUtil.KeyCode key, Supplier<String> defaultLogic) {
			String keyName = defaultLogic.get();
			String localizationFormatKey = MinecraftClient.IS_SYSTEM_MAC ? "forge.controlsgui.control.mac" : "forge.controlsgui.control";
			return I18n.translate(localizationFormatKey, keyName);
		}
	},
	SHIFT {
		@Override
		public boolean matches(InputUtil.KeyCode key) {
			return key.getKeyCode() == GLFW.GLFW_KEY_LEFT_SHIFT || key.getKeyCode() == GLFW.GLFW_KEY_RIGHT_SHIFT;
		}

		@Override
		public boolean isActive(@Nullable IKeyConflictContext conflictContext) {
			return Screen.hasShiftDown();
		}

		@Override
		public String getLocalizedComboName(InputUtil.KeyCode key, Supplier<String> defaultLogic) {
			return I18n.translate("forge.controlsgui.shift", defaultLogic.get());
		}
	},
	ALT {
		@Override
		public boolean matches(InputUtil.KeyCode key) {
			return key.getKeyCode() == GLFW.GLFW_KEY_LEFT_ALT || key.getKeyCode() == GLFW.GLFW_KEY_RIGHT_ALT;
		}

		@Override
		public boolean isActive(@Nullable IKeyConflictContext conflictContext) {
			return Screen.hasAltDown();
		}

		@Override
		public String getLocalizedComboName(InputUtil.KeyCode keyCode, Supplier<String> defaultLogic) {
			return I18n.translate("forge.controlsgui.alt", defaultLogic.get());
		}
	},
	NONE {
		@Override
		public boolean matches(InputUtil.KeyCode key) {
			return false;
		}

		@Override
		public boolean isActive(@Nullable IKeyConflictContext conflictContext) {
			if (conflictContext != null && !conflictContext.conflicts(KeyConflictContext.IN_GAME)) {
				for (KeyModifier keyModifier : MODIFIER_VALUES) {
					if (keyModifier.isActive(conflictContext)) {
						return false;
					}
				}
			}
			return true;
		}

		@Override
		public String getLocalizedComboName(InputUtil.KeyCode key, Supplier<String> defaultLogic) {
			return defaultLogic.get();
		}
	};

	public static final KeyModifier[] MODIFIER_VALUES = {SHIFT, CONTROL, ALT};

	public static KeyModifier getActiveModifier() {
		for (KeyModifier keyModifier : MODIFIER_VALUES) {
			if (keyModifier.isActive(null)) {
				return keyModifier;
			}
		}
		return NONE;
	}

	public static boolean isKeyCodeModifier(InputUtil.KeyCode key) {
		for (KeyModifier keyModifier : MODIFIER_VALUES) {
			if (keyModifier.matches(key)) {
				return true;
			}
		}
		return false;
	}

	public static KeyModifier valueFromString(String stringValue) {
		try {
			return valueOf(stringValue);
		} catch (NullPointerException | IllegalArgumentException ignored) {
			return NONE;
		}
	}

	public abstract boolean matches(InputUtil.KeyCode key);

	public abstract boolean isActive(@Nullable IKeyConflictContext conflictContext);

	public abstract String getLocalizedComboName(InputUtil.KeyCode key, Supplier<String> defaultLogic);
}
