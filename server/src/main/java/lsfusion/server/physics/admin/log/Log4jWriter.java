package lsfusion.server.physics.admin.log;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.Writer;

public class Log4jWriter extends Writer {
    
    private Logger logger;

    public Log4jWriter(Logger logger) {
        this.logger = logger;
    }

    public void write(char[] cbuf, int off, int len) {
        logger.info(new String(cbuf, off, len));
    }

    public void flush() {

    }

    public void close() {

    }
}
