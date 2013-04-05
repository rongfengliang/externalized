package com.github.cb372.util.stream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/**
 * A StreamGobbler consumes an input stream, notifying listeners on
 * every character and every line read.
 *
 * It is essential to consume the STDOUT and STDERR of a process,
 * as some processes hang when their output buffers fill up.
 *
 * Author: chris
 * Created: 4/5/13
 */
public class StreamGobbler {

    private final InputStream stream;
    private final Charset charset;
    private final List<StreamListener> listeners;

    public StreamGobbler(InputStream stream, Charset charset, List<StreamListener> listeners) {
        this.stream = stream;
        this.charset = charset;
        this.listeners = listeners;
    }

    public StreamGobbler(InputStream stream, Charset charset, StreamListener... listeners) {
        this(stream, charset, Arrays.asList(listeners));
    }

    public void gobble() throws IOException {
        int c;
        StringBuilder line = new StringBuilder();
        CharType lastSeen = CharType.Nil;

        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, charset));
        try {
            while ((c = reader.read()) != -1) {
                char ch = (char) c;
                if (ch == '\r') {
                    switch (lastSeen) {
                        case CR:
                        case LF:
                            // two \r in a row = an empty line
                            // \n followed by \r = an empty line
                            onLine(line.toString());
                            line.setLength(0);
                            break;
                    }
                    lastSeen = CharType.CR;
                } else if (ch == '\n') {
                    switch (lastSeen) {
                        case LF:
                            // two \n in a row = an empty line
                            onLine(line.toString());
                            line.setLength(0);
                            break;
                    }
                    lastSeen = CharType.LF;
                } else {
                    switch (lastSeen) {
                        case CR:
                        case LF:
                            // start of new line
                            onLine(line.toString());
                            line.setLength(0);
                            break;
                    }
                    line.append(ch);
                    lastSeen = CharType.Normal;
                }
                onChar(ch);
            }
            // drain the last line
            if (lastSeen != CharType.Nil) {
                onLine(line.toString());
            }
        } finally {
            reader.close();
        }
    }

    private enum CharType { Nil, Normal, CR, LF }

    private void onLine(String line) {
        for (StreamListener listener : listeners) {
            listener.onLine(line);
        }
    }

    private void onChar(char c) {
        for (StreamListener listener : listeners) {
            listener.onChar(c);
        }
    }
}
