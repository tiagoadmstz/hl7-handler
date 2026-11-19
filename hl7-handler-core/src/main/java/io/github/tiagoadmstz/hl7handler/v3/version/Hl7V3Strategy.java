package io.github.tiagoadmstz.hl7handler.v3.version;

import io.github.tiagoadmstz.hl7handler.v3.Hl7V3Document;
import io.github.tiagoadmstz.hl7handler.v3.Hl7V3Format;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Map;

/**
 * Strategy for one family of HL7 v3 documents. The registry asks each strategy whether it supports a root element;
 * the first match decides the format, the version identifier, the semantic coordinates and the warnings.
 */
public interface Hl7V3Strategy {

    /**
     * Returns the document family handled by this strategy.
     *
     * @return the format
     */
    Hl7V3Format format();

    /**
     * Returns whether this strategy handles the document whose root element is given.
     *
     * @param root the root element of the document
     * @return true if this strategy supports the root element
     */
    boolean supports(Element root);

    /**
     * Returns the version identifier of the document, such as {@code CDA-R2}.
     *
     * @param document the document, already built, to read values from
     * @return the version identifier
     */
    String versionIdentifier(Hl7V3Document document);

    /**
     * Returns the semantic coordinates of this strategy: a name, used as {@code #name} in mappings, mapped to an
     * XPath relative to the root element.
     *
     * @return an unmodifiable map from coordinate name to XPath
     */
    Map<String, String> semanticCoordinates();

    /**
     * Checks the document for conditions that do not block the parse but deserve attention.
     *
     * @param document the document to check
     * @return an unmodifiable list of warnings, empty when there are none
     */
    List<String> validate(Hl7V3Document document);
}
