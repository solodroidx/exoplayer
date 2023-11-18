/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.solodroid.android.exoplayer2.util;

import com.solodroid.android.exoplayer2.C;

/**
 * Holder for FLAC stream info.
 */
public final class FlacStreamInfo {

  public final int minBlockSize;
  public final int maxBlockSize;
  public final int minFrameSize;
  public final int maxFrameSize;
  public final int sampleRate;
  public final int channels;
  public final int bitsPerSample;
  public final long totalSamples;

  /**
   * Constructs a FlacStreamInfo parsing the given binary FLAC stream info metadata structure.
   *
   * @param data An array holding FLAC stream info metadata structure
   * @param offset Offset of the structure in the array
   * @see <a href="https://xiph.org/flac/format.html#metadata_block_streaminfo">FLAC format
   *     METADATA_BLOCK_STREAMINFO</a>
   */
  public FlacStreamInfo(byte[] data, int offset) {
    ParsableBitArray scratch = new ParsableBitArray(data);
    scratch.setPosition(offset * 8);
    this.minBlockSize = scratch.readBits(16);
    this.maxBlockSize = scratch.readBits(16);
    this.minFrameSize = scratch.readBits(24);
    this.maxFrameSize = scratch.readBits(24);
    this.sampleRate = scratch.readBits(20);
    this.channels = scratch.readBits(3) + 1;
    this.bitsPerSample = scratch.readBits(5) + 1;
    this.totalSamples = ((scratch.readBits(4) & 0xFL) << 32)
        | (scratch.readBits(32) & 0xFFFFFFFFL);
    // Remaining 16 bytes is md5 value
  }

  /**
   * Constructs a FlacStreamInfo given the parameters.
   *
   * @param minBlockSize Minimum block size of the FLAC stream.
   * @param maxBlockSize Maximum block size of the FLAC stream.
   * @param minFrameSize Minimum frame size of the FLAC stream.
   * @param maxFrameSize Maximum frame size of the FLAC stream.
   * @param sampleRate Sample rate of the FLAC stream.
   * @param channels Number of channels of the FLAC stream.
   * @param bitsPerSample Number of bits per sample of the FLAC stream.
   * @param totalSamples Total samples of the FLAC stream.
   * @see <a href="https://xiph.org/flac/format.html#metadata_block_streaminfo">FLAC format
   *     METADATA_BLOCK_STREAMINFO</a>
   */
  public FlacStreamInfo(
      int minBlockSize,
      int maxBlockSize,
      int minFrameSize,
      int maxFrameSize,
      int sampleRate,
      int channels,
      int bitsPerSample,
      long totalSamples) {
    this.minBlockSize = minBlockSize;
    this.maxBlockSize = maxBlockSize;
    this.minFrameSize = minFrameSize;
    this.maxFrameSize = maxFrameSize;
    this.sampleRate = sampleRate;
    this.channels = channels;
    this.bitsPerSample = bitsPerSample;
    this.totalSamples = totalSamples;
  }

  /** Returns the maximum size for a decoded frame from the FLAC stream. */
  public int maxDecodedFrameSize() {
    return maxBlockSize * channels * (bitsPerSample / 8);
  }

  /** Returns the bit-rate of the FLAC stream. */
  public int bitRate() {
    return bitsPerSample * sampleRate;
  }

  /** Returns the duration of the FLAC stream in microseconds. */
  public long durationUs() {
    return (totalSamples * 1000000L) / sampleRate;
  }

  /**
   * Returns the sample index for the sample at given position.
   *
   * @param timeUs Time position in microseconds in the FLAC stream.
   * @return The sample index for the sample at given position.
   */
  public long getSampleIndex(long timeUs) {
    long sampleIndex = (timeUs * sampleRate) / C.MICROS_PER_SECOND;
    return Util.constrainValue(sampleIndex, 0, totalSamples - 1);
  }

  /** Returns the approximate number of bytes per frame for the current FLAC stream. */
  public long getApproxBytesPerFrame() {
    long approxBytesPerFrame;
    if (maxFrameSize > 0) {
      approxBytesPerFrame = ((long) maxFrameSize + minFrameSize) / 2 + 1;
    } else {
      // Uses the stream's block-size if it's a known fixed block-size stream, otherwise uses the
      // default value for FLAC block-size, which is 4096.
      long blockSize = (minBlockSize == maxBlockSize && minBlockSize > 0) ? minBlockSize : 4096;
      approxBytesPerFrame = (blockSize * channels * bitsPerSample) / 8 + 64;
    }
    return approxBytesPerFrame;
  }
}
