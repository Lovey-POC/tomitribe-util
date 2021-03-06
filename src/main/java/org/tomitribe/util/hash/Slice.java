/*
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
package org.tomitribe.util.hash;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.tomitribe.util.hash.JvmUtils.newByteBuffer;
import static org.tomitribe.util.hash.JvmUtils.unsafe;
import static org.tomitribe.util.hash.Preconditions.checkArgument;
import static org.tomitribe.util.hash.Preconditions.checkNotNull;
import static org.tomitribe.util.hash.Preconditions.checkPositionIndexes;
import static org.tomitribe.util.hash.SizeOf.SIZE_OF_BYTE;
import static org.tomitribe.util.hash.SizeOf.SIZE_OF_DOUBLE;
import static org.tomitribe.util.hash.SizeOf.SIZE_OF_FLOAT;
import static org.tomitribe.util.hash.SizeOf.SIZE_OF_INT;
import static org.tomitribe.util.hash.SizeOf.SIZE_OF_LONG;
import static org.tomitribe.util.hash.SizeOf.SIZE_OF_SHORT;
import static org.tomitribe.util.hash.StringDecoder.decodeString;
import static sun.misc.Unsafe.ARRAY_BOOLEAN_BASE_OFFSET;
import static sun.misc.Unsafe.ARRAY_BOOLEAN_INDEX_SCALE;
import static sun.misc.Unsafe.ARRAY_BYTE_BASE_OFFSET;
import static sun.misc.Unsafe.ARRAY_DOUBLE_BASE_OFFSET;
import static sun.misc.Unsafe.ARRAY_DOUBLE_INDEX_SCALE;
import static sun.misc.Unsafe.ARRAY_FLOAT_BASE_OFFSET;
import static sun.misc.Unsafe.ARRAY_FLOAT_INDEX_SCALE;
import static sun.misc.Unsafe.ARRAY_INT_BASE_OFFSET;
import static sun.misc.Unsafe.ARRAY_INT_INDEX_SCALE;
import static sun.misc.Unsafe.ARRAY_LONG_BASE_OFFSET;
import static sun.misc.Unsafe.ARRAY_LONG_INDEX_SCALE;
import static sun.misc.Unsafe.ARRAY_SHORT_BASE_OFFSET;
import static sun.misc.Unsafe.ARRAY_SHORT_INDEX_SCALE;

public final class Slice
        implements Comparable<Slice> {
    /**
     * @deprecated use {@link Slices#wrappedBuffer(java.nio.ByteBuffer)}
     */
    @Deprecated
    public static Slice toUnsafeSlice(ByteBuffer byteBuffer) {
        return Slices.wrappedBuffer(byteBuffer);
    }

    /**
     * Base object for relative addresses.  If null, the address is an
     * absolute location in memory.
     */
    private final Object base;

    /**
     * If base is null, address is the absolute memory location of data for
     * this slice; otherwise, address is the offset from the base object.
     * This base plus relative offset addressing is taken directly from
     * the Unsafe interface.
     * <p/>
     * Note: if base object is a byte array, this address ARRAY_BYTE_BASE_OFFSET,
     * since the byte array data starts AFTER the byte array object header.
     */
    private final long address;

    /**
     * Size of the slice
     */
    private final int size;

    /**
     * Reference is typically a ByteBuffer object, but can be any object this
     * slice must hold onto to assure that the underlying memory is not
     * freed by the garbage collector.
     */
    private final Object reference;

    private int hash;

    /**
     * Creates an empty slice.
     */
    Slice() {
        this.base = null;
        this.address = 0;
        this.size = 0;
        this.reference = null;
    }

    /**
     * Creates a slice over the specified array.
     */
    Slice(byte[] base) {
        checkNotNull(base, "base is null");
        this.base = base;
        this.address = ARRAY_BYTE_BASE_OFFSET;
        this.size = base.length;
        this.reference = null;
    }

    /**
     * Creates a slice over the specified array range.
     */
    Slice(byte[] base, int offset, int length) {
        checkNotNull(base, "base is null");
        checkPositionIndexes(offset, offset + length, base.length);

        this.base = base;
        this.address = ARRAY_BYTE_BASE_OFFSET + offset;
        this.size = length;
        this.reference = null;
    }

    /**
     * Creates a slice over the specified array range.
     */
    Slice(boolean[] base, int offset, int length) {
        checkNotNull(base, "base is null");
        checkPositionIndexes(offset, offset + length, base.length);

        this.base = base;
        this.address = ARRAY_BOOLEAN_BASE_OFFSET + offset;
        this.size = length * ARRAY_BOOLEAN_INDEX_SCALE;
        this.reference = null;
    }

    /**
     * Creates a slice over the specified array range.
     */
    Slice(short[] base, int offset, int length) {
        checkNotNull(base, "base is null");
        checkPositionIndexes(offset, offset + length, base.length);

        this.base = base;
        this.address = ARRAY_SHORT_BASE_OFFSET + offset;
        this.size = length * ARRAY_SHORT_INDEX_SCALE;
        this.reference = null;
    }

    /**
     * Creates a slice over the specified array range.
     */
    Slice(int[] base, int offset, int length) {
        checkNotNull(base, "base is null");
        checkPositionIndexes(offset, offset + length, base.length);

        this.base = base;
        this.address = ARRAY_INT_BASE_OFFSET + offset;
        this.size = length * ARRAY_INT_INDEX_SCALE;
        this.reference = null;
    }

    /**
     * Creates a slice over the specified array range.
     */
    Slice(long[] base, int offset, int length) {
        checkNotNull(base, "base is null");
        checkPositionIndexes(offset, offset + length, base.length);

        this.base = base;
        this.address = ARRAY_LONG_BASE_OFFSET + offset;
        this.size = length * ARRAY_LONG_INDEX_SCALE;
        this.reference = null;
    }

    /**
     * Creates a slice over the specified array range.
     */
    Slice(float[] base, int offset, int length) {
        checkNotNull(base, "base is null");
        checkPositionIndexes(offset, offset + length, base.length);

        this.base = base;
        this.address = ARRAY_FLOAT_BASE_OFFSET + offset;
        this.size = length * ARRAY_FLOAT_INDEX_SCALE;
        this.reference = null;
    }

    /**
     * Creates a slice over the specified array range.
     */
    Slice(double[] base, int offset, int length) {
        checkNotNull(base, "base is null");
        checkPositionIndexes(offset, offset + length, base.length);

        this.base = base;
        this.address = ARRAY_DOUBLE_BASE_OFFSET + offset;
        this.size = length * ARRAY_DOUBLE_INDEX_SCALE;
        this.reference = null;
    }

    /**
     * Creates a slice for directly accessing the base object.
     */
    Slice(Object base, long address, int size, Object reference) {
        if (address <= 0) {
            throw new IllegalArgumentException(format("Invalid address: %s", address));
        }
        if (size <= 0) {
            throw new IllegalArgumentException(format("Invalid size: %s", size));
        }
        checkArgument((address + size) >= size, "Address + size is greater than 64 bits");

        this.reference = reference;
        this.base = base;
        this.address = address;
        this.size = size;
    }

    /**
     * Returns the base object of this Slice, or null.  This is appropriate for use
     * with {@link sun.misc.Unsafe} if you wish to avoid all the safety belts e.g. bounds checks.
     */
    public Object getBase() {
        return base;
    }

    /**
     * Return the address offset of this Slice.  This is appropriate for use
     * with {@link sun.misc.Unsafe} if you wish to avoid all the safety belts e.g. bounds checks.
     */
    public long getAddress() {
        return address;
    }

    /**
     * Length of this slice.
     */
    public int length() {
        return size;
    }

    /**
     * Fill the slice with the specified value;
     */
    public void fill(byte value) {
        int offset = 0;
        int length = size;
        long longValue = fillLong(value);
        while (length >= SIZE_OF_LONG) {
            unsafe.putLong(base, address + offset, longValue);
            offset += SIZE_OF_LONG;
            length -= SIZE_OF_LONG;
        }

        while (length > 0) {
            unsafe.putByte(base, address + offset, value);
            offset++;
            length--;
        }
    }

    /**
     * Fill the slice with zeros;
     */
    public void clear() {
        clear(0, size);
    }

    public void clear(int offset, int length) {
        while (length >= SIZE_OF_LONG) {
            unsafe.putLong(base, address + offset, 0);
            offset += SIZE_OF_LONG;
            length -= SIZE_OF_LONG;
        }

        while (length > 0) {
            unsafe.putByte(base, address + offset, (byte) 0);
            offset++;
            length--;
        }
    }

    /**
     * Gets a byte at the specified absolute {@code index} in this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     * {@code index + 1} is greater than {@code this.length()}
     */
    public byte getByte(int index) {
        checkIndexLength(index, SIZE_OF_BYTE);
        return unsafe.getByte(base, address + index);
    }

    /**
     * Gets an unsigned byte at the specified absolute {@code index} in this
     * buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     * {@code index + 1} is greater than {@code this.length()}
     */
    public short getUnsignedByte(int index) {
        return (short) (getByte(index) & 0xFF);
    }

    /**
     * Gets a 16-bit short integer at the specified absolute {@code index} in
     * this slice.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     * {@code index + 2} is greater than {@code this.length()}
     */
    public short getShort(int index) {
        checkIndexLength(index, SIZE_OF_SHORT);
        return unsafe.getShort(base, address + index);
    }

    /**
     * Gets a 32-bit integer at the specified absolute {@code index} in
     * this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     * {@code index + 4} is greater than {@code this.length()}
     */
    public int getInt(int index) {
        checkIndexLength(index, SIZE_OF_INT);
        return unsafe.getInt(base, address + index);
    }

    /**
     * Gets a 64-bit long integer at the specified absolute {@code index} in
     * this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     * {@code index + 8} is greater than {@code this.length()}
     */
    public long getLong(int index) {
        checkIndexLength(index, SIZE_OF_LONG);
        return unsafe.getLong(base, address + index);
    }

    /**
     * Gets a 32-bit float at the specified absolute {@code index} in
     * this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     * {@code index + 4} is greater than {@code this.length()}
     */
    public float getFloat(int index) {
        checkIndexLength(index, SIZE_OF_FLOAT);
        return unsafe.getFloat(base, address + index);
    }

    /**
     * Gets a 64-bit double at the specified absolute {@code index} in
     * this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     * {@code index + 8} is greater than {@code this.length()}
     */
    public double getDouble(int index) {
        checkIndexLength(index, SIZE_OF_DOUBLE);
        return unsafe.getDouble(base, address + index);
    }

    /**
     * Transfers portion of data from this slice into the specified destination starting at
     * the specified absolute {@code index}.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0}, or
     * if {@code index + destination.length()} is greater than {@code this.length()}
     */
    public void getBytes(int index, Slice destination) {
        getBytes(index, destination, 0, destination.length());
    }

    /**
     * Transfers portion of data from this slice into the specified destination starting at
     * the specified absolute {@code index}.
     *
     * @param destinationIndex the first index of the destination
     * @param length the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0},
     * if the specified {@code destinationIndex} is less than {@code 0},
     * if {@code index + length} is greater than
     * {@code this.length()}, or
     * if {@code destinationIndex + length} is greater than
     * {@code destination.length()}
     */
    public void getBytes(int index, Slice destination, int destinationIndex, int length) {
        destination.setBytes(destinationIndex, this, index, length);
    }

    /**
     * Transfers portion of data from this slice into the specified destination starting at
     * the specified absolute {@code index}.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0}, or
     * if {@code index + destination.length} is greater than {@code this.length()}
     */
    public void getBytes(int index, byte[] destination) {
        getBytes(index, destination, 0, destination.length);
    }

    /**
     * Transfers portion of data from this slice into the specified destination starting at
     * the specified absolute {@code index}.
     *
     * @param destinationIndex the first index of the destination
     * @param length the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0},
     * if the specified {@code destinationIndex} is less than {@code 0},
     * if {@code index + length} is greater than
     * {@code this.length()}, or
     * if {@code destinationIndex + length} is greater than
     * {@code destination.length}
     */
    public void getBytes(int index, byte[] destination, int destinationIndex, int length) {
        checkIndexLength(index, length);
        checkPositionIndexes(destinationIndex, destinationIndex + length, destination.length);

        copyMemory(base, address + index, destination, (long) ARRAY_BYTE_BASE_OFFSET + destinationIndex, length);
    }

    /**
     * Returns a copy of this buffer as a byte array.
     */
    public byte[] getBytes() {
        return getBytes(0, length());
    }

    /**
     * Returns a copy of this buffer as a byte array.
     *
     * @param index the absolute index to start at
     * @param length the number of bytes to return
     * @throws IndexOutOfBoundsException if the specified {@code index} is less then {@code 0},
     * or if the specified {@code index + length} is greater than {@code this.length()}
     */
    public byte[] getBytes(int index, int length) {
        byte[] bytes = new byte[length];
        getBytes(index, bytes, 0, length);
        return bytes;
    }

    /**
     * Transfers a portion of data from this slice into the specified stream starting at the
     * specified absolute {@code index}.
     *
     * @param length the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     * if {@code index + length} is greater than
     * {@code this.length()}
     * @throws java.io.IOException if the specified stream threw an exception during I/O
     */
    public void getBytes(int index, OutputStream out, int length)
            throws IOException {
        checkIndexLength(index, length);

        byte[] buffer = new byte[4096];
        while (length > 0) {
            int size = Math.min(buffer.length, length);
            getBytes(index, buffer, 0, size);
            out.write(buffer, 0, size);
            length -= size;
            index += size;
        }
    }

    /**
     * Sets the specified byte at the specified absolute {@code index} in this
     * buffer.  The 24 high-order bits of the specified value are ignored.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     * {@code index + 1} is greater than {@code this.length()}
     */
    public void setByte(int index, int value) {
        checkIndexLength(index, SIZE_OF_BYTE);
        unsafe.putByte(base, address + index, (byte) (value & 0xFF));
    }

    /**
     * Sets the specified 16-bit short integer at the specified absolute
     * {@code index} in this buffer.  The 16 high-order bits of the specified
     * value are ignored.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     * {@code index + 2} is greater than {@code this.length()}
     */
    public void setShort(int index, int value) {
        checkIndexLength(index, SIZE_OF_SHORT);
        unsafe.putShort(base, address + index, (short) (value & 0xFFFF));
    }

    /**
     * Sets the specified 32-bit integer at the specified absolute
     * {@code index} in this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     * {@code index + 4} is greater than {@code this.length()}
     */
    public void setInt(int index, int value) {
        checkIndexLength(index, SIZE_OF_INT);
        unsafe.putInt(base, address + index, value);
    }

    /**
     * Sets the specified 64-bit long integer at the specified absolute
     * {@code index} in this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     * {@code index + 8} is greater than {@code this.length()}
     */
    public void setLong(int index, long value) {
        checkIndexLength(index, SIZE_OF_LONG);
        unsafe.putLong(base, address + index, value);
    }

    /**
     * Sets the specified 32-bit float at the specified absolute
     * {@code index} in this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     * {@code index + 4} is greater than {@code this.length()}
     */
    public void setFloat(int index, float value) {
        checkIndexLength(index, SIZE_OF_FLOAT);
        unsafe.putFloat(base, address + index, value);
    }

    /**
     * Sets the specified 64-bit double at the specified absolute
     * {@code index} in this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     * {@code index + 8} is greater than {@code this.length()}
     */
    public void setDouble(int index, double value) {
        checkIndexLength(index, SIZE_OF_DOUBLE);
        unsafe.putDouble(base, address + index, value);
    }

    /**
     * Transfers data from the specified slice into this buffer starting at
     * the specified absolute {@code index}.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0}, or
     * if {@code index + source.length()} is greater than {@code this.length()}
     */
    public void setBytes(int index, Slice source) {
        setBytes(index, source, 0, source.length());
    }

    /**
     * Transfers data from the specified slice into this buffer starting at
     * the specified absolute {@code index}.
     *
     * @param sourceIndex the first index of the source
     * @param length the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0},
     * if the specified {@code sourceIndex} is less than {@code 0},
     * if {@code index + length} is greater than
     * {@code this.length()}, or
     * if {@code sourceIndex + length} is greater than
     * {@code source.length()}
     */
    public void setBytes(int index, Slice source, int sourceIndex, int length) {
        checkIndexLength(index, length);
        checkPositionIndexes(sourceIndex, sourceIndex + length, source.length());

        copyMemory(source.base, source.address + sourceIndex, base, address + index, length);
    }

    /**
     * Transfers data from the specified slice into this buffer starting at
     * the specified absolute {@code index}.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0}, or
     * if {@code index + source.length} is greater than {@code this.length()}
     */
    public void setBytes(int index, byte[] source) {
        setBytes(index, source, 0, source.length);
    }

    /**
     * Transfers data from the specified array into this buffer starting at
     * the specified absolute {@code index}.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0},
     * if the specified {@code sourceIndex} is less than {@code 0},
     * if {@code index + length} is greater than
     * {@code this.length()}, or
     * if {@code sourceIndex + length} is greater than {@code source.length}
     */
    public void setBytes(int index, byte[] source, int sourceIndex, int length) {
        checkPositionIndexes(sourceIndex, sourceIndex + length, source.length);
        copyMemory(source, (long) ARRAY_BYTE_BASE_OFFSET + sourceIndex, base, address + index, length);
    }

    /**
     * Transfers data from the specified input stream into this slice starting at
     * the specified absolute {@code index}.
     */
    public int setBytes(int index, InputStream in, int length)
            throws IOException {
        checkIndexLength(index, length);
        byte[] bytes = new byte[4096];
        int remaining = length;
        while (remaining > 0) {
            int bytesRead = in.read(bytes, 0, Math.min(bytes.length, remaining));
            if (bytesRead < 0) {
                // if we didn't read anything return -1
                if (remaining == length) {
                    return -1;
                }
                break;
            }
            copyMemory(bytes, ARRAY_BYTE_BASE_OFFSET, base, address + index, bytesRead);
            remaining -= bytesRead;
            index += bytesRead;
        }
        return length - remaining;
    }

    /**
     * Returns a slice of this buffer's sub-region. Modifying the content of
     * the returned buffer or this buffer affects each other's content.
     */
    public Slice slice(int index, int length) {
        if ((index == 0) && (length == length())) {
            return this;
        }
        checkIndexLength(index, length);
        if (length == 0) {
            return Slices.EMPTY_SLICE;
        }
        return new Slice(base, address + index, length, reference);
    }

    /**
     * Compares the content of the specified buffer to the content of this
     * buffer.  This comparison is performed byte by byte using an unsigned
     * comparison.
     */
    @SuppressWarnings("ObjectEquality")
    @Override
    public int compareTo(Slice that) {
        if (this == that) {
            return 0;
        }
        return compareTo(0, size, that, 0, that.size);
    }

    /**
     * Compares a portion of this slice with a portion of the specified slice.  Equality is
     * solely based on the contents of the slice.
     */
    @SuppressWarnings("ObjectEquality")
    public int compareTo(int offset, int length, Slice that, int otherOffset, int otherLength) {
        if ((this == that) && (offset == otherOffset) && (length == otherLength)) {
            return 0;
        }

        checkIndexLength(offset, length);
        that.checkIndexLength(otherOffset, otherLength);

        int compareLength = Math.min(length, otherLength);
        while (compareLength >= SIZE_OF_LONG) {
            long thisLong = unsafe.getLong(base, address + offset);
            thisLong = Long.reverseBytes(thisLong);
            long thatLong = unsafe.getLong(that.base, that.address + otherOffset);
            thatLong = Long.reverseBytes(thatLong);

            int v = compareUnsignedLongs(thisLong, thatLong);
            if (v != 0) {
                return v;
            }

            offset += SIZE_OF_LONG;
            otherOffset += SIZE_OF_LONG;
            compareLength -= SIZE_OF_LONG;
        }

        while (compareLength > 0) {
            byte thisByte = unsafe.getByte(base, address + offset);
            byte thatByte = unsafe.getByte(that.base, that.address + otherOffset);

            int v = compareUnsignedBytes(thisByte, thatByte);
            if (v != 0) {
                return v;
            }
            offset++;
            otherOffset++;
            compareLength--;
        }

        return Integer.compare(length, otherLength);
    }

    /**
     * Compares the specified object with this slice for equality.  Equality is
     * solely based on the contents of the slice.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Slice)) {
            return false;
        }

        Slice that = (Slice) o;
        if (length() != that.length()) {
            return false;
        }

        int offset = 0;
        int length = size;
        while (length >= SIZE_OF_LONG) {
            long thisLong = unsafe.getLong(base, address + offset);
            long thatLong = unsafe.getLong(that.base, that.address + offset);

            if (thisLong != thatLong) {
                return false;
            }

            offset += SIZE_OF_LONG;
            length -= SIZE_OF_LONG;
        }

        while (length > 0) {
            byte thisByte = unsafe.getByte(base, address + offset);
            byte thatByte = unsafe.getByte(that.base, that.address + offset);
            if (thisByte != thatByte) {
                return false;
            }
            offset++;
            length--;
        }

        return true;
    }

    /**
     * Returns the hash code of this slice.  The hash code is cached once calculated
     * and any future changes to the slice will not effect the hash code.
     */
    @SuppressWarnings("NonFinalFieldReferencedInHashCode")
    @Override
    public int hashCode() {
        if (hash != 0) {
            return hash;
        }

        hash = hashCode(0, size);
        return hash;
    }

    /**
     * Returns the hash code of a portion of this slice.
     */
    public int hashCode(int offset, int length) {
        return (int) XxHash64.hash(this, offset, length);
    }

    /**
     * Compares a portion of this slice with a portion of the specified slice.  Equality is
     * solely based on the contents of the slice.
     */
    @SuppressWarnings("ObjectEquality")
    public boolean equals(int offset, int length, Slice that, int otherOffset, int otherLength) {
        if (length != otherLength) {
            return false;
        }

        if ((this == that) && (offset == otherOffset)) {
            return true;
        }

        checkIndexLength(offset, length);
        that.checkIndexLength(otherOffset, otherLength);

        while (length >= SIZE_OF_LONG) {
            long thisLong = unsafe.getLong(base, address + offset);
            long thatLong = unsafe.getLong(that.base, that.address + otherOffset);

            if (thisLong != thatLong) {
                return false;
            }

            offset += SIZE_OF_LONG;
            otherOffset += SIZE_OF_LONG;
            length -= SIZE_OF_LONG;
        }

        while (length > 0) {
            byte thisByte = unsafe.getByte(base, address + offset);
            byte thatByte = unsafe.getByte(that.base, that.address + otherOffset);
            if (thisByte != thatByte) {
                return false;
            }
            offset++;
            otherOffset++;
            length--;
        }

        return true;
    }

    /**
     * Decodes the contents of this slice into a string with the specified
     * character set name.
     */
    public String toString(Charset charset) {
        return toString(0, length(), charset);
    }

    /**
     * Decodes the contents of this slice into a string using the UTF-8
     * character set.
     */
    public String toStringUtf8() {
        return toString(UTF_8);
    }

    /**
     * Decodes the a portion of this slice into a string with the specified
     * character set name.
     */
    public String toString(int index, int length, Charset charset) {
        if (length == 0) {
            return "";
        }
        if (base instanceof byte[]) {
            return new String((byte[]) base, (int) ((address - ARRAY_BYTE_BASE_OFFSET) + index), length, charset);
        }
        // direct memory can only be converted to a string using a ByteBuffer
        return decodeString(toByteBuffer(index, length), charset);
    }

    public ByteBuffer toByteBuffer() {
        return toByteBuffer(0, size);
    }

    public ByteBuffer toByteBuffer(int index, int length) {
        checkIndexLength(index, length);

        if (base instanceof byte[]) {
            return ByteBuffer.wrap((byte[]) base, (int) ((address - ARRAY_BYTE_BASE_OFFSET) + index), length);
        }

        try {
            final Object[] args = {address + index, length, (Object) reference};
            return (ByteBuffer) newByteBuffer.invokeExact(args);
        } catch (Throwable throwable) {
            if (throwable instanceof Error) {
                throw (Error) throwable;
            }
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            }
            if (throwable instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException(throwable);
        }
    }

    /**
     * Decodes the a portion of this slice into a string with the specified
     * character set name.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Slice{");
        if (base != null) {
            builder.append("base=").append(identityToString(base)).append(", ");
        }
        builder.append("address=").append(address);
        builder.append(", length=").append(length());
        builder.append('}');
        return builder.toString();
    }

    private static String identityToString(Object o) {
        if (o == null) {
            return null;
        }
        return o.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(o));
    }

    private static void copyMemory(Object src, long srcAddress, Object dest, long destAddress, int length) {
        // The Unsafe Javadoc specifies that the transfer size is 8 iff length % 8 == 0
        // so ensure that we copy big chunks whenever possible, even at the expense of two separate copy operations
        int bytesToCopy = length - (length % 8);
        unsafe.copyMemory(src, srcAddress, dest, destAddress, bytesToCopy);
        unsafe.copyMemory(src, srcAddress + bytesToCopy, dest, destAddress + bytesToCopy, length - bytesToCopy);
    }

    private void checkIndexLength(int index, int length) {
        checkPositionIndexes(index, index + length, length());
    }

    //
    // The following methods were forked from Guava primitives
    //

    private static long fillLong(byte value) {
        return (value & 0xFFL) << 56
                | (value & 0xFFL) << 48
                | (value & 0xFFL) << 40
                | (value & 0xFFL) << 32
                | (value & 0xFFL) << 24
                | (value & 0xFFL) << 16
                | (value & 0xFFL) << 8
                | (value & 0xFFL);
    }

    private static int compareUnsignedBytes(byte thisByte, byte thatByte) {
        return unsignedByteToInt(thisByte) - unsignedByteToInt(thatByte);
    }

    private static int unsignedByteToInt(byte thisByte) {
        return thisByte & 0xFF;
    }

    private static int compareUnsignedLongs(long thisLong, long thatLong) {
        return Long.compare(flipUnsignedLong(thisLong), flipUnsignedLong(thatLong));
    }

    private static long flipUnsignedLong(long thisLong) {
        return thisLong ^ Long.MIN_VALUE;
    }
}
