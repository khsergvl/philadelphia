/*
 * Copyright 2015 Philadelphia authors
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
package com.paritytrading.philadelphia;

import static java.util.Arrays.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.Before;
import org.junit.Test;

public class FIXMessageParserTest {

    private FIXMessages messages;

    @Before
    public void setUp() {
        messages = new FIXMessages();
    }

    @Test
    public void readPartialBeginStringTag() throws IOException {
        readPartial("8");
    }

    @Test
    public void readPartialBeginStringValueMissing() throws IOException {
        readPartial("8=");
    }

    @Test
    public void readPartialBeginStringValue() throws IOException {
        readPartial("8=FIX");
    }

    @Test
    public void readGarbledBeginString() throws IOException {
        readGarbledOrPartial("9=30\u00018=FIX.4.2\u0001", 10);
    }

    @Test
    public void readPartialBodyLengthMissing() throws IOException {
        readPartial("8=FIX.4.2\u0001");
    }

    @Test
    public void readPartialBodyLengthTag() throws IOException {
        readPartial("8=FIX.4.2\u00019");
    }

    @Test
    public void readPartialBodyLengthValueMissing() throws IOException {
        readPartial("8=FIX.4.2\u00019=");
    }

    @Test
    public void readPartialBodyLengthValue() throws IOException {
        readPartial("8=FIX.4.2\u00019=1");
    }

    @Test
    public void readGarbledBodyLength() throws IOException {
        readGarbledOrPartial("8=FIX.4.2\u00018=FIX.4.2\u0001", 10);
    }

    @Test
    public void readPartialBody() throws IOException {
        readPartial("8=FIX.4.2\u00019=5\u000135=");
    }

    @Test
    public void readPartialCheckSumMissing() throws IOException {
        readPartial("8=FIX.4.2\u00019=5\u000135=D\u0001");
    }

    @Test
    public void readPartialCheckSumTag() throws IOException {
        readPartial("8=FIX.4.2\u00019=5\u000135=D\u000110");
    }

    @Test
    public void readPartialCheckSumValueMissing() throws IOException {
        readPartial("8=FIX.4.2\u00019=5\u000135=D\u000110=");
    }

    @Test
    public void readPartialCheckSumValue() throws IOException {
        readPartial("8=FIX.4.2\u00019=5\u000135=D\u000110=0");
    }

    @Test
    public void readGarbledCheckSum() throws IOException {
        readGarbledOrPartial("8=FIX.4.2\u00019=5\u000135=D\u000110=000\u0001", 0);
    }

    @Test
    public void readGarbledCheckSumCheckSumDisabled() throws IOException {
        ByteBuffer buffer = ByteBuffers.wrap("8=FIX.4.2\u00019=5\u000135=D\u000110=000\u0001");

        assertEquals(true, parse(buffer, false));

        assertEquals(0, buffer.remaining());

        assertEquals(asList("35=D\u0001"), messages.collect());
    }

    @Test
    public void read() throws IOException {
        ByteBuffer buffer = ByteBuffers.wrap("8=FIX.4.2\u00019=5\u000135=D\u000110=181\u0001");

        assertEquals(true, parse(buffer, true));

        assertEquals(0, buffer.remaining());

        assertEquals(asList("35=D\u0001"), messages.collect());
    }

    @Test
    public void readFixt11() throws IOException {
        ByteBuffer buffer = ByteBuffers.wrap("8=FIXT.1.1\u00019=5\u000135=D\u000110=005\u0001");

        assertEquals(true, parse(buffer, true));

        assertEquals(0, buffer.remaining());

        assertEquals(asList("35=D\u0001"), messages.collect());
    }

    private void readPartial(String input) throws IOException {
        readGarbledOrPartial(input, input.length());
    }

    private void readGarbledOrPartial(String input, int remaining) throws IOException {
        ByteBuffer buffer = ByteBuffers.wrap(input);

        assertEquals(false, parse(buffer, true));

        assertEquals(remaining, buffer.remaining());

        assertEquals(asList(), messages.collect());
    }

    private boolean parse(ByteBuffer buffer, boolean checkSumEnabled) throws IOException {
        FIXConfig config = new FIXConfig.Builder()
            .setMaxFieldCount(32)
            .setFieldCapacity(32)
            .setCheckSumEnabled(checkSumEnabled)
            .build();

        FIXMessageParser parser = new FIXMessageParser(config, messages);

        return parser.parse(buffer);
    }

}
