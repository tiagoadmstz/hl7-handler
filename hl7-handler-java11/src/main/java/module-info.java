module io.github.tdteixeira.hl7handler {
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;

    exports io.github.tdteixeira.hl7handler;
    exports io.github.tdteixeira.hl7handler.api;
    exports io.github.tdteixeira.hl7handler.binding;
    exports io.github.tdteixeira.hl7handler.mapping;
    exports io.github.tdteixeira.hl7handler.model;
    exports io.github.tdteixeira.hl7handler.network;
    exports io.github.tdteixeira.hl7handler.parser;
    exports io.github.tdteixeira.hl7handler.writer;
}
