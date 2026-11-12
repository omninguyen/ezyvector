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

package org.youngmonkeys.ezyvector.admin.controller.api;

import com.tvd12.ezyfox.annotation.EzyFeature;
import com.tvd12.ezyhttp.core.annotation.Description;
import com.tvd12.ezyhttp.core.exception.HttpBadRequestException;
import com.tvd12.ezyhttp.core.response.ResponseEntity;
import com.tvd12.ezyhttp.server.core.annotation.Api;
import com.tvd12.ezyhttp.server.core.annotation.Authenticated;
import com.tvd12.ezyhttp.server.core.annotation.Controller;
import com.tvd12.ezyhttp.server.core.annotation.DoPut;
import com.tvd12.ezyhttp.server.core.annotation.RequestBody;
import lombok.AllArgsConstructor;
import org.youngmonkeys.ezyvector.admin.request.AdminSaveEzyVectorCollectionsSettingRequest;
import org.youngmonkeys.ezyvector.admin.service.AdminEzyVectorSettingService;

import java.util.Collections;

@Api
@Authenticated
@Controller("/api/v1")
@EzyFeature("settings_management")
@AllArgsConstructor
public class AdminApiEzyVectorSettingController {

    private final AdminEzyVectorSettingService ezyVectorSettingService;

    @Description("Update vector collections setting")
    @DoPut("/settings/collections")
    public ResponseEntity settingsPut(
        @RequestBody AdminSaveEzyVectorCollectionsSettingRequest request
    ) {
        Integer maxQueryLimit = request.getVectorCollectionsMaxQueryLimit();
        if (maxQueryLimit != null && maxQueryLimit <= 0) {
            throw new HttpBadRequestException(
                Collections.singletonMap(
                    "vectorCollectionsMaxQueryLimit",
                    "invalid"
                )
            );
        }
        ezyVectorSettingService.setVectorCollectionsApiKey(
            request.getVectorCollectionsApiKey()
        );
        ezyVectorSettingService.setVectorCollectionsAllowedIps(
            request.getVectorCollectionsAllowedIps()
        );
        if (maxQueryLimit != null) {
            ezyVectorSettingService.setVectorCollectionsMaxQueryLimit(
                maxQueryLimit
            );
        }
        return ResponseEntity.noContent();
    }
}
