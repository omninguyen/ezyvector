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

package org.youngmonkeys.ezyvector.admin.service;

import com.tvd12.ezyhttp.server.core.annotation.Service;
import org.youngmonkeys.ezyplatform.admin.service.AdminSettingService;
import org.youngmonkeys.ezyvector.service.EzyVectorSettingService;

import java.util.Collection;

import static com.tvd12.ezyfox.io.EzyStrings.isNotBlank;
import static org.youngmonkeys.ezyplatform.constant.CommonConstants.PATTERN_HIDDEN_PASSWORD;
import static org.youngmonkeys.ezyvector.constant.EzyVectorConstants.SETTING_NAME_VECTOR_COLLECTIONS_ALLOWED_IPS;
import static org.youngmonkeys.ezyvector.constant.EzyVectorConstants.SETTING_NAME_VECTOR_COLLECTIONS_API_KEY;
import static org.youngmonkeys.ezyvector.constant.EzyVectorConstants.SETTING_NAME_VECTOR_COLLECTIONS_MAX_QUERY_LIMIT;

@Service
public class AdminEzyVectorSettingService extends EzyVectorSettingService {

    private final AdminSettingService settingService;

    public AdminEzyVectorSettingService(
        AdminSettingService settingService
    ) {
        super(settingService);
        this.settingService = settingService;
    }

    public void setVectorCollectionsApiKey(String value) {
        if (isNotBlank(value)
            && !value.matches(PATTERN_HIDDEN_PASSWORD)
        ) {
            settingService.setPasswordValue(
                SETTING_NAME_VECTOR_COLLECTIONS_API_KEY,
                value
            );
        }
    }

    public void setVectorCollectionsAllowedIps(Collection<String> values) {
        settingService.setArrayValue(
            SETTING_NAME_VECTOR_COLLECTIONS_ALLOWED_IPS,
            values
        );
    }

    public void setVectorCollectionsMaxQueryLimit(int value) {
        settingService.setIntValue(
            SETTING_NAME_VECTOR_COLLECTIONS_MAX_QUERY_LIMIT,
            value
        );
    }
}
