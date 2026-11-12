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

package org.youngmonkeys.ezyvector.constant;

public final class EzyVectorConstants {

    public static final int EXPLORATION_FACTOR_SEARCH_MULTIPLIER = 8;

    public static final int MIN_EXPLORATION_FACTOR_SEARCH = 64;

    public static final long FIRST_SEGMENT_NO = 1L;
    public static final long FIRST_VERSION = 1L;

    public static final String DEFAULT_VECTOR_DATA_DIR = "data/ezyvector";
    public static final int DEFAULT_MAX_QUERY_LIMIT = 10_000;

    public static final String SETTING_NAME_VECTOR_DATA_DIR =
        "ezyrag_vector_data_dir";
    public static final String SETTING_NAME_VECTOR_COLLECTIONS_MAX_QUERY_LIMIT =
        "ezyvector_vector_collections_max_query_limit";
    public static final String SETTING_NAME_VECTOR_COLLECTIONS_ALLOWED_IPS =
        "ezyvector_vector_collections_allowed_ips";
    public static final String SETTING_NAME_VECTOR_COLLECTIONS_API_KEY =
        "ezyvector_vector_collections_api_key";

    private EzyVectorConstants() {}

}
