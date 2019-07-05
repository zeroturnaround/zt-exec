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

import org.junit.Assert;
import org.junit.Test;
import org.zeroturnaround.exec.test.RememberCloseOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class TeeOutputStreamTest {

    public static class ExceptionOnCloseByteArrayOutputStream extends RememberCloseOutputStream {

        public ExceptionOnCloseByteArrayOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            super.close();
            throw new IOException();
        }
    }

    @Test
    public void shouldCopyContentsToBothStreams() throws IOException {
        ByteArrayOutputStream left = new ByteArrayOutputStream();
        ByteArrayOutputStream right = new ByteArrayOutputStream();
        TeeOutputStream teeOutputStream = new TeeOutputStream(left, right);

        teeOutputStream.write(10);
        teeOutputStream.write(new byte[]{1, 2, 3});
        teeOutputStream.write(new byte[]{10, 11, 12, 13, 14, 15, 15, 16}, 2, 3);

        Assert.assertArrayEquals(new byte[]{10, 1, 2, 3, 12, 13, 14}, left.toByteArray());
        Assert.assertArrayEquals(new byte[]{10, 1, 2, 3, 12, 13, 14}, right.toByteArray());
    }

    @Test
    public void shouldCloseBothStreamsWhenClosingTee() throws IOException {
        RememberCloseOutputStream left = new RememberCloseOutputStream(NullOutputStream.NULL_OUTPUT_STREAM);
        RememberCloseOutputStream right = new RememberCloseOutputStream(NullOutputStream.NULL_OUTPUT_STREAM);
        TeeOutputStream teeOutputStream = new TeeOutputStream(left, right);

        teeOutputStream.close();

        Assert.assertTrue(left.isClosed());
        Assert.assertTrue(right.isClosed());
    }

    @Test
    public void shouldCloseSecondStreamWhenClosingFirstFails() {
        ExceptionOnCloseByteArrayOutputStream left = new ExceptionOnCloseByteArrayOutputStream(NullOutputStream.NULL_OUTPUT_STREAM);
        RememberCloseOutputStream right = new RememberCloseOutputStream(NullOutputStream.NULL_OUTPUT_STREAM);
        TeeOutputStream teeOutputStream = new TeeOutputStream(left, right);
        try {
            teeOutputStream.close();
            Assert.fail("Was expecting an exception!");
        } catch (IOException expected) {

        }

        Assert.assertTrue(left.isClosed());
        Assert.assertTrue(right.isClosed());
    }
}
