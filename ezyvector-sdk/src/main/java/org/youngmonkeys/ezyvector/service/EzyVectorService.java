/*
 * Copyright 2026 youngmonkeys.org
 *
 * Licensed under the ezyplatform, Version 1.0.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://youngmonkeys.org/licenses/ezyplatform-1.0.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.youngmonkeys.ezyvector.service;

import com.tvd12.ezyfox.util.EzyLoggable;
import com.tvd12.ezyfox.util.EzyNext;
import lombok.AllArgsConstructor;
import org.youngmonkeys.ezyplatform.manager.FileSystemManager;
import org.youngmonkeys.ezyplatform.result.IdResult;
import org.youngmonkeys.ezyvector.converter.EzyVectorEntityToModelConverter;
import org.youngmonkeys.ezyvector.converter.EzyVectorModelToEntityConverter;
import org.youngmonkeys.ezyvector.entity.EzyVectorCollection;
import org.youngmonkeys.ezyvector.entity.EzyVectorCollectionPoint;
import org.youngmonkeys.ezyvector.entity.EzyVectorCollectionSegment;
import org.youngmonkeys.ezyvector.hnsw.HnswIndex;
import org.youngmonkeys.ezyvector.model.EzyVectorSearchResultModel;
import org.youngmonkeys.ezyvector.model.SaveVectorCollectionModel;
import org.youngmonkeys.ezyvector.model.SaveVectorPointModel;
import org.youngmonkeys.ezyvector.repo.EzyVectorCollectionPointRepository;
import org.youngmonkeys.ezyvector.repo.EzyVectorCollectionRepository;
import org.youngmonkeys.ezyvector.repo.EzyVectorCollectionSegmentRepository;
import org.youngmonkeys.ezyvector.result.EzyVectorCollectionVectorSizeResult;
import org.youngmonkeys.ezyvector.storage.EzyVectorFileStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.youngmonkeys.ezyplatform.constant.CommonConstants.LIMIT_500_RECORDS;
import static org.youngmonkeys.ezyplatform.constant.CommonConstants.ZERO_LONG;
import static org.youngmonkeys.ezyvector.constant.EzyVectorConstants.EXPLORATION_FACTOR_SEARCH_MULTIPLIER;
import static org.youngmonkeys.ezyvector.constant.EzyVectorConstants.FIRST_SEGMENT_NO;
import static org.youngmonkeys.ezyvector.constant.EzyVectorConstants.MIN_EXPLORATION_FACTOR_SEARCH;

@AllArgsConstructor
public class EzyVectorService extends EzyLoggable {

    private final FileSystemManager fileSystemManager;
    private final EzyVectorSettingService ezyVectorSettingService;
    private final EzyVectorCollectionRepository collectionRepository;
    private final EzyVectorCollectionPointRepository collectionPointRepository;
    private final EzyVectorCollectionSegmentRepository collectionSegmentRepository;
    private final EzyVectorEntityToModelConverter entityToModelConverter;
    private final EzyVectorModelToEntityConverter modelToEntityConverter;
    private final Object writeLock = new Object();
    private final Set<Long> backfillingCollectionIds =
        ConcurrentHashMap.newKeySet();
    private final Map<Long, HnswIndex> hnswIndexByCollectionId =
        new ConcurrentHashMap<>();
    private final Set<Long> readyHnswCollectionIds =
        ConcurrentHashMap.newKeySet();
    private final Set<Long> buildingHnswCollectionIds =
        ConcurrentHashMap.newKeySet();

    public void createCollectionIfAbsent(
        String collectionName,
        SaveVectorCollectionModel model
    ) throws Exception {
        EzyVectorCollection entity = collectionRepository
            .findByName(collectionName);
        if (entity == null) {
            entity = modelToEntityConverter
                .toVectorCollectionEntity(collectionName, model);
            collectionRepository.save(entity);
        }
        ensureMutableSegment(collectionName);
        startBackfillIfNecessary(collectionName);
        startHnswBuildIfNecessary(collectionName);
    }

    public void upsert(
        String collectionName,
        List<SaveVectorPointModel> points
    ) throws Exception {
        EzyVectorCollectionVectorSizeResult collection =
            getVectorSizeResultEntityByNameOrThrow(
                collectionName
            );
        long collectionId = collection.getId();
        long vectorSize = collection.getVectorSize();
        ensureMutableSegment(collectionId);
        startBackfillIfNecessary(collectionId, vectorSize);
        startHnswBuildIfNecessary(collectionId, vectorSize);
        EzyVectorFileStorage storage = newVectorFileStorage();
        synchronized (writeLock) {
            List<EzyVectorFileStorage.VectorRecord> records =
                new ArrayList<>(points.size());
            for (SaveVectorPointModel point : points) {
                EzyVectorCollectionPoint entity = collectionPointRepository
                    .findByCollectionIdAndPointId(
                        collectionId,
                        point.getId()
                    );
                if (entity == null) {
                    entity = modelToEntityConverter
                        .toVectorCollectionPointEntity(
                            collectionId,
                            point
                        );
                } else {
                    modelToEntityConverter.mergeToCollectionPointEntity(
                        point,
                        entity
                    );
                }
                collectionPointRepository.save(entity);
                records.add(
                    new EzyVectorFileStorage.VectorRecord(
                        entity.getId(),
                        point.getId(),
                        point.getVector()
                    )
                );
            }
            storage.upsertAll(
                collection.getId(),
                collection.getVectorSize(),
                records
            );
            updateHnswIndex(collectionId, records);
        }
        startHnswBuildIfNecessary(collectionId, vectorSize);
    }

    public void deletePoints(
        String collectionName,
        List<Long> pointIds
    ) throws Exception {
        EzyVectorCollectionVectorSizeResult collection =
            getVectorSizeResultEntityByNameOrThrow(
                collectionName
            );
        long collectionId = collection.getId();
        synchronized (writeLock) {
            List<EzyVectorCollectionPoint> points =
                new ArrayList<>(pointIds.size());
            List<Long> slotIds = new ArrayList<>(pointIds.size());
            for (Long pointId : pointIds) {
                EzyVectorCollectionPoint point = collectionPointRepository
                    .findByCollectionIdAndPointId(
                        collectionId,
                        pointId
                    );
                if (point != null) {
                    points.add(point);
                    slotIds.add(point.getId());
                }
            }
            newVectorFileStorage().deleteAll(collectionId, slotIds);
            deleteFromHnswIndex(collectionId, points);
            for (EzyVectorCollectionPoint point : points) {
                collectionPointRepository.delete(point.getId());
            }
        }
    }

    public List<EzyVectorSearchResultModel> search(
        String collectionName,
        float[] vector,
        int limit
    ) throws Exception {
        return search(collectionName, vector, 0, limit);
    }

    public List<EzyVectorSearchResultModel> search(
        String collectionName,
        float[] vector,
        int offset,
        int limit
    ) throws Exception {
        validateSearchWindow(offset, limit);
        EzyVectorCollectionVectorSizeResult collection =
            getVectorSizeResultEntityByNameOrThrow(
                collectionName
            );
        long collectionId = collection.getId();
        long vectorSize = collection.getVectorSize();
        startBackfillIfNecessary(collectionId, vectorSize);
        startHnswBuildIfNecessary(collectionId, vectorSize);
        HnswIndex hnswIndex = getReadyHnswIndex(
            collectionId,
            vectorSize
        );
        if (hnswIndex != null) {
            int window = offset + limit;
            List<HnswIndex.SearchResult> hits = hnswIndex.search(
                vector,
                window,
                toExplorationFactor(window)
            );
            return toSearchResults(
                collectionId,
                hits.subList(Math.min(offset, hits.size()), hits.size())
            );
        }
        List<EzyVectorFileStorage.SearchResult> hits =
            newVectorFileStorage().search(
                collectionId,
                collection.getVectorSize(),
                vector,
                offset,
                limit
            );
        return toExactSearchResults(collectionId, hits);
    }

    private void validateSearchWindow(int offset, int limit) {
        if (offset < 0) {
            throw new IllegalArgumentException(
                "offset must not be negative: " + offset
            );
        }
        if (limit < 1) {
            throw new IllegalArgumentException(
                "limit must be positive: " + limit
            );
        }
        int maxQueryLimit = ezyVectorSettingService.getMaxQueryLimit();
        long window = (long) offset + limit;
        if (window > maxQueryLimit) {
            throw new IllegalArgumentException(
                "offset + limit must not exceed " + maxQueryLimit +
                    ": " + window
            );
        }
    }

    private static int toExplorationFactor(int window) {
        long explorationFactor = Math.max(
            (long) window * EXPLORATION_FACTOR_SEARCH_MULTIPLIER,
            MIN_EXPLORATION_FACTOR_SEARCH
        );
        return (int) Math.min(explorationFactor, Integer.MAX_VALUE);
    }

    private List<EzyVectorSearchResultModel> toSearchResults(
        long collectionId,
        List<HnswIndex.SearchResult> hits
    ) throws Exception {
        List<EzyVectorSearchResultModel> results =
            new ArrayList<>(hits.size());
        for (HnswIndex.SearchResult hit : hits) {
            EzyVectorCollectionPoint point = collectionPointRepository
                .findByCollectionIdAndPointId(
                    collectionId,
                    hit.getId()
                );
            results.add(
                entityToModelConverter.toSearchResultModel(
                    hit,
                    point
                )
            );
        }
        return results;
    }

    private List<EzyVectorSearchResultModel> toExactSearchResults(
        long collectionId,
        List<EzyVectorFileStorage.SearchResult> hits
    ) throws Exception {
        List<EzyVectorSearchResultModel> results =
            new ArrayList<>(hits.size());
        for (EzyVectorFileStorage.SearchResult hit : hits) {
            EzyVectorCollectionPoint point = collectionPointRepository
                .findByCollectionIdAndPointId(
                    collectionId,
                    hit.getId()
                );
            results.add(
                entityToModelConverter.toSearchResultModel(
                    hit,
                    point
                )
            );
        }
        return results;
    }

    private EzyVectorCollectionVectorSizeResult getVectorSizeResultEntityByNameOrThrow(
        String collectionName
    ) {
        EzyVectorCollectionVectorSizeResult collection = collectionRepository
            .findVectorSizeByName(collectionName);
        if (collection == null) {
            throw new IllegalStateException(
                "You need to setup MySQL vector database first"
            );
        }
        return collection;
    }

    private void ensureMutableSegment(
        String collectionName
    ) {
        IdResult collection = collectionRepository
            .findIdByName(collectionName);
        if (collection != null) {
            ensureMutableSegment(collection.getId());
        }
    }

    private void ensureMutableSegment(
        long collectionId
    ) {
        EzyVectorCollectionSegment segment =
            collectionSegmentRepository
                .findByCollectionIdAndSegmentNo(
                    collectionId,
                    FIRST_SEGMENT_NO
                );
        if (segment != null) {
            return;
        }
        segment = modelToEntityConverter
            .toVectorCollectionSegment(collectionId);
        collectionSegmentRepository.save(segment);
    }

    private void startBackfillIfNecessary(
        String collectionName
    ) throws Exception {
        EzyVectorCollectionVectorSizeResult collection =
            collectionRepository
                .findVectorSizeByName(collectionName);
        if (collection != null) {
            startBackfillIfNecessary(
                collection.getId(),
                collection.getVectorSize()
            );
        }
    }

    private void startBackfillIfNecessary(
        long collectionId,
        long vectorSize
    ) throws Exception {
        EzyVectorFileStorage storage = newVectorFileStorage();
        long backfillProgress =
            storage.getBackfillProgress(collectionId);
        List<EzyVectorCollectionPoint> points = collectionPointRepository
            .findListByCollectionIdAndIdGreaterThan(
                collectionId,
                backfillProgress,
                EzyNext.fromLimit(1)
            );
        if (points.isEmpty()
            || !backfillingCollectionIds.add(collectionId)) {
            return;
        }
        Thread thread = new Thread(
            () -> backfillCollection(collectionId, vectorSize),
            "ezyvector-vector-backfill-" + collectionId
        );
        thread.setDaemon(true);
        thread.start();
    }

    private void backfillCollection(
        long collectionId,
        long vectorSize
    ) {
        try {
            EzyVectorFileStorage storage = newVectorFileStorage();
            long lastId = storage.getBackfillProgress(collectionId);
            while (true) {
                List<EzyVectorCollectionPoint> points =
                    collectionPointRepository
                        .findListByCollectionIdAndIdGreaterThan(
                            collectionId,
                            lastId,
                            EzyNext.fromLimit(LIMIT_500_RECORDS)
                        );
                if (points.isEmpty()) {
                    return;
                }
                synchronized (writeLock) {
                    List<EzyVectorFileStorage.VectorRecord> records =
                        new ArrayList<>(points.size());
                    for (EzyVectorCollectionPoint point : points) {
                        records.add(
                            new EzyVectorFileStorage.VectorRecord(
                                point.getId(),
                                point.getPointId(),
                                point.getVector()
                            )
                        );
                        lastId = point.getId();
                    }
                    storage.upsertAll(
                        collectionId,
                        vectorSize,
                        records
                    );
                }
                storage.saveBackfillProgress(collectionId, lastId);
            }
        } catch (Exception e) {
            logger.warn(
                "backfill vector collection: {} failed",
                collectionId,
                e
            );
        } finally {
            backfillingCollectionIds.remove(collectionId);
        }
    }

    private void startHnswBuildIfNecessary(
        String collectionName
    ) {
        EzyVectorCollectionVectorSizeResult collection =
            collectionRepository.findVectorSizeByName(collectionName);
        if (collection != null) {
            startHnswBuildIfNecessary(
                collection.getId(),
                collection.getVectorSize()
            );
        }
    }

    private void startHnswBuildIfNecessary(
        long collectionId,
        long vectorSize
    ) {
        if (readyHnswCollectionIds.contains(collectionId)) {
            return;
        }
        EzyVectorFileStorage storage = newVectorFileStorage();
        if (storage.isHnswPresent(collectionId)) {
            try {
                HnswIndex loaded =
                    HnswIndex.load(storage.getHnswPath(collectionId));
                if (loaded.getVectorSize() == (int) vectorSize) {
                    hnswIndexByCollectionId.put(collectionId, loaded);
                    readyHnswCollectionIds.add(collectionId);
                    return;
                }
                logger.warn(
                    "hnsw index file for vector collection: {} has " +
                        "vectorSize: {} but collection is configured " +
                        "with vectorSize: {}, rebuilding",
                    collectionId,
                    loaded.getVectorSize(),
                    vectorSize
                );
            } catch (Exception e) {
                logger.warn(
                    "load hnsw index for vector collection: {} failed",
                    collectionId,
                    e
                );
            }
        }
        if (!buildingHnswCollectionIds.add(collectionId)) {
            return;
        }
        HnswIndex index = new HnswIndex(
            HnswIndex.DEFAULT_MAX_M,
            HnswIndex.DEFAULT_EF_CONSTRUCTION,
            (int) vectorSize
        );
        hnswIndexByCollectionId.put(collectionId, index);
        Thread thread = new Thread(
            () -> buildHnswIndex(collectionId),
            "ezyvector-vector-hnsw-build-" + collectionId
        );
        thread.setDaemon(true);
        thread.start();
    }

    private HnswIndex getReadyHnswIndex(
        long collectionId,
        long vectorSize
    ) {
        if (!readyHnswCollectionIds.contains(collectionId)) {
            return null;
        }
        HnswIndex index = hnswIndexByCollectionId.get(collectionId);
        if (index != null) {
            return index;
        }
        EzyVectorFileStorage storage = newVectorFileStorage();
        if (!storage.isHnswPresent(collectionId)) {
            return null;
        }
        try {
            index = HnswIndex.load(storage.getHnswPath(collectionId));
            hnswIndexByCollectionId.put(collectionId, index);
            return index;
        } catch (Exception e) {
            readyHnswCollectionIds.remove(collectionId);
            logger.warn(
                "load hnsw index for vector collection: {} failed",
                collectionId,
                e
            );
            startHnswBuildIfNecessary(collectionId, vectorSize);
            return null;
        }
    }

    private void updateHnswIndex(
        long collectionId,
        List<EzyVectorFileStorage.VectorRecord> records
    ) throws Exception {
        HnswIndex index = hnswIndexByCollectionId.get(collectionId);
        if (index == null) {
            return;
        }
        for (EzyVectorFileStorage.VectorRecord record : records) {
            insertIntoHnswIndex(
                index,
                collectionId,
                record.getPointId(),
                record.getVector()
            );
        }
        if (readyHnswCollectionIds.contains(collectionId)) {
            index
                .save(newVectorFileStorage()
                .getHnswPath(collectionId));
        }
    }

    private void deleteFromHnswIndex(
        long collectionId,
        List<EzyVectorCollectionPoint> points
    ) throws Exception {
        HnswIndex index = hnswIndexByCollectionId.get(collectionId);
        if (index == null) {
            return;
        }
        for (EzyVectorCollectionPoint point : points) {
            index.remove(point.getPointId());
        }
        if (readyHnswCollectionIds.contains(collectionId)) {
            index.save(
                newVectorFileStorage().getHnswPath(collectionId)
            );
        }
    }

    private void buildHnswIndex(long collectionId) {
        try {
            HnswIndex index = hnswIndexByCollectionId
                .get(collectionId);
            if (index == null) {
                logger.warn(
                    "no hnsw index bound to vector collection: {} " +
                        "before build, skip",
                    collectionId
                );
                return;
            }
            long lastId = ZERO_LONG;
            while (true) {
                List<EzyVectorCollectionPoint> points =
                    collectionPointRepository
                        .findListByCollectionIdAndIdGreaterThan(
                            collectionId,
                            lastId,
                            EzyNext.fromLimit(
                                LIMIT_500_RECORDS
                            )
                        );
                if (points.isEmpty()) {
                    break;
                }
                for (EzyVectorCollectionPoint point : points) {
                    insertIntoHnswIndex(
                        index,
                        collectionId,
                        point.getPointId(),
                        point.getVector()
                    );
                    lastId = point.getId();
                }
            }
            index
                .save(newVectorFileStorage()
                .getHnswPath(collectionId));
            readyHnswCollectionIds.add(collectionId);
        } catch (Exception e) {
            logger.warn(
                "build hnsw index for vector collection: {} failed",
                collectionId,
                e
            );
        } finally {
            buildingHnswCollectionIds.remove(collectionId);
        }
    }

    private void insertIntoHnswIndex(
        HnswIndex index,
        long collectionId,
        long pointId,
        float[] vector
    ) {
        try {
            index.insert(pointId, vector);
        } catch (IllegalArgumentException e) {
            logger.warn(
                "skip vector point: {} of collection: {} due to: {}",
                pointId,
                collectionId,
                e.getMessage()
            );
        }
    }

    private EzyVectorFileStorage newVectorFileStorage() {
        return new EzyVectorFileStorage(
            fileSystemManager,
            ezyVectorSettingService.getVectorDataDir()
        );
    }
}
