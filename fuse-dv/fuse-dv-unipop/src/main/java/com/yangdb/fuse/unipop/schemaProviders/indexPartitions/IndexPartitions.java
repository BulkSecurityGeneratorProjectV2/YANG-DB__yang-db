package com.yangdb.fuse.unipop.schemaProviders.indexPartitions;

/*-
 * #%L
 * fuse-dv-unipop
 * %%
 * Copyright (C) 2016 - 2019 The YangDb Graph Database Project
 * %%
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
 * #L%
 */

import javaslang.collection.Stream;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public interface IndexPartitions {
    Optional<String> getPartitionField();

    Iterable<Partition> getPartitions();

    /**
     * collect all indices names from index partitions
     */
    default Iterable<String> getIndices() {
        return StreamSupport.stream(getPartitions().spliterator(), false)
                .flatMap(p -> StreamSupport.stream(p.getIndices().spliterator(), false))
                .collect(Collectors.toList());
    }

    interface Partition {
        Iterable<String> getIndices();

        interface Range<T extends Comparable<T>> extends Partition {
            T getFrom();

            T getTo();

            boolean isWithin(T value);

            class Impl<T extends Comparable<T>> implements Range<T> {
                //region Constructors
                public Impl(T from, T to, String... indices) {
                    this(from, to, Stream.of(indices));
                }

                public Impl(T from, T to, Iterable<String> indices) {
                    this.from = from;
                    this.to = to;
                    this.indices = Stream.ofAll(indices).toJavaSet();
                }
                //endregion

                //region Range Implementation
                @Override
                public Iterable<String> getIndices() {
                    return this.indices;
                }

                @Override
                public T getFrom() {
                    return this.from;
                }

                @Override
                public T getTo() {
                    return this.to;
                }

                @Override
                public boolean isWithin(T value) {
                    return value.compareTo(this.from) >= 0 && value.compareTo(this.to) < 0;
                }
                //endregion

                //region Fields
                private Iterable<String> indices;
                private T from;
                private T to;
                //endregion

                @Override
                public boolean equals(Object o) {
                    if (this == o) return true;
                    if (o == null || getClass() != o.getClass()) return false;
                    Impl<?> impl = (Impl<?>) o;
                    return indices.equals(impl.indices) &&
                            from.equals(impl.from) &&
                            to.equals(impl.to);
                }

                @Override
                public int hashCode() {
                    return Objects.hash(indices, from, to);
                }
            }
        }

        interface Default<T extends Comparable<T>> extends Range {
            @Override
            default boolean isWithin(Comparable value) {
                return true;
            }
        }
    }

    class Impl implements IndexPartitions {
        //region Constructors
        public Impl(Partition... partitions) {
            this(Stream.of(partitions));
        }

        public Impl(Iterable<Partition> partitions) {
            this.partitions = Stream.ofAll(partitions).toJavaList();
            this.partitionField = Optional.empty();
        }

        public Impl(String partitionField, Partition... partitions) {
            this(partitionField, Stream.of(partitions));
        }

        public Impl(String partitionField, Iterable<Partition> partitions) {
            this.partitionField = Optional.of(partitionField);
            this.partitions = Stream.ofAll(partitions).toJavaList();
        }
        //endregion

        //region IndexPartitions Implementation
        @Override
        public Optional<String> getPartitionField() {
            return this.partitionField;
        }

        @Override
        public Iterable<Partition> getPartitions() {
            return this.partitions;
        }
        //endregion

        //region Fields
        private Optional<String> partitionField;
        private Iterable<Partition> partitions;
        //endregion
    }
}
