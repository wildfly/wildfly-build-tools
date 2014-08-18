package org.wildfly.build.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Eduardo Martins
 */
public interface InputStreamSource {

    InputStream getInputStream() throws IOException;

}
