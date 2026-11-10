module io.github.tdteixeira.hl7handler {
    requires tools.jackson.core;
    requires tools.jackson.databind;
    requires tools.jackson.dataformat.yaml;

    exports io.github.tdteixeira.hl7handler;
    exports io.github.tdteixeira.hl7handler.api;
    exports io.github.tdteixeira.hl7handler.binding;
    exports io.github.tdteixeira.hl7handler.mapping;
    exports io.github.tdteixeira.hl7handler.model;
    exports io.github.tdteixeira.hl7handler.network;
    exports io.github.tdteixeira.hl7handler.parser;
    exports io.github.tdteixeira.hl7handler.version;
    exports io.github.tdteixeira.hl7handler.version.v21;
    exports io.github.tdteixeira.hl7handler.version.v22;
    exports io.github.tdteixeira.hl7handler.version.v23;
    exports io.github.tdteixeira.hl7handler.version.v24;
    exports io.github.tdteixeira.hl7handler.version.v25;
    exports io.github.tdteixeira.hl7handler.version.v26;
    exports io.github.tdteixeira.hl7handler.version.v27;
    exports io.github.tdteixeira.hl7handler.version.v28;
    exports io.github.tdteixeira.hl7handler.writer;
}
