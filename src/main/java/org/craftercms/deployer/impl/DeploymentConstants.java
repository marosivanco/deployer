/*
 * Copyright (C) 2007-2017 Crafter Software Corporation.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.craftercms.deployer.impl;

/**
 * Common constants used by Deployer classes.
 *
 * @author avasquez
 */
public class DeploymentConstants {

    private DeploymentConstants() {
    }

    // Target-specific Configuration Keys

    public static final String TARGET_ENV_CONFIG_KEY = "target.env";
    public static final String TARGET_SITE_NAME_CONFIG_KEY = "target.siteName";
    public static final String TARGET_ID_CONFIG_KEY = "target.id";
    public static final String TARGET_SCHEDULED_DEPLOYMENT_ENABLED_CONFIG_KEY = "target.deployment.scheduling.enabled";
    public static final String TARGET_SCHEDULED_DEPLOYMENT_CRON_CONFIG_KEY = "target.deployment.scheduling.cron";
    public static final String TARGET_DEPLOYMENT_PIPELINE_CONFIG_KEY = "target.deployment.pipeline";

    // Processor-specific Configuration Keys

    public static final String PROCESSOR_NAME_CONFIG_KEY = "processorName";
    public static final String PROCESSOR_INCLUDE_FILES_CONFIG_KEY = "includeFiles";
    public static final String PROCESSOR_EXCLUDE_FILES_CONFIG_KEY = "excludeFiles";

    // Processor params

    public static final String REPROCESS_ALL_FILES_PARAM_NAME = "reprocess_all_files";

    // Logging MDC Keys

    public static final String TARGET_ID_MDC_KEY = "targetId";

    // Other constants

    public static final String PROCESSED_COMMIT_FILE_EXTENSION = "commit";

}
