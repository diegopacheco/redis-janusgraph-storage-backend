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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.primitives.Bytes;
import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.janusgraph.diskstorage.*;
import org.janusgraph.diskstorage.common.AbstractStoreManager;
import org.janusgraph.diskstorage.common.AbstractStoreTransaction;
import org.janusgraph.diskstorage.configuration.ConfigNamespace;
import org.janusgraph.diskstorage.configuration.Configuration;
import org.janusgraph.diskstorage.keycolumnvalue.*;
import org.janusgraph.diskstorage.util.ByteBufferUtil;
import org.janusgraph.diskstorage.util.StaticArrayBuffer;
import org.janusgraph.graphdb.configuration.GraphDatabaseConfiguration;
import static org.janusgraph.graphdb.configuration.GraphDatabaseConfiguration.*;
import org.janusgraph.graphdb.configuration.PreInitializeConfigOptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Duration;


@PreInitializeConfigOptions
public class RedisStoreManager extends AbstractStoreManager implements KeyColumnValueStoreManager {

    private static final Logger log = LoggerFactory.getLogger(RedisStoreManager.class);

    private final ConcurrentHashMap<String, RedisKeyColumnValueStore> stores = new ConcurrentHashMap<>();

    protected final StoreFeatures features;

    private static final int DEFAULT_PORT = 6379;
    protected final String hostname;
    protected final String password;
    protected final int port;
    protected final Duration connectionTimeoutMS;

    protected final RedisClient client;
    protected final RedisStoreCodec codec = new RedisStoreCodec();

    protected final StaticBuffer KEYS_SET_KEY = RedisStoreCodec.stringToBuffer("___keys___");
    protected final StaticBuffer COLUMNS_SET_KEY = RedisStoreCodec.stringToBuffer("___cols___");

    public RedisStoreManager(Configuration configuration) throws BackendException {
        super(configuration);

        hostname = configuration.get(STORAGE_HOSTS)[0];
        password = storageConfig.has(AUTH_PASSWORD) ? storageConfig.get(AUTH_PASSWORD) : "";
        port = storageConfig.has(STORAGE_PORT) ? storageConfig.get(STORAGE_PORT) : DEFAULT_PORT;
        connectionTimeoutMS = configuration.get(CONNECTION_TIMEOUT);

        client = RedisClient.create(RedisURI.Builder
            .redis(hostname)
            .withPassword(password)
            .withTimeout(connectionTimeoutMS)
            .build());

        features = new StandardStoreFeatures.Builder()
            .keyConsistent(configuration)
            .persists(true)
            .optimisticLocking(true)
            .unorderedScan(true)
            .orderedScan(true)
            .multiQuery(true)
            .batchMutation(true)
            .keyOrdered(false)
            .locking(false)
            .localKeyPartition(false)
            .timestamps(false)
            .transactional(false)
            .distributed(false)
            .supportsInterruption(false)
            .build();
    }

    @Override
    public RedisKeyColumnValueStore openDatabase(String name) throws BackendException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "Database name may not be null or empty");

        if (stores.containsKey(name)) {
            return stores.get(name);
        }

        try {
            log.debug("Opening database {}", name, new Throwable());

            RedisKeyColumnValueStore store = new RedisKeyColumnValueStore(name, client.connect(codec).sync(), this);
            stores.put(name, store);
            return store;
        } catch (RedisException e) {
            throw new PermanentBackendException("Could not open Redis data store", e);
        }
    }

    @Override
    public KeyColumnValueStore openDatabase(String name, StoreMetaData.Container metaData) throws BackendException {
        return openDatabase(name);
    }

    @Override
    public StoreTransaction beginTransaction(final BaseTransactionConfig config) throws BackendException {
        return new RedisTransaction(config);
    }

    @Override
    public void mutateMany(Map<String, Map<StaticBuffer, KCVMutation>> mutations, StoreTransaction txh) throws BackendException {
        for (Map.Entry<String, Map<StaticBuffer, KCVMutation>> mutationMapEntry : mutations.entrySet()) {
            final RedisKeyColumnValueStore store = openDatabase(mutationMapEntry.getKey());
            final Map<StaticBuffer, KCVMutation> storeMutations = mutationMapEntry.getValue();

            for (Map.Entry<StaticBuffer, KCVMutation> entry : storeMutations.entrySet()) {
                final StaticBuffer key = entry.getKey();
                final KCVMutation mutation = entry.getValue();

                if (!mutation.hasAdditions() && !mutation.hasDeletions())
                    if (log.isDebugEnabled())
                        log.debug("Empty mutation set for {}, doing nothing", key);

                if (mutation.hasDeletions())
                    for (StaticBuffer column : mutation.getDeletions()) {
                        store.del(key, column);

                        if (log.isDebugEnabled())
                            log.debug("Deletion for key: {}, column: {}", key, column);
                    }

                if (mutation.hasAdditions())
                    for (Entry addition : mutation.getAdditions()) {
                        final StaticBuffer column = addition.getColumn();
                        final StaticBuffer value = addition.getValue();
                        store.add(key, column, value);

                        if (log.isDebugEnabled())
                            log.debug("Insertion for key: {}, column: {}, value: {}", key, column, value);
                    }
            }
        }
    }

    void removeDatabase(RedisKeyColumnValueStore db) {
        if (stores.containsKey(db.getName())) {
            String name = db.getName();
            stores.remove(name);
            log.debug("Removed database {}", name);
        }
    }

    @Override
    public void close() throws BackendException {
        try {
            for (RedisKeyColumnValueStore db : stores.values()) db.close();
            client.shutdown();
        } catch (RedisException e) {
            throw new PermanentBackendException("Could not shutdown Redis client", e);
        }
    }

    @Override
    public void clearStorage() throws BackendException {
        for (RedisKeyColumnValueStore db : stores.values()) db.clear();
        close();
    }

    @Override
    public boolean exists() throws BackendException {
        return !stores.keySet().isEmpty();
    }

    @Override
    public String getName() {
        // TODO Determine how to handle name.
        return getClass().getSimpleName() + ":" + "HARDCODED";
    }

    @Override
    public StoreFeatures getFeatures() {
        return features;
    }

    @Override
    public List<KeyRange> getLocalKeyPartition() throws BackendException {
        throw new UnsupportedOperationException();
    }

    private static class RedisTransaction extends AbstractStoreTransaction {
        RedisTransaction(final BaseTransactionConfig config) {
            super(config);
        }
    }
}
