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

package org.youngmonkeys.ezyvector.admin.controller.view;

import com.tvd12.ezyfox.annotation.EzyFeature;
import com.tvd12.ezyhttp.server.core.annotation.Authenticated;
import com.tvd12.ezyhttp.server.core.annotation.Controller;
import com.tvd12.ezyhttp.server.core.annotation.DoGet;
import com.tvd12.ezyhttp.server.core.view.View;
import lombok.AllArgsConstructor;
import org.youngmonkeys.ezyvector.admin.service.AdminEzyVectorSettingService;

import java.util.Set;

@Controller
@Authenticated
@EzyFeature("settings_management")
@AllArgsConstructor
public class AdminEzyVectorSettingController {

    private final AdminEzyVectorSettingService ezyVectorSettingService;

    @DoGet("/settings")
    public View settingsGet() {
        String vectorCollectionsApiKey = ezyVectorSettingService
            .getVectorCollectionsApiKey();
        Set<String> vectorCollectionsAllowedIps = ezyVectorSettingService
            .getVectorCollectionsAllowedIps();
        return View.builder()
            .template("ezyvector/setting/index")
            .addVariable(
                "vectorCollectionsApiKey",
                vectorCollectionsApiKey
            )
            .addVariable(
                "vectorCollectionsAllowedIps",
                vectorCollectionsAllowedIps
            )
            .addVariable(
                "vectorCollectionsMaxQueryLimit",
                ezyVectorSettingService.getMaxQueryLimit()
            )
            .build();
    }
}
