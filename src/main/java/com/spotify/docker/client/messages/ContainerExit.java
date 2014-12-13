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

package com.spotify.docker.client.messages;

import com.google.common.base.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
public class ContainerExit {

  @JsonProperty("StatusCode") private Integer statusCode;

  public ContainerExit() {
  }

  public ContainerExit(final Integer statusCode) {
    this.statusCode = statusCode;
  }

  public Integer statusCode() {
    return statusCode;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final ContainerExit that = (ContainerExit) o;

    if (statusCode != null ? !statusCode.equals(that.statusCode) : that.statusCode != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return statusCode != null ? statusCode.hashCode() : 0;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("statusCode", statusCode)
        .toString();
  }
}
