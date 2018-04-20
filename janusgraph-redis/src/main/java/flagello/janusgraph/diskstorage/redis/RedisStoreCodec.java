// Copyright 2018 William Esz
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package flagello.janusgraph.diskstorage.redis;

import io.lettuce.core.codec.RedisCodec;
import org.apache.commons.codec.binary.Hex;
import org.janusgraph.diskstorage.StaticBuffer;
import org.janusgraph.diskstorage.util.StaticArrayBuffer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class RedisStoreCodec implements RedisCodec<StaticBuffer, StaticBuffer> {
    @Override
    public StaticBuffer decodeKey(ByteBuffer bytes) {
        return StaticArrayBuffer.of(bytes);
    }

    @Override
    public StaticBuffer decodeValue(ByteBuffer bytes) {
        return StaticArrayBuffer.of(bytes);
    }

    @Override
    public ByteBuffer encodeKey(StaticBuffer key) {
        return key.asByteBuffer();
    }

    @Override
    public ByteBuffer encodeValue(StaticBuffer value) {
        return value.asByteBuffer();
    }

    public static String bufferToString(StaticBuffer b) {
        ByteBuffer bb = b.asByteBuffer();
        return new String(bb.array(), bb.position() + bb.arrayOffset(), bb.remaining(), StandardCharsets.ISO_8859_1);
    }

    public static String bufferToHex(final StaticBuffer input) {
        final ByteBuffer buf = input.asByteBuffer();
        final byte[] bytes = Arrays.copyOf(buf.array(), buf.limit());
        return Hex.encodeHexString(bytes);
    }

    public static StaticBuffer stringToBuffer(String s) {
        byte[] b;
        b = s.getBytes(StandardCharsets.ISO_8859_1);
        ByteBuffer bb = ByteBuffer.allocate(b.length);
        bb.put(b);
        bb.flip();
        return StaticArrayBuffer.of(bb);
    }
}
