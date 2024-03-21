package net.totobirdcreations.iapi

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import net.minecraft.client.MinecraftClient
import net.minecraft.command.argument.ItemStringReader
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket
import net.minecraft.registry.Registries
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.SocketException


internal object Server {

    private var thread : Thread?       = null;
    private var server : ServerSocket? = null;

    private var occupied : Boolean = false;


    fun open() {
        val thread = Thread{ ->
            try { this.start(Main.CONFIG.serverPort); }
            catch (_ : SocketException) {}
            catch (e : Exception) {
                Main.LOGGER.error("${Main.ID} server crashed:\n${e.stackTraceToString()}");
                MinecraftClient.getInstance().player?.sendMessage(Text.translatable("${Main.ID}.crash"));
            }

        };
        this.thread = thread;
        thread.start();
    }

    fun close() {
        Main.LOGGER.info("Closing ${Main.ID} server.");
        this.thread?.interrupt();
        this.server?.close();
        this.thread   = null;
        this.server   = null;
        this.occupied = false;
    }


    private fun start(port : Int) {
        Main.LOGGER.info("Opening ${Main.ID} server on port ${port}.");
        val server = ServerSocket();
        server.bind(InetSocketAddress("127.0.0.1", port));
        this.server = server;
        while (true) {
            val client = server.accept();
            val addr   = (client.remoteSocketAddress as InetSocketAddress).address.hostAddress;
            Main.LOGGER.info("Incoming ${Main.ID} connection from ${addr}.");
            val incoming = client.getInputStream();
            val outgoing = client.getOutputStream();
            if (this.occupied) {
                this.clientRespond(outgoing, CRCode.ServiceUnavailable, "Currently processing another request.");
            } else if (MinecraftClient.getInstance().player?.isCreative != true) {
                this.clientRespond(outgoing, CRCode.ServiceUnavailable, "Player is not in creative mode.");
            } else {
                this.occupied = true;
                Thread{ ->
                    try {
                        this.handleClient(addr, incoming, outgoing);
                    } catch (e : Exception) {
                        this.clientRespond(outgoing, CRCode.InternalServerError, e.stackTraceToString());
                        this.resetOccupied();
                    }
                    incoming.close();
                    outgoing.close();
                    client.close();
                }.start();
            }
        }
    }


    private fun handleClient(addr : String, incoming : InputStream, outgoing : OutputStream) {
        val size = incoming.available();
        if (size >= Short.MAX_VALUE) {
            return this.clientRespond(outgoing, CRCode.PayloadTooLarge, "The received data is too large to process.");
        }

        val lines    = incoming.readNBytes(size).toString(Charsets.UTF_8);
        val sections = lines.split("\r\n\r\n", limit = 2);
        val header   = sections[0].split("\r\n");
        val request  = header[0].split(" ");
        val method  = request[0];
        val route   = request[1];
        val version = request[2];

        if (version != "HTTP/1.1") {
            return this.clientRespond(outgoing, CRCode.HTTPVersionNotSupported, "Requests in version ${version} are not supported. Use HTTP/1.1 instead.");
        }
        if (method != "POST") {
            return this.clientRespond(outgoing, CRCode.MethodNotAllowed, "Requests of type ${method} are not supported. Use POST instead.");
        }
        if (route != "/") {
            return this.clientRespond(outgoing, CRCode.NotFound, "Requests to ${route} are not supported. Use / instead.");
        }

        val headers  = hashMapOf<String, String>();
        for (item_ in header.slice(1..<header.size)) {
            val item = item_.split(": ", limit = 2);
            headers[item[0]] = item[1];
        }
        val body = sections[1];

        try {
            val item = ItemStringReader.item(Registries.ITEM.readOnlyWrapper, StringReader(body));
            this.awaitImportApproval(addr, item);
        } catch (e : CommandSyntaxException) {
            return this.clientRespond(outgoing, CRCode.BadRequest, "Invalid item received: ${e.message}");
        }

        return this.clientRespond(outgoing, CRCode.Accepted, "Awaiting manual approval.");
    }


    private fun clientRespond(outgoing : OutputStream, code : CRCode, body : String) {
        val msg = "Responded to ${Main.ID} connection with ${code.code} ${code.id}: ${body}";
        if (code == CRCode.Accepted) {
            Main.LOGGER.info(msg);
        } else {
            Main.LOGGER.warn(msg);
        }
        val response = "HTTP/1.1 ${code.code} ${code.id}\r\n\r\n${body}";
        outgoing.write(response.toByteArray(Charsets.UTF_8));
        outgoing.flush();
        if (code != CRCode.Accepted && code != CRCode.ServiceUnavailable) {
            this.occupied = false;
        }
    }


    private fun awaitImportApproval(addr : String, item : ItemStringReader.ItemResult) {
        val stack = item.item.value().defaultStack;
        stack.nbt = item.nbt;

        if (Main.CONFIG.autoApprove) {
            this.approveImport(stack);
        } else {
            val client = MinecraftClient.getInstance();
            client.player?.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), 1.0f, 1.5f);
            client.execute{ -> client.setScreen(ImportScreen(addr, stack)); };
        }
    }

    fun approveImport(stack : ItemStack) {
        Main.LOGGER.error("Approved ${Main.ID} import ${stack.item}");
        val client = MinecraftClient.getInstance();
        val player = client.player?: return;
        var suffix = "";
        if (! player.isCreative) {
            suffix = ".not_creative";
        } else {
            var slot = player.inventory?.emptySlot ?: return;
            if (slot == -1) {
                suffix = ".no_space";
            } else {
                if (slot < 9) {
                    slot += 36;
                }
                client.networkHandler?.sendPacket(CreativeInventoryActionC2SPacket(
                    slot, stack
                ));
            }
        }
        player.sendMessage(Text.translatable("${Main.ID}.approve${suffix}"));
        this.resetOccupied();
    }

    fun resetOccupied() {
        if (this.occupied) {
            Main.LOGGER.error("Finished handling ${Main.ID} import.");
            this.occupied = false;
        }
    }


    private enum class CRCode(val code : Int, val id : String) {
        Accepted                (202, "Accepted"                   ),
        BadRequest              (400, "Bad Request"                ),
        NotFound                (404, "Not Found"                  ),
        MethodNotAllowed        (405, "Method Not Allowed"         ),
        PayloadTooLarge         (413, "Payload Too Large"          ),
        InternalServerError     (500, "Internal Server Error"      ),
        ServiceUnavailable      (503, "Service Unavailable"        ),
        HTTPVersionNotSupported (505, "HTTP Version Not Supported" )
    }

}