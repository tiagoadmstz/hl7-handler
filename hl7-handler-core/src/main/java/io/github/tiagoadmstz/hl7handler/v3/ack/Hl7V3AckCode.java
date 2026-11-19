package io.github.tiagoadmstz.hl7handler.v3.ack;

/**
 * Acknowledgement codes of an HL7 v3 MCCI_IN000002UV01 message. The first letter is the level, application (A) or
 * communication (C), and the second is accept (A), error (E) or reject (R).
 */
public enum Hl7V3AckCode {

    /** Application accept. */
    AA,

    /** Application error. */
    AE,

    /** Application reject. */
    AR,

    /** Communication accept. */
    CA,

    /** Communication error. */
    CE,

    /** Communication reject. */
    CR
}
