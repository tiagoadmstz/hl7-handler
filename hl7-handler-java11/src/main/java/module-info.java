module io.github.tiagoadmstz.hl7handler {
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;

    exports io.github.tiagoadmstz.hl7handler;
    exports io.github.tiagoadmstz.hl7handler.api;
    exports io.github.tiagoadmstz.hl7handler.binding;
    exports io.github.tiagoadmstz.hl7handler.mapping;
    exports io.github.tiagoadmstz.hl7handler.model;
    exports io.github.tiagoadmstz.hl7handler.network;
    exports io.github.tiagoadmstz.hl7handler.parser;
    exports io.github.tiagoadmstz.hl7handler.version;
    exports io.github.tiagoadmstz.hl7handler.version.v21;
    exports io.github.tiagoadmstz.hl7handler.version.v22;
    exports io.github.tiagoadmstz.hl7handler.version.v23;
    exports io.github.tiagoadmstz.hl7handler.version.v24;
    exports io.github.tiagoadmstz.hl7handler.version.v25;
    exports io.github.tiagoadmstz.hl7handler.version.v26;
    exports io.github.tiagoadmstz.hl7handler.version.v27;
    exports io.github.tiagoadmstz.hl7handler.version.v28;
    exports io.github.tiagoadmstz.hl7handler.writer;
}
