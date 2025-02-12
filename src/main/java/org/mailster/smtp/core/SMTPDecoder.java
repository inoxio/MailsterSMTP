/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.mailster.smtp.core;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;

import org.apache.mina.core.buffer.BufferDataException;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

/**
 * A {@link ProtocolDecoder} which decodes incoming SMTP data based on session context.
 *
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 */
public final class SMTPDecoder implements ProtocolDecoder {

    protected final static String TMPFILE_PREFIX = "mailsterSmtp";
    protected final static String TMPFILE_SUFFIX = ".eml";
    private final static String CONTEXT = SMTPDecoder.class.getName() + ".context";
    private final static byte[] SMTP_CMD_DELIMITER = new byte[]{'\r', '\n'};
    private final static byte[] SMTP_DATA_DELIMITER = new byte[]{'\r', '\n', '.', '\r', '\n'};

    private Charset charset;

    /**
     * <a href="http://rfc.net/rfc2822.html#s2.1.1.">RFC 2822</a>
     */
    private int maxLineLength = 998;

    /**
     * When to trigger
     */
    private int threshold;

    public SMTPDecoder(Charset charset, int thresholdBytes) {
        setup(charset, thresholdBytes);
    }

    public void setup(Charset charset, int thresholdBytes) {
        if (charset == null) {
            throw new NullPointerException("charset");
        }

        this.charset = charset;
        this.threshold = thresholdBytes;
    }

    public void setDataDeferredSize(int dataDeferredSize) {
        this.threshold = dataDeferredSize;
    }

    /**
     * Returns the allowed maximum size of the line to be decoded. If the size
     * of the line to be decoded exceeds this value, the decoder will throw a
     * {@link BufferDataException}. The default value is <i>998</i> bytes.
     */
    public int getMaxLineLength() {
        return maxLineLength;
    }

    /**
     * Sets the allowed maximum size of the line to be decoded. If the size of
     * the line to be decoded exceeds this value, the decoder will throw a
     * {@link BufferDataException}. The default value is <i>998</i> bytes.
     */
    public void setMaxLineLength(int maxLineLength) {
        if (maxLineLength <= 0) {
            throw new IllegalArgumentException("maxLineLength: " + maxLineLength);
        }

        this.maxLineLength = maxLineLength;
    }

    private SMTPDecoderContext getContext(IoSession session) {
        var ctx = (SMTPDecoderContext) session.getAttribute(CONTEXT);
        if (ctx == null) {
            ctx = new SMTPDecoderContext(this);
            session.setAttribute(CONTEXT, ctx);
        }
        return ctx;
    }

    @Override
    public void finishDecode(IoSession session, ProtocolDecoderOutput out) {
    }

    @Override
    public void dispose(IoSession session) throws Exception {
        var ctx = (SMTPDecoderContext) session.getAttribute(CONTEXT);
        if (ctx != null) {
            ctx.getBuffer().free();
            ctx.closeOutputStream();
            session.removeAttribute(CONTEXT);
        }
    }

    @Override
    public void decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
        var ctx = getContext(session);
        var matchCount = ctx.getMatchCount();

        var minaCtx = (SMTPContext) session.getAttribute(SMTPConnectionHandler.CONTEXT_ATTRIBUTE);

        var dataMode = minaCtx.getSMTPState().isDataMode();
        ctx.setDataMode(dataMode);
        var delimBuf = dataMode ? SMTP_DATA_DELIMITER : SMTP_CMD_DELIMITER;

        // Try to find a match
        var oldPos = in.position();
        var oldLimit = in.limit();

        if (matchCount == delimBuf.length) {
            matchCount = 0;
        }

        while (in.remaining() > 0) {
            var b = in.get();
            if (delimBuf[matchCount] == b) {
                matchCount++;
                if (matchCount == delimBuf.length) {
                    // Found a match.
                    var pos = in.position();
                    in.limit(pos);
                    in.position(oldPos);

                    ctx.write(in);

                    in.limit(oldLimit);
                    in.position(pos);

                    if (ctx.getOverflowPosition() == 0) {
                        var buf = ctx.getBuffer();
                        buf.flip();

                        try {
                            if (dataMode) {
                                delimBuf = SMTP_CMD_DELIMITER;
                                out.write(ctx.getNewInputStream());
                            } else {
                                buf.limit(buf.limit() - matchCount);
                                out.write(buf.getString(ctx.getDecoder()));
                            }
                        } catch (IOException ioex) {
                            throw new CharacterCodingException();
                        } finally {
                            ctx.reset();
                            buf.clear();
                        }
                    } else {
                        var msg = "Line is too long: " + ctx.getOverflowPosition();
                        ctx.reset();
                        throw new BufferDataException(msg);
                    }

                    oldPos = pos;
                    matchCount = 0;
                }
            } else {
                // fix for DIRMINA-506
                in.position(Math.max(0, in.position() - matchCount));
                matchCount = 0;
            }
        }

        // Put remainder to buf.
        in.position(oldPos);
        ctx.write(in);

        ctx.setMatchCount(matchCount);
    }

    public Charset getCharset() {
        return charset;
    }

    public int getThreshold() {
        return threshold;
    }
}
