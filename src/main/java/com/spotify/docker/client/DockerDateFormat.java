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

import com.fasterxml.jackson.databind.util.StdDateFormat;

import java.text.ParseException;
import java.util.Date;

/**
 * Docker returns timestamps with nanosecond precision (e.g.
 * <tt>2014-10-17T21:22:56.949763914Z</tt>), but {@link Date} only supports milliseconds.
 * Creating a Date from the nanosecond timestamp results in the date being set to several
 * days after what date should be. This class converts the timestamp from nanoseconds to
 * milliseconds by removing the last six digits of the timestamp, so we can generate a Date
 * with the correct value (albeit with less precision).
 *
 * Note: a more complete solution would be to introduce a custom date type which can store the
 * nanosecond value in an additional field, so users can access the complete value. Or just use
 * Java 8 which has date objects with nanosecond support.
 */
public class DockerDateFormat extends StdDateFormat {

  private static final long serialVersionUID = 249048552876483658L;

  @Override
  public Date parse(String source) throws ParseException {
    // If the date has nanosecond precision (e.g. 2014-10-17T21:22:56.949763914Z), remove the last
    // six digits so we can create a Date object, which only support milliseconds.
    if (source.matches(".+\\.\\d{9}Z$")) {
      source = source.replaceAll("\\d{6}Z$", "Z");
    }

    return super.parse(source);
  }

  @Override
  @SuppressWarnings("CloneDoesntCallSuperClone")
  public DockerDateFormat clone() {
    // Normally clone should call super.clone(), but that works only if StdDateFormat calls
    // super.clone(), which it does not. We must create a new instance and disable the warning.
    return new DockerDateFormat();
  }
}
