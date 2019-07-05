/*
 * Copyright (C) 2019 Ketan Padegaonkar <ketanpadegaonkar@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.zeroturnaround.exec.stream;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Splits an OutputStream into two. Named after the unix 'tee'
 * command. It allows a stream to be branched off so there
 * are now two streams.
 */
public class TeeOutputStream extends OutputStream {
    private final OutputStream left;
    private final OutputStream right;

    public TeeOutputStream(OutputStream left, OutputStream right) {
        this.left = left;
        this.right = right;
    }

    /**
     * Write a byte array to both output streams.
     *
     * @param b   the data.
     * @param off the start offset in the data.
     * @param len the number of bytes to write.
     * @throws IOException on error.
     */
    public void write(byte[] b, int off, int len) throws IOException {
        left.write(b, off, len);
        right.write(b, off, len);
    }


    /**
     * Write a byte to both output streams.
     *
     * @param b the byte to write.
     * @throws IOException on error.
     */
    public void write(int b) throws IOException {
        left.write(b);
        right.write(b);
    }


    /**
     * Write a byte array to both output streams.
     *
     * @param b an array of bytes.
     * @throws IOException on error.
     */
    public void write(byte[] b) throws IOException {
        left.write(b);
        right.write(b);
    }

    /**
     * Closes both output streams
     *
     * @throws IOException on error.
     */
    @Override
    public void close() throws IOException {
        try {
            left.close();
        } finally {
            right.close();
        }
    }


    /**
     * Flush both output streams.
     *
     * @throws IOException on error
     */
    @Override
    public void flush() throws IOException {
        left.flush();
        right.flush();
    }
}
