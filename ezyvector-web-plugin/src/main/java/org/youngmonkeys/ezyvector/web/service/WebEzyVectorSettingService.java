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

package org.youngmonkeys.ezyvector.web.service;

import com.tvd12.ezyhttp.core.codec.SingletonStringDeserializer;
import com.tvd12.ezyhttp.server.core.annotation.Service;
import org.youngmonkeys.ezyplatform.util.Numbers;
import org.youngmonkeys.ezyplatform.web.service.WebSettingService;
import org.youngmonkeys.ezyvector.service.EzyVectorSettingService;

import java.util.Collections;
import java.util.Set;

import static org.youngmonkeys.ezyvector.constant.EzyVectorConstants.SETTING_NAME_VECTOR_COLLECTIONS_ALLOWED_IPS;
import static org.youngmonkeys.ezyvector.constant.EzyVectorConstants.SETTING_NAME_VECTOR_COLLECTIONS_API_KEY;
import static org.youngmonkeys.ezyvector.constant.EzyVectorConstants.SETTING_NAME_VECTOR_COLLECTIONS_MAX_QUERY_LIMIT;

@Service
public class WebEzyVectorSettingService extends EzyVectorSettingService {

    public WebEzyVectorSettingService(
        WebSettingService settingService
    ) {
        super(settingService);
        settingService.addValueConverter(
            SETTING_NAME_VECTOR_COLLECTIONS_API_KEY,
            settingService::decryptValue
        );
        settingService.scheduleCacheValue(
            SETTING_NAME_VECTOR_COLLECTIONS_API_KEY,
            15
        );
        settingService.cacheValueIfNotNull(
            SETTING_NAME_VECTOR_COLLECTIONS_ALLOWED_IPS,
            settingService.getSetStringValue(
                SETTING_NAME_VECTOR_COLLECTIONS_ALLOWED_IPS,
                Collections.emptySet()
            )
        );
        settingService.addValueConverter(
            SETTING_NAME_VECTOR_COLLECTIONS_ALLOWED_IPS,
            it -> SingletonStringDeserializer
                .getInstance()
                .deserialize(it, Set.class, String.class)
        );
        settingService.scheduleCacheValue(
            SETTING_NAME_VECTOR_COLLECTIONS_ALLOWED_IPS,
            15
        );
        settingService.addValueConverter(
            SETTING_NAME_VECTOR_COLLECTIONS_MAX_QUERY_LIMIT,
            Numbers::toIntOrZero
        );
        settingService.scheduleCacheValue(
            SETTING_NAME_VECTOR_COLLECTIONS_MAX_QUERY_LIMIT,
            15
        );
    }

    @Override
    public int getMaxQueryLimit() {
        Integer value = settingService.getCachedValue(
            SETTING_NAME_VECTOR_COLLECTIONS_MAX_QUERY_LIMIT
        );
        return value != null
            ? normalizeMaxQueryLimit(value)
            : super.getMaxQueryLimit();
    }

    @Override
    public String getVectorCollectionsApiKey() {
        String value = settingService.getCachedValue(
            SETTING_NAME_VECTOR_COLLECTIONS_API_KEY
        );
        return value != null ? value : super.getVectorCollectionsApiKey();
    }

    @Override
    public Set<String> getVectorCollectionsAllowedIps() {
        return settingService.getCachedValue(
            SETTING_NAME_VECTOR_COLLECTIONS_ALLOWED_IPS,
            Collections.emptySet()
        );
    }
}
