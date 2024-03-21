package net.totobirdcreations.iapi.config

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import net.minecraft.client.gui.screen.Screen
import net.totobirdcreations.iapi.Main


internal object ConfigScreen : ModMenuApi {

    override fun getModConfigScreenFactory() : ConfigScreenFactory<*>? {
        return ConfigScreenFactory<Screen>{ parent -> createConfigScreen(parent) }
    }

    private fun createConfigScreen(parent: Screen?): Screen? {
        return Main.CONFIG_HANDLER.generateGui().generateScreen(parent);
    }

}