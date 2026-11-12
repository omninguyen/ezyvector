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

import org.youngmonkeys.ezyplatform.service.DefaultSettingService;

import java.util.Collections;
import java.util.Set;

import static org.youngmonkeys.ezyvector.constant.EzyVectorConstants.DEFAULT_MAX_QUERY_LIMIT;
import static org.youngmonkeys.ezyvector.constant.EzyVectorConstants.DEFAULT_VECTOR_DATA_DIR;
import static org.youngmonkeys.ezyvector.constant.EzyVectorConstants.SETTING_NAME_VECTOR_COLLECTIONS_ALLOWED_IPS;
import static org.youngmonkeys.ezyvector.constant.EzyVectorConstants.SETTING_NAME_VECTOR_COLLECTIONS_API_KEY;
import static org.youngmonkeys.ezyvector.constant.EzyVectorConstants.SETTING_NAME_VECTOR_COLLECTIONS_MAX_QUERY_LIMIT;
import static org.youngmonkeys.ezyvector.constant.EzyVectorConstants.SETTING_NAME_VECTOR_DATA_DIR;

public class EzyVectorSettingService {

    protected final DefaultSettingService settingService;

    public EzyVectorSettingService(
        DefaultSettingService settingService
    ) {
        this.settingService = settingService;
    }

    public String getVectorDataDir() {
        return settingService.getTextValue(
            SETTING_NAME_VECTOR_DATA_DIR,
            DEFAULT_VECTOR_DATA_DIR
        );
    }

    public String getVectorCollectionsApiKey() {
        return settingService.getPasswordValue(
            SETTING_NAME_VECTOR_COLLECTIONS_API_KEY
        );
    }

    public Set<String> getVectorCollectionsAllowedIps() {
        return settingService.getSetStringValue(
            SETTING_NAME_VECTOR_COLLECTIONS_ALLOWED_IPS,
            Collections.emptySet()
        );
    }

    /**
     * The upper bound of offset + limit in a search request.
     *
     * @return the configured value, or the default when it is missing
     *         or not positive
     */
    public int getMaxQueryLimit() {
        return normalizeMaxQueryLimit(
            settingService.getIntValue(
                SETTING_NAME_VECTOR_COLLECTIONS_MAX_QUERY_LIMIT,
                DEFAULT_MAX_QUERY_LIMIT
            )
        );
    }

    protected int normalizeMaxQueryLimit(int value) {
        return value > 0 ? value : DEFAULT_MAX_QUERY_LIMIT;
    }
}
