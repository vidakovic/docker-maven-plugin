/*
 * Copyright (c) 2014 Spotify AB.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.spotify.docker.client;

import com.google.common.io.ByteStreams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static com.google.common.io.ByteStreams.copy;
import static com.google.common.io.ByteStreams.nullOutputStream;

public class LogReader implements Closeable {

  private static final Logger log = LoggerFactory.getLogger(LogReader.class);
  private final InputStream stream;
  public static final int HEADER_SIZE = 8;
  public static final int FRAME_SIZE_OFFSET = 4;

  private volatile boolean closed;

  public LogReader(final InputStream stream) {
    this.stream = stream;
  }

  public LogMessage nextMessage() throws IOException {

    // Read header
    final byte[] headerBytes = new byte[HEADER_SIZE];
    final int n = ByteStreams.read(stream, headerBytes, 0, HEADER_SIZE);
    if (n == 0) {
      return null;
    }
    if (n != HEADER_SIZE) {
      throw new EOFException();
    }
    final ByteBuffer header = ByteBuffer.wrap(headerBytes);
    final int streamId = header.get();
    header.position(FRAME_SIZE_OFFSET);
    final int frameSize = header.getInt();

    // Read frame
    final byte[] frame = new byte[frameSize];
    ByteStreams.readFully(stream, frame);
    return new LogMessage(streamId, ByteBuffer.wrap(frame));
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    if (!closed) {
      log.warn(this + " not closed properly");
      close();
    }
  }

  @Override
  public void close() throws IOException {
    closed = true;
    // Jersey will close the stream and release the connection after we read all the data.
    // We cannot call the stream's close method because it an instance of UncloseableInputStream,
    // where close is a no-op.
    copy(stream, nullOutputStream());
  }

}
