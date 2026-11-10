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
    exports io.github.tdteixeira.hl7handler.writer;
}
