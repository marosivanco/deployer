/*
 * Copyright (C) 2007-2016 Crafter Software Corporation.
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
package org.craftercms.deployer.impl.processors;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.impl.ProcessedCommitsStore;
import org.craftercms.deployer.utils.ConfigUtils;
import org.craftercms.deployer.utils.GitUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import static org.craftercms.deployer.impl.DeploymentConstants.PROCESSED_COMMIT_FILE_EXTENSION;

/**
 * Created by alfonsovasquez on 1/12/16.
 */
public class GitPullProcessor extends AbstractMainDeploymentProcessor {

    public static final String REMOTE_REPO_URL_CONFIG_KEY = "remoteRepo.url";
    public static final String REMOTE_REPO_BRANCH_CONFIG_KEY = "remoteRepo.branch";
    public static final String REMOTE_REPO_USERNAME_CONFIG_KEY = "remoteRepo.username";
    public static final String REMOTE_REPO_PASSWORD_CONFIG_KEY = "remoteRepo.password";
    public static final String GIT_CONFIG_BIG_FILE_THRESHOLD_CONFIG_KEY = "gitConfig.bigFileThreshold";
    public static final String GIT_CONFIG_COMPRESSION_CONFIG_KEY = "gitConfig.compression";

    public static final String GIT_FOLDER_NAME = ".git";

    private static final Logger logger = LoggerFactory.getLogger(GitPullProcessor.class);

    protected File localRepoFolder;
    protected ProcessedCommitsStore processedCommitsStore;

    protected String remoteRepoUrl;
    protected String remoteRepoBranch;
    protected String remoteRepoUsername;
    protected String remoteRepoPassword;
    protected String gitConfigBigFileThreshold;
    protected Integer gitConfigCompression;

    @Required
    public void setLocalRepoFolder(File localRepoFolder) {
        this.localRepoFolder = localRepoFolder;
    }

    @Required
    public void setProcessedCommitsStore(ProcessedCommitsStore processedCommitsStore) {
        this.processedCommitsStore = processedCommitsStore;
    }

    @Override
    protected void doConfigure(Configuration config) throws DeployerException {
        remoteRepoUrl = ConfigUtils.getRequiredStringProperty(config, REMOTE_REPO_URL_CONFIG_KEY);
        remoteRepoBranch = ConfigUtils.getStringProperty(config, REMOTE_REPO_BRANCH_CONFIG_KEY);
        remoteRepoUsername = ConfigUtils.getStringProperty(config, REMOTE_REPO_USERNAME_CONFIG_KEY);
        remoteRepoPassword = ConfigUtils.getStringProperty(config, REMOTE_REPO_PASSWORD_CONFIG_KEY);
        gitConfigBigFileThreshold = ConfigUtils.getStringProperty(config, GIT_CONFIG_BIG_FILE_THRESHOLD_CONFIG_KEY);
        gitConfigCompression = ConfigUtils.getIntegerProperty(config, GIT_CONFIG_COMPRESSION_CONFIG_KEY);
    }

    @Override
    protected boolean shouldExecute(Deployment deployment, ChangeSet filteredChangeSet) {
        // Run if the deployment is running
        return deployment.isRunning();
    }

    @Override
    protected ChangeSet doExecute(Deployment deployment, ProcessorExecution execution,
                                  ChangeSet filteredChangeSet, Map<String, Object> params) throws DeployerException {
        File gitFolder = new File(localRepoFolder, GIT_FOLDER_NAME);

        if (localRepoFolder.exists() && gitFolder.exists()) {
            doPull(execution);
        } else {
            processedCommitsStore.delete(targetId);

            doClone(execution);
        }

        return null;
    }

    @Override
    protected boolean failDeploymentOnProcessorFailure() {
        return true;
    }

    protected void doPull(ProcessorExecution execution) throws DeployerException {
        try (Git git = openLocalRepository()) {
            logger.info("Executing git pull for repository {}...", localRepoFolder);

            PullResult pullResult = git.pull().call();
            if (pullResult.isSuccessful()) {
                MergeResult mergeResult = pullResult.getMergeResult();
                String details;

                switch (mergeResult.getMergeStatus()) {
                    case FAST_FORWARD:
                        details = "Changes successfully pulled from remote repo " + remoteRepoUrl + " into local repo " +
                                  localRepoFolder;

                        logger.info(details);

                        execution.setStatusDetails(details);

                        break;
                    case ALREADY_UP_TO_DATE:
                        details = "Local repository " + localRepoFolder + " up to date (no changes pulled from remote repo " +
                                  remoteRepoUrl + ")";

                        logger.info(details);

                        execution.setStatusDetails(details);

                        break;
                    case MERGED:
                        details = "Changes from remote repo " + remoteRepoUrl + " merged into local repo " + localRepoFolder;

                        logger.info(details);

                        execution.setStatusDetails(details);

                        break;
                    default:
                        // Non-supported merge results
                        throw new DeployerException("Received unsupported merge result after executing pull " + pullResult);
                }
            } else {
                throw new DeployerException("Git pull for repository " + localRepoFolder + " failed: " + pullResult);
            }
        } catch (GitAPIException e) {
            throw new DeployerException("Git pull for repository " + localRepoFolder + " failed", e);
        }
    }

    protected Git openLocalRepository() throws DeployerException {
        try {
            logger.debug("Opening local Git repository at {}", localRepoFolder);

            return GitUtils.openRepository(localRepoFolder);
        } catch (IOException e) {
            throw new DeployerException("Failed to open Git repository at " + localRepoFolder, e);
        }
    }

    protected void doClone(ProcessorExecution execution) throws DeployerException {
        try (Git git = cloneRemoteRepository()) {
            String details = "Successfully cloned Git remote repository " + remoteRepoUrl + " into " + localRepoFolder;

            logger.info(details);

            execution.setStatusDetails(details);
        }
    }

    protected Git cloneRemoteRepository() throws DeployerException {
        try {
            if (localRepoFolder.exists()) {
                logger.debug("Deleting existing folder {} before cloning", localRepoFolder);

                FileUtils.forceDelete(localRepoFolder);
            } else {
                logger.debug("Creating folder {} and any nonexistent parents before cloning", localRepoFolder);

                FileUtils.forceMkdir(localRepoFolder);
            }

            logger.info("Cloning Git remote repository {} into {}", remoteRepoUrl, localRepoFolder);

            return GitUtils.cloneRemoteRepository(remoteRepoUrl, remoteRepoBranch, remoteRepoUsername, remoteRepoPassword,
                                                  localRepoFolder, gitConfigBigFileThreshold, gitConfigCompression);
        } catch (IOException | GitAPIException e) {
            // Force delete so there's no invalid remains
            FileUtils.deleteQuietly(localRepoFolder);

            throw new DeployerException("Failed to clone Git remote repository " + remoteRepoUrl + " into " + localRepoFolder, e);
        }
    }

}
