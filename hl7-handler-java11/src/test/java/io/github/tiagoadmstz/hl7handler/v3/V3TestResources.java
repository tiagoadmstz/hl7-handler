package io.github.tiagoadmstz.hl7handler.v3;

import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

final class V3TestResources {

    private V3TestResources() {}

    static byte[] bytes(String resourcePath) {
        try (InputStream in = V3TestResources.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalArgumentException("Test resource not found: " + resourcePath);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static URL url(String resourcePath) {
        URL url = V3TestResources.class.getResource(resourcePath);
        if (url == null) {
            throw new IllegalArgumentException("Test resource not found: " + resourcePath);
        }
        return url;
    }

    static Path path(String resourcePath) {
        try {
            return Paths.get(url(resourcePath).toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Test resource has an invalid URI: " + resourcePath, e);
        }
    }

    static String read(String resourcePath) {
        return new String(bytes(resourcePath), StandardCharsets.UTF_8);
    }

    static Element root(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml))).getDocumentElement();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalStateException("Test XML is not well-formed", e);
        }
    }
}
