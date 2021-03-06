/*
 * Copyright 2011-2016 the original author or authors.
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
package com.lambdaworks.redis.cluster;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * Dynamic selection of nodes.
 * 
 * @param <API> API type.
 * @param <CMD> Command command interface type to invoke multi-node operations.
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
class DynamicNodeSelection<API, CMD, K, V> extends AbstractNodeSelection<API, CMD, K, V> {

    private final ClusterDistributionChannelWriter<K, V> writer;
    private final Predicate<RedisClusterNode> selector;
    private final ClusterConnectionProvider.Intent intent;
    private final Function<StatefulRedisConnection<K, V>, API> apiExtractor;

    public DynamicNodeSelection(ClusterDistributionChannelWriter<K, V> writer, Predicate<RedisClusterNode> selector,
            ClusterConnectionProvider.Intent intent, Function<StatefulRedisConnection<K, V>, API> apiExtractor) {

        this.selector = selector;
        this.intent = intent;
        this.writer = writer;
        this.apiExtractor = apiExtractor;
    }

    @Override
    protected StatefulRedisConnection<K, V> getConnection(RedisClusterNode redisClusterNode) {

        RedisURI uri = redisClusterNode.getUri();
        return writer.getClusterConnectionProvider().getConnection(intent, uri.getHost(), uri.getPort());
    }

    @Override
    protected API getApi(RedisClusterNode redisClusterNode) {
        return apiExtractor.apply(getConnection(redisClusterNode));
    }

    @Override
    protected List<RedisClusterNode> nodes() {
        return writer.getPartitions().getPartitions().stream().filter(selector).collect(Collectors.toList());
    }
}
