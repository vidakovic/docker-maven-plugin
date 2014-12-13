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

import com.fasterxml.jackson.annotation.JsonProperty;

public class RemovedImage {

  private final String imageId;
  private final Type type;

  public RemovedImage(@JsonProperty("Untagged") final String untagged,
                      @JsonProperty("Deleted") final String deleted) {
    if (untagged != null) {
      this.type = Type.UNTAGGED;
      this.imageId = untagged;
    } else if (deleted != null) {
      this.type = Type.DELETED;
      this.imageId = deleted;
    } else {
      this.type = Type.UNKNOWN;
      this.imageId = null;
    }
  }

  public RemovedImage(Type type, String imageId) {
    this.type = type;
    this.imageId = imageId;
  }

  public String imageId() {
    return imageId;
  }

  public Type type() {
    return type;
  }

  public static enum Type {
    UNTAGGED,
    DELETED,
    UNKNOWN
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    RemovedImage that = (RemovedImage) o;

    if (imageId != null ? !imageId.equals(that.imageId) : that.imageId != null) {
      return false;
    }
    if (type != that.type) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = imageId != null ? imageId.hashCode() : 0;
    result = 31 * result + (type != null ? type.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("type", type)
        .add("imageId", imageId)
        .toString();
  }
}

