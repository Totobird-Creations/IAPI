package net.totobirdcreations.iapi.config

import dev.isxander.yacl3.config.v2.api.SerialEntry
import dev.isxander.yacl3.config.v2.api.autogen.AutoGen
import dev.isxander.yacl3.config.v2.api.autogen.Boolean as BoolAG
import dev.isxander.yacl3.config.v2.api.autogen.IntField as IntAG


class Config {

    @AutoGen(category = "main")
    @IntAG(min = 1024, max = 65535)
    @SerialEntry
    @JvmField var serverPort : Int = 25530;

    @AutoGen(category = "main")
    @BoolAG(formatter = BoolAG.Formatter.YES_NO, colored = true)
    @SerialEntry
    @JvmField var autoApprove : Boolean = false;

}