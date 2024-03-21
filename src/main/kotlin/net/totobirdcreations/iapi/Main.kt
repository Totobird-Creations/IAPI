package net.totobirdcreations.iapi

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder
import dev.isxander.yacl3.platform.YACLPlatform
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.util.Identifier
import net.totobirdcreations.iapi.config.Config
import org.slf4j.Logger
import org.slf4j.LoggerFactory


internal object Main : ClientModInitializer {
	val ID     : String = "iapi";
    val LOGGER : Logger = LoggerFactory.getLogger(ID);

	private val CONFIG_ID : Identifier = Identifier(ID, "config");
	val CONFIG_HANDLER : ConfigClassHandler<Config> = ConfigClassHandler.createBuilder(Config::class.java)
		.id(CONFIG_ID)
		.serializer{config -> GsonConfigSerializerBuilder.create(config)
			.setPath(YACLPlatform.getConfigDir().resolve("${Main.ID}.json5"))
			.setJson5(true)
			.build()
		}
		.build();
	@JvmStatic
	val CONFIG : Config get() = CONFIG_HANDLER.instance();


	override fun onInitializeClient() {

		CONFIG_HANDLER.load();

		ClientPlayConnectionEvents.JOIN       .register{ _, _, _ -> Server.open  (); };
		ClientPlayConnectionEvents.DISCONNECT .register{ _, _    -> Server.close (); };

	}

}